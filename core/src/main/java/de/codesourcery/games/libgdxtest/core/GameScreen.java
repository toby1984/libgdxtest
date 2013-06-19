package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class GameScreen implements Screen
{
    private static final float SCROLL_BORDER_PROXIMITY_THRESHOLD = Entity.OUTER_RADIUS_IN_PIXELS*4; 
    
    private int width;
    private int height;
    
    private final GameWorld world;
    private final Entity player = new Entity("Player #1", new Vector2(0,0) , new Vector2( 1,1 ) ) ;
    
    private OrthographicCamera camera;
    private long tickCounter=0;
    
    private BitmapFont font;
    
    // background
    private Texture backgroundTexture;    
    private SpriteBatch backgroundBatch;
    private final Vector2 backgroundPosition=new Vector2();
    
    public GameScreen() 
    {
        backgroundBatch = new SpriteBatch();
        
    	setupTextRendering();
    	
        world = new GameWorld();
        world.setPlayer( player );
        final Entity agent1 = new Entity("Agent #1", new Vector2(50,50) , new Vector2( 1,1 ) ) ;
        world.addAgent( agent1 );
    }
    
    private void setupTextRendering() 
    {
    	if ( backgroundTexture != null ) {
    		backgroundTexture.dispose();
    		font.dispose();
    	}
    	
        backgroundTexture = new Texture(Gdx.files.internal("metal.png"));
        backgroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        
        font = new BitmapFont(Gdx.files.internal("courier.fnt"),false);
    }
    
    @Override
    public void dispose() { }

    @Override
    public void hide() { }

    @Override
    public void pause() { }
    
    @Override
    public void render(float deltaSeconds)
    {
        if ( (tickCounter++ % 60 ) == 0 ) {
            System.out.println("FPS: "+(1.0/deltaSeconds));
        }
        
        camera.apply( Gdx.gl10 );
        
        // process input
        processInput();
        
        // render world
        renderWorld();
        
        // advance world state
        tick(deltaSeconds);     
        
        tickCounter++;
    }
    
    private void tick(float deltaSeconds) 
    {
        final boolean playerHasMoved = player.isInMotion();
        world.tick( deltaSeconds );
        
        if (  playerHasMoved ) 
        {
            maybeScrollBackgroundAndMoveCamera();
        }
    }

    private void renderWorld()
    {
        // clear display
        Gdx.graphics.getGL10().glClearColor( 0 , 0, 0, 1 );
        Gdx.graphics.getGL10().glClear( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
        
        // render background
        backgroundBatch.begin();
        
        int texX = (int) backgroundPosition.x % backgroundTexture.getWidth();
        int texY = (int) backgroundPosition.y % backgroundTexture.getHeight();        
        
        backgroundBatch.draw( backgroundTexture , 0 , 0 , texX ,texY , width , height );
        
        backgroundBatch.end();
        
        // DEBUG: render player/camera positions
        backgroundBatch.begin();
        int y = height-20;
        font.draw(backgroundBatch, "Player position: "+player.position, 10, y );
        y -= 20;
        font.draw(backgroundBatch, "Camera position: "+camera.position, 10, y );
        y -= 20;
        font.draw(backgroundBatch, "Player bounds: "+player.aabb, 10, y );        
        
        backgroundBatch.end();
        
        // render world
        final ShapeRenderer renderer = new ShapeRenderer();
        renderer.setProjectionMatrix( camera.combined );
        world.render( renderer , camera );
        renderer.dispose();
    }

    private void processInput()
    {
        // player movement
        if ( Gdx.input.isKeyPressed(Keys.A) ) {
            player.moveLeft();
        } else if ( Gdx.input.isKeyPressed(Keys.D) ) {
            player.moveRight();
        }
        if ( Gdx.input.isKeyPressed(Keys.W) ) { 
            player.moveUp();
        } else if ( Gdx.input.isKeyPressed(Keys.S) ) {
            player.moveDown();
        }
        
        // handle shooting
        if ( Gdx.input.isButtonPressed( Input.Buttons.LEFT ) ) 
        {
            player.shoot(world);
        }
        
        // update player orientation
        final int x = Gdx.input.getX();
        final int y = Gdx.input.getY();
        
        final Ray ray = camera.getPickRay( x ,y );
        
        Plane xyPlane = new Plane(new Vector3(0, 0, -1f) ,0 );
        Vector3 coords=new Vector3();
        Intersector.intersectRayPlane(ray , xyPlane, coords);
        
        float newOrientX = coords.x - player.position.x;
        float newOrientY = coords.y - player.position.y;
        player.setOrientation( newOrientX , newOrientY );
    }

    private void maybeScrollBackgroundAndMoveCamera()
    {
        // transform player position to view coordinates
        final Vector3 playerPosInScreenCoordinates = new Vector3( player.position.x , player.position.y , 0 );
        camera.project( playerPosInScreenCoordinates ); // world -> screen coordinates
        
        float posDeltaX = 0.0f;
        float posDeltaY = 0.0f;        
        if ( playerPosInScreenCoordinates.x < SCROLL_BORDER_PROXIMITY_THRESHOLD ) // close to left border 
        { 
            posDeltaX = (SCROLL_BORDER_PROXIMITY_THRESHOLD - playerPosInScreenCoordinates.x);
        } 
        else if ( playerPosInScreenCoordinates.x > ( width - SCROLL_BORDER_PROXIMITY_THRESHOLD ) ) // close to right border 
        { 
            posDeltaX = -1.0f*(playerPosInScreenCoordinates.x-(width-SCROLL_BORDER_PROXIMITY_THRESHOLD));
        } 
        
        if ( playerPosInScreenCoordinates.y < SCROLL_BORDER_PROXIMITY_THRESHOLD ) // close to bottom border 
        {
            posDeltaY = SCROLL_BORDER_PROXIMITY_THRESHOLD-playerPosInScreenCoordinates.y;
        } 
        else if ( playerPosInScreenCoordinates.y > ( height - SCROLL_BORDER_PROXIMITY_THRESHOLD ) )  // close to top border 
        {
            posDeltaY = -1.0f*(playerPosInScreenCoordinates.y - (height - SCROLL_BORDER_PROXIMITY_THRESHOLD));
        }
        
        if ( posDeltaX != 0f || posDeltaY != 0f ) 
        {
            backgroundPosition.x -= posDeltaX;
            backgroundPosition.y += posDeltaY;
            
            playerPosInScreenCoordinates.x += posDeltaX;
            playerPosInScreenCoordinates.y += posDeltaY;            
            
            Vector3 playerPosWorldCoordinates = new Vector3(playerPosInScreenCoordinates.x , height - playerPosInScreenCoordinates.y - 1.0f , 0 ); 
            camera.unproject( playerPosWorldCoordinates ); // screen -> world coordinates
            
            Vector3 cameraDelta = new Vector3( player.position.x , player.position.y , camera.position.z ).sub(playerPosWorldCoordinates.x ,playerPosWorldCoordinates.y , 0);
            camera.position.add( cameraDelta );
            camera.update(true);                
        }        
    }
    
    @Override
    public void resize(int width, int height)
    {
        this.width = width;
        this.height = height;
        
        if ( camera == null ) 
        {
        	camera = new OrthographicCamera( width , height );
        } 
        else 
        {
        	Vector3 oldPos = new Vector3(camera.position);
        	Vector3 oldDir = new Vector3(camera.direction);
        	
        	camera = new OrthographicCamera( width , height );
        	camera.position.set(oldPos);
        	camera.direction.set(oldDir);

        	backgroundBatch.dispose();
        	backgroundBatch = new SpriteBatch();
        }
        camera.update();
    }

    @Override
    public void resume() { }

    @Override
    public void show() { }
}