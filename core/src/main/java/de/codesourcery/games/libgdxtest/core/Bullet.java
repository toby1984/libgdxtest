package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Bullet implements ITickListener,IDrawable
{
	private final BoundingBox aabb=new BoundingBox();

	private final Vector2 initialPosition;
	private final float maxRangeSquared;
	
	private Vector2 position;
	private Vector2 velocity;

	public Bullet(Vector2 position,Vector2 orientation,float velocity,float maxRangeSquared) 
	{
		this.initialPosition = new Vector2(position);
		this.position = new Vector2(position);
		this.velocity = new Vector2(orientation);
		this.velocity.nor().scl(velocity);
		this.maxRangeSquared = maxRangeSquared;
		updateBoundingBox();
	}

	private void updateBoundingBox()
	{
		aabb.min.set( position.x-1, position.y-1 ,0 );
		aabb.max.set( position.y+1 , position.y +1 , 0);        
	}

	@Override
	public BoundingBox getBounds()
	{
		return aabb;
	}

	@Override
	public boolean isVisible(Frustum f)
	{
		return f.pointInFrustum( new Vector3( position.x,position.y , 0 ) );
	}

	@Override
	public void render(ShapeRenderer renderer)
	{
		renderer.begin(ShapeType.Filled);
		renderer.setColor(Color.RED);
		renderer.circle( position.x , position.y , 5 );
		renderer.end();
	}

	@Override
	public boolean tick(float deltaSeconds)
	{
		float dx = position.x - initialPosition.x;
		float dy = position.y - initialPosition.y;

		if ( (dx*dx+dy*dy) > maxRangeSquared ) {
			return false;
		}
		position.x += (velocity.x * deltaSeconds);
		position.y += (velocity.y * deltaSeconds);
		return true;
	}
}
