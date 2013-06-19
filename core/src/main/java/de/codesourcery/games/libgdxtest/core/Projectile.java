package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Projectile extends Bullet
{
    private static final int RADIUS_IN_PIXELS = 5;
    
	private final Vector2 initialPosition;
	private final float maxRangeSquared;
	
	private Vector2 position;
	private Vector2 velocity;
	private boolean isAlive = true;

	public Projectile(Entity shooter,Vector2 direction,float velocity,float maxRangeSquared) 
	{
	    super(shooter);
		this.initialPosition = new Vector2(shooter.gunTip);
		this.position = new Vector2(shooter.gunTip);
		
		this.velocity = new Vector2(direction);
		this.velocity.nor().scl(velocity);
		this.maxRangeSquared = maxRangeSquared;
		updateBoundingBox();
	}

	private void updateBoundingBox()
	{
		aabb.min.set( position.x-1, position.y-1 ,0 );
		aabb.max.set( position.x+1 , position.y +1 , 0);        
	}
	
	@Override
	public BoundingBox getBounds()
	{
		return aabb;
	}

	@Override
	public boolean isVisible(Camera camera)
	{
		return camera.frustum.pointInFrustum( new Vector3( position.x,position.y , 0 ) );
	}

    @Override
    public boolean intersects(BoundingBox box)
    {
        return Utils.intersect( getBounds() , box );
    }
    
	@Override
	public void render(ShapeRenderer renderer)
	{
		renderer.begin(ShapeType.Filled);
		renderer.setColor(Color.RED);
		renderer.circle( position.x , position.y , RADIUS_IN_PIXELS );
		renderer.end();
		
		// DEBUG
//        renderer.begin(ShapeType.Line);		
//        renderer.setColor(Color.GREEN);
//        
//        renderer.rect( aabb.min.x , aabb.min.y , aabb.max.x - aabb.min.x  , aabb.max.y - aabb.min.y );
//        renderer.end();        
	}

	@Override
	public boolean tick(GameWorld world,float deltaSeconds)
	{
	    if ( ! isAlive ) 
	    {
	        return false;
	    }
	    
        for ( IDrawable hit : world.getEntities( getBounds() )) 
        {
            if ( hit instanceof Entity  && hit != shooter) {
                isAlive = false;
                ((Entity) hit).hitBy(this);
                return false;
            }
        }	 
        
		float dx = position.x - initialPosition.x;
		float dy = position.y - initialPosition.y;

		if ( (dx*dx+dy*dy) > maxRangeSquared ) 
		{
		    isAlive=false;
			return false;
		}
		
		position.x += (velocity.x * deltaSeconds);
		position.y += (velocity.y * deltaSeconds);
		
		updateBoundingBox();
		
		return true;
	}
	
	@Override
	public String toString()
	{
	    return "projectile from "+shooter;
	}

    @Override
    public boolean isAlive()
    {
        return isAlive;
    }
}