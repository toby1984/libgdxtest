package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;

public final class Player implements ITickListener,IDrawable
{
    private static final float PLAYER_SPEED = 20f;
    private static final float PLAYER_MAX_ACCELERATION = 25f;
    
    public static final float RADIUS = 25f;
    
    private final BoundingBox bounds = new BoundingBox();
    
    public Vector2 position;
    public Vector2 orientation;
    public final Vector2 acceleration = new Vector2();
    
    private Gun gun = Gun.getDefaultGun();
    
    private long lastShotTimestamp=0;
    
    private String name;

    public Player(String name,Vector2 position,Vector2 orientation) 
    {
        this.name = name;
        this.position = new Vector2(position);
        this.orientation = new Vector2(orientation).nor();
        updateBounds();
    }
    
    public Vector2 getAcceleration()
    {
        return acceleration;
    }
    
    public void shoot(GameWorld world) 
    {
        final long now = System.currentTimeMillis();
        if ( lastShotTimestamp == 0 || (now - lastShotTimestamp) > gun.getShotDeltaMilliseconds() ) 
        {
            lastShotTimestamp = now;
            world.addBullet( new Bullet( calcGunTip()  , orientation , gun.getMaxBulletVelocity() , gun.getMaxRangeSquared() ) );
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
    }    
    
    public String getName()
    {
        return name;
    }
    
    private void updateBounds() {
        
        float minX = position.x - RADIUS;
        float minY = position.y - RADIUS;
        
        float maxX = position.x + RADIUS;
        float maxY = position.y + RADIUS;
        
        this.bounds.min.set(minX,minY,0);
        this.bounds.max.set(maxX,maxY,0);
    }
    
    @Override
    public void render(ShapeRenderer renderer)
    {
        renderer.begin(ShapeType.Filled);
        renderer.setColor(Color.BLUE);
        renderer.circle( position.x , position.y , RADIUS );
        renderer.end();
        
        renderer.begin(ShapeType.Line);
        renderer.setColor(Color.RED);
        
        final Vector2 orient = calcGunTip();
        
        renderer.line( position.x , position.y , orient.x , orient.y );
        renderer.end();        
    }
    
    private Vector2 calcGunTip() {
        float orientX = position.x + orientation.x*RADIUS*2;
        float orientY = position.y + orientation.y*RADIUS*2;
        return new Vector2(orientX,orientY);
    }

    @Override
    public BoundingBox getBounds()
    {
        return bounds;
    }

    public void moveUp() 
    {
        acceleration.y += PLAYER_SPEED;
        clampAcceleration();
    }
    
    public void moveDown() {
        acceleration.y -= PLAYER_SPEED;
        clampAcceleration();
    }
    
    public void moveLeft() {
        acceleration.x -= PLAYER_SPEED;
        clampAcceleration();
    }
    
    public void moveRight() {
        acceleration.x += PLAYER_SPEED;
        clampAcceleration( );
    }
    
    private void clampAcceleration() {
        float len = acceleration.len();
        if ( len > PLAYER_MAX_ACCELERATION) {
            acceleration.scl( PLAYER_MAX_ACCELERATION / len );
        }
    }    
    
    @Override
    public boolean isVisible(Frustum f)
    {
        updateBounds();
        return f.boundsInFrustum( getBounds() );
    }
    
    @Override
    public boolean tick(float deltaSeconds)
    {
        if ( isInMotion() ) 
        {
           float deltaPosX = acceleration.x * deltaSeconds*20;
           float deltaPosY = acceleration.y * deltaSeconds*20;
           
           position.x += deltaPosX;
           position.y += deltaPosY;
           
           acceleration.x *= 0.95f;
           acceleration.y *= 0.95f;
        } else {
            acceleration.x = acceleration.y = 0;
        }
        return true;
    }

    public final boolean isInMotion()
    {
        return Math.abs( acceleration.x ) > 0.70 || Math.abs( acceleration.y ) > 0.70;
    }
}