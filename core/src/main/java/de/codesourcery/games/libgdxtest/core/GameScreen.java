package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.games.libgdxtest.core.world.ChunkManager;
import de.codesourcery.games.libgdxtest.core.world.DefaultTileManager;
import de.codesourcery.games.libgdxtest.core.world.TileFactory;

public class GameScreen implements Screen
{
    private static final float SCROLL_BORDER_PROXIMITY_THRESHOLD = Entity.OUTER_RADIUS_IN_PIXELS*4; 
    
    private int width;
    private int height;
    
    private final GameWorld world;
    private final Entity player = new Entity("Player #1", new Vector2(0,0) , new Vector2( 1,1 ) ) ;
    
    private final ChunkManager chunkManager;
    
    private BitmapFont font;
    
    // background
    private SpriteBatch backgroundBatch;
    
    private ShapeRenderer shapeRenderer;
    
    public GameScreen() 
    {
        Gdx.graphics.setDisplayMode( 400, 400 , true );
        chunkManager = new ChunkManager();
        chunkManager.setTileManager( new DefaultTileManager( new TileFactory(0xdeadbeef) ) );
        
        backgroundBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
    	setupTextRendering();
    	
        world = new GameWorld();
        world.setPlayer( player );
        
        final Entity agent1 = new Entity("Agent #1", new Vector2(50,50) , new Vector2( 1,1 ) ) ;
//        world.addAgent( agent1 );
    }
    
    private void setupTextRendering() 
    {
  		if ( font != null ) {
  		    font.dispose();
  		}
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
        chunkManager.camera.apply( Gdx.gl10 );
        
        // process input
        processInput();
        
        // render world
        renderWorld(deltaSeconds);
        
        // advance world state
        tick(deltaSeconds);     
    }
    
    private void tick(float deltaSeconds) 
    {
    	// memorize motion state BEFORE invoking tick(),even
    	// if the player came to a halt after tick() we still might
    	// need to adjust the camera position
        final boolean playerHasMoved = player.isInMotion(); 
        world.tick( deltaSeconds );
        
        if (  playerHasMoved ) 
        {
            maybeMoveCamera();
        }
    }
    
    private void renderWorld(float deltaSeconds)
    {
        // clear display
        Gdx.graphics.getGL10().glClearColor( 0 , 0, 0, 1 );
        Gdx.graphics.getGL10().glClear( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
        
        chunkManager.camera.apply( Gdx.gl10 );
        
        // render background
        chunkManager.renderCurrentChunk( backgroundBatch );
        
        // DEBUG: render player/camera positions

        backgroundBatch.begin();
        backgroundBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()) );
        int y = height-20;
        font.draw(backgroundBatch, "Player position: "+player.position, 10, y );
        y -= 20;
        font.draw(backgroundBatch, "Camera position: "+chunkManager.camera.position, 10, y );
        y -= 20;
        font.draw(backgroundBatch, "Current tile: "+chunkManager.cameraTileX+" / "+chunkManager.cameraTileY,10,y);
        y -= 20;
        font.draw(backgroundBatch, "FPS: "+Gdx.graphics.getFramesPerSecond(), 10, y );        	
        
        backgroundBatch.end();
        
        // render world
        shapeRenderer.setProjectionMatrix( chunkManager.camera.combined );
        world.render( shapeRenderer , chunkManager.camera );
    }
    
    private void processInput()
    {
        // player movement
        if ( Gdx.input.isKeyPressed(Keys.A) ) {
            player.moveLeft();
        } 
        else if ( Gdx.input.isKeyPressed(Keys.D) ) 
        {
            player.moveRight();
        }
        
        if ( Gdx.input.isKeyPressed(Keys.W) ) { 
            player.moveUp();
        } 
        else if ( Gdx.input.isKeyPressed(Keys.S) ) {
            player.moveDown();
        }
        
        // handle shooting
        if ( Gdx.input.isButtonPressed( Input.Buttons.LEFT ) ) 
        {
            player.shoot(world);
        }
        
        // update player orientation to always point where
        // the mouse pointer is
        final int x = Gdx.input.getX();
        final int y = Gdx.input.getY();
        
        final Vector3 coords=Utils.TMP_3;
        coords.set( x , y , 0 );
        chunkManager.camera.unproject( coords );
        
        float newOrientX = coords.x - player.position.x;
        float newOrientY = coords.y - player.position.y;
        player.setOrientation( newOrientX , newOrientY );
    }

    private void maybeMoveCamera()
    {
        // transform player position to view coordinates
        final Vector3 playerPosInScreenCoordinates = new Vector3( player.position.x , player.position.y , 0 );
        chunkManager.camera.project( playerPosInScreenCoordinates ); // world -> screen coordinates
        
        Vector3 initialScreenCoordinates = new Vector3(playerPosInScreenCoordinates);
        
        float posDeltaX = 0.0f;
        float posDeltaY = 0.0f;        
        if ( playerPosInScreenCoordinates.x < SCROLL_BORDER_PROXIMITY_THRESHOLD ) // close to left border 
        { 
            posDeltaX = -1.0f*(SCROLL_BORDER_PROXIMITY_THRESHOLD - playerPosInScreenCoordinates.x);
            if ( Math.abs(posDeltaX) > SCROLL_BORDER_PROXIMITY_THRESHOLD ) {
                System.out.println("X1: Player moved too fast: "+posDeltaX);
                posDeltaX = posDeltaX % SCROLL_BORDER_PROXIMITY_THRESHOLD;                
            }
        } 
        else if ( playerPosInScreenCoordinates.x > ( width - SCROLL_BORDER_PROXIMITY_THRESHOLD ) ) // close to right border 
        { 
            posDeltaX = (playerPosInScreenCoordinates.x-(width-SCROLL_BORDER_PROXIMITY_THRESHOLD));
            if ( Math.abs(posDeltaX) > SCROLL_BORDER_PROXIMITY_THRESHOLD ) {
                System.out.println("X2: Player moved too fast: "+posDeltaX);
                posDeltaX = posDeltaX % SCROLL_BORDER_PROXIMITY_THRESHOLD;
            }            
        } 
        
        if ( playerPosInScreenCoordinates.y < SCROLL_BORDER_PROXIMITY_THRESHOLD ) // close to bottom border 
        {
            posDeltaY = (SCROLL_BORDER_PROXIMITY_THRESHOLD-playerPosInScreenCoordinates.y);
            if ( Math.abs(posDeltaY) > SCROLL_BORDER_PROXIMITY_THRESHOLD ) {
                System.out.println("Y1: Player moved too fast: "+posDeltaY);
                posDeltaY = posDeltaY % SCROLL_BORDER_PROXIMITY_THRESHOLD;
            }               
        } 
        else if ( playerPosInScreenCoordinates.y > ( height - SCROLL_BORDER_PROXIMITY_THRESHOLD ) )  // close to top border 
        {
            posDeltaY = -1.0f*(playerPosInScreenCoordinates.y - (height - SCROLL_BORDER_PROXIMITY_THRESHOLD));
            if ( Math.abs(posDeltaY) > SCROLL_BORDER_PROXIMITY_THRESHOLD ) {
                System.out.println("Y2: Player moved too fast: "+posDeltaY);
                posDeltaY = posDeltaY % SCROLL_BORDER_PROXIMITY_THRESHOLD;                
            }             
        }
        
        if ( posDeltaX != 0f || posDeltaY != 0f ) 
        {
            Vector3 debugPlayerWorldCoords = new Vector3(playerPosInScreenCoordinates.x , height - playerPosInScreenCoordinates.y - 1.0f , 0 ); 
            chunkManager.camera.unproject( debugPlayerWorldCoords ); // screen -> world coordinates
            
            playerPosInScreenCoordinates.x -= posDeltaX;
            playerPosInScreenCoordinates.y += posDeltaY;            
            
            Vector3 playerPosWorldCoordinates = new Vector3(playerPosInScreenCoordinates.x , height - playerPosInScreenCoordinates.y - 1.0f , 0 ); 
            chunkManager.camera.unproject( playerPosWorldCoordinates ); // screen -> world coordinates
            
            Vector3 cameraDelta = new Vector3( player.position.x , player.position.y , chunkManager.camera.position.z )
                .sub(playerPosWorldCoordinates.x ,playerPosWorldCoordinates.y , 0);
            
            try 
            {
                chunkManager.moveCameraRelative( cameraDelta.x , cameraDelta.y );
            } 
            catch(IllegalArgumentException e) 
            {
                System.out.println("Initial player coordinates: "+player.position);
                System.out.println("Initial screen coordinates: "+initialScreenCoordinates);
                System.out.println("Player world coordinates  : "+debugPlayerWorldCoords);
                System.out.println("Offending delta in screen coordinates: "+posDeltaX+" / "+posDeltaY+" (player position: "+player.position+")");
                throw e;
            }
        }        
    }
    
    @Override
    public void resize(int width, int height)
    {
        System.out.println("Viewport size: "+width+" / "+height);
        this.width = width;
        this.height = height;
        
        chunkManager.viewportResized( width , height );
        
    	backgroundBatch.dispose();
    	backgroundBatch = new SpriteBatch();
    	
    	shapeRenderer.dispose();
    	shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void resume() { }

    @Override
    public void show() { }
}