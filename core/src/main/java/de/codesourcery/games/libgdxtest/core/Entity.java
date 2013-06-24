package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;

public final class Entity implements ITickListener,IDrawable
{
    private static final float PLAYER_SPEED = 20f;
    private static final float PLAYER_MAX_VELOCITY = 25f;
    
    public static final float OUTER_RADIUS_IN_PIXELS = 25f;
    public static final float INNER_RADIUS_IN_PIXELS = 15f;
    
    public final BoundingBox aabb = new BoundingBox();
    
    public final Vector2 velocity = new Vector2();
    public final Vector2 gunTip = new Vector2();
    
    public final Vector2 position;
    public final Vector2 orientation;
    
    public boolean isAlive = true;
    
    public float maxShield = 25.0f;
    public float shield;
    
    public Gun gun;
    
    private String name;

    public Entity(String name,Vector2 position,Vector2 orientation) 
    {
        this.name = name;
        this.gun = Gun.newDefaultGun();
        this.shield = maxShield;
        this.position = new Vector2(position);
        this.orientation = new Vector2(orientation).nor();
        updateBounds();
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public Vector2 getAcceleration()
    {
        return velocity;
    }
    
    public void shoot(GameWorld world) 
    {
        gun.shoot( this , world );
    }
    
    public void hitBy(IBullet bullet) 
    {
        if ( shield > 0 ) {
            shield -= 1.0f;
        } else {
            isAlive = false;
        }
    }
    
    public void setAcceleration(float x,float y)
    {
        this.velocity.x = x;
        this.velocity.y = y;
    }
    
    public void setOrientation(float x,float y)
    {
        this.orientation.x = x;
        this.orientation.y = y;
        this.orientation.nor();
        orientationChanged();
    }    
    
    public String getName()
    {
        return name;
    }
    
    private void updateBounds() {
        
        float minX = position.x - OUTER_RADIUS_IN_PIXELS;
        float minY = position.y - OUTER_RADIUS_IN_PIXELS;
        
        float maxX = position.x + OUTER_RADIUS_IN_PIXELS;
        float maxY = position.y + OUTER_RADIUS_IN_PIXELS;
        
        this.aabb.min.set(minX,minY,0);
        this.aabb.max.set(maxX,maxY,0);
    }
    
    @Override
    public void render(ShapeRenderer renderer)
    {
        // render 'player'
        renderer.begin(ShapeType.Filled);
        if ( isAlive ) 
        {
            // render outer shield
            final float shieldPercentage = shield / maxShield;
            final int shieldThickness = Math.round( shieldPercentage * (OUTER_RADIUS_IN_PIXELS-INNER_RADIUS_IN_PIXELS) );
            
            renderer.setColor(Color.YELLOW);
            renderer.circle( position.x , position.y , INNER_RADIUS_IN_PIXELS + shieldThickness );
            
            // render core
            renderer.setColor(Color.BLUE);

            renderer.circle( position.x , position.y , INNER_RADIUS_IN_PIXELS );  
        } else {
            renderer.setColor(Color.BLACK);
            renderer.circle( position.x , position.y , INNER_RADIUS_IN_PIXELS );
        }
        renderer.end();      
        
        // render 'gun'
        renderer.begin(ShapeType.Line);
        renderer.setColor(Color.RED);
        
        renderer.line( position.x , position.y , gunTip.x , gunTip.y );
        renderer.end();        
    }
    
    @Override
    public BoundingBox getBounds()
    {
        return aabb;
    }
    
    public void setGun(Gun gun)
    {
        this.gun = gun;
    }    

    public void moveUp() 
    {
        velocity.y += PLAYER_SPEED;
        clampAcceleration();
        positionChanged();
    }
    
    public void moveDown() {
        velocity.y -= PLAYER_SPEED;
        clampAcceleration();
        positionChanged();
    }
    
    public void moveLeft() {
        velocity.x -= PLAYER_SPEED;
        clampAcceleration();
        positionChanged();
    }
    
    public void moveRight() {
        velocity.x += PLAYER_SPEED;
        clampAcceleration( );
        positionChanged();
    }
    
    private void clampAcceleration() 
    {
        float len = velocity.len();
        if ( len > PLAYER_MAX_VELOCITY) {
            velocity.scl( PLAYER_MAX_VELOCITY / len );
        }
    }    
    
    private void positionChanged() 
    {
        updateBounds();
        updateGunTipPosition();
    }
    
    private void orientationChanged() {
        updateGunTipPosition();
    }   
    
    private void updateGunTipPosition() 
    {
        float orientX = position.x + orientation.x*OUTER_RADIUS_IN_PIXELS*2;
        float orientY = position.y + orientation.y*OUTER_RADIUS_IN_PIXELS*2;
        this.gunTip.set( orientX , orientY );
    }
    
    @Override
    public boolean isVisible(Camera camera)
    {
    	Utils.TMP_3.set(position.x, position.y , 0 );
        camera.project( Utils.TMP_3 );
        
        return Utils.TMP_3 .x >= 0 && Utils.TMP_3 .y >= 0 && 
        		Utils.TMP_3 .x < camera.viewportWidth && Utils.TMP_3 .y < camera.viewportHeight;
    }
    
    @Override
    public boolean tick(GameWorld world,float deltaSeconds)
    {
        if ( isInMotion() ) 
        {
           float deltaPosX = velocity.x * deltaSeconds*20;
           float deltaPosY = velocity.y * deltaSeconds*20;
           
           position.x += deltaPosX;
           position.y += deltaPosY;
           
           positionChanged();
           
           velocity.x *= 0.95f;
           velocity.y *= 0.95f;
        } else {
            velocity.x = velocity.y = 0;
        }
        return isAlive;
    }

    public final boolean isInMotion()
    {
        return Math.abs( velocity.x ) > 0.70 || Math.abs( velocity.y ) > 0.70;
    }

    @Override
    public boolean intersects(BoundingBox box)
    {
        return Utils.intersect( box , getBounds() );
    }
}