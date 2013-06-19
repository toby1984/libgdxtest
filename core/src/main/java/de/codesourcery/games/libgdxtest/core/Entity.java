package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public final class Entity implements ITickListener,IDrawable
{
    private static final float PLAYER_SPEED = 20f;
    private static final float PLAYER_MAX_ACCELERATION = 25f;
    
    public static final float OUTER_RADIUS_IN_PIXELS = 25f;
    public static final float INNER_RADIUS_IN_PIXELS = 15f;
    
    public final BoundingBox aabb = new BoundingBox();
    
    public final Vector2 acceleration = new Vector2();
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
        return acceleration;
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
        this.acceleration.x = x;
        this.acceleration.y = y;
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
        acceleration.y += PLAYER_SPEED;
        clampAcceleration();
        positionChanged();
    }
    
    public void moveDown() {
        acceleration.y -= PLAYER_SPEED;
        clampAcceleration();
        positionChanged();
    }
    
    public void moveLeft() {
        acceleration.x -= PLAYER_SPEED;
        clampAcceleration();
        positionChanged();
    }
    
    public void moveRight() {
        acceleration.x += PLAYER_SPEED;
        clampAcceleration( );
        positionChanged();
    }
    
    private void clampAcceleration() 
    {
        float len = acceleration.len();
        if ( len > PLAYER_MAX_ACCELERATION) {
            acceleration.scl( PLAYER_MAX_ACCELERATION / len );
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
        Vector3 screenCoords = new Vector3(position.x, position.y , 0 );
        camera.project( screenCoords );
        
        return screenCoords.x >= 0 && screenCoords.y >= 0 && 
                screenCoords.x < camera.viewportWidth && screenCoords.y < camera.viewportHeight;
    }
    
    @Override
    public boolean tick(GameWorld world,float deltaSeconds)
    {
        if ( isInMotion() ) 
        {
           float deltaPosX = acceleration.x * deltaSeconds*20;
           float deltaPosY = acceleration.y * deltaSeconds*20;
           
           position.x += deltaPosX;
           position.y += deltaPosY;
           
           positionChanged();
           
           acceleration.x *= 0.95f;
           acceleration.y *= 0.95f;
        } else {
            acceleration.x = acceleration.y = 0;
        }
        return isAlive;
    }

    public final boolean isInMotion()
    {
        return Math.abs( acceleration.x ) > 0.70 || Math.abs( acceleration.y ) > 0.70;
    }

    @Override
    public boolean intersects(BoundingBox box)
    {
        return Utils.intersect( box , getBounds() );
    }
}