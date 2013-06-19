package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;

import de.codesourcery.games.libgdxtest.core.GameWorld.IDrawableVisitor;

public abstract class Beam extends Bullet
{
    private final Vector2 target;
    
    private static final IDrawableVisitor<Beam> tickVisitor = new IDrawableVisitor<Beam>() {
		
		@Override
		public boolean visit(IDrawable hit,Beam beam) 
		{
            if ( hit instanceof Entity && hit != beam.shooter ) {
                beam.aliveTicks=0;
                ((Entity) hit).hitBy(beam);
            }			
			return true;
		}
	};
	
    private int aliveTicks = 10;

    public Beam(Entity shooter,float range) 
    {
        super(shooter);
        this.target = perturbAim( new Vector2(shooter.orientation).scl(range) );
        updateTarget();
    }
    
    @Override
    public boolean intersects(BoundingBox box)
    {
        return Utils.intersect( getBounds() , box );
    }
    
    private void updateBoundingBox(Vector2 target)
    {
        float minX = Math.min( shooter.gunTip.x , target.x );
        float minY = Math.min( shooter.gunTip.y , target.y );

        float maxX = Math.max( shooter.gunTip.x , target.x );
        float maxY = Math.max( shooter.gunTip.y , target.y );        
        
        aabb.min.set( minX,minY,0);
        aabb.max.set( maxX,maxY,0);
    }
    
    @Override
    public BoundingBox getBounds()
    {
        return aabb;
    }

    @Override
    public boolean isVisible(Camera camera)
    {
        return camera.frustum.boundsInFrustum( getBounds() );
    }
    
    protected abstract Vector2 perturbAim(Vector2 aimDirection);
    
    private void updateTarget() 
    {
        updateBoundingBox(target);        
    }

    @Override
    public void render(ShapeRenderer renderer)
    {
        updateTarget();
        
        renderer.begin(ShapeType.Line);
        renderer.setColor(Color.RED);
        renderer.line( shooter.gunTip.x , shooter.gunTip.y , target.x ,target.y );
        renderer.end();
    }
    
    @Override
    public boolean tick(GameWorld world,float deltaSeconds)
    {
    	world.visitEntities( getBounds() , tickVisitor , this );
        return aliveTicks-- > 0;
    }
    
    @Override
    public String toString()
    {
        return "beam from "+shooter;
    }    

    @Override
    public boolean isAlive()
    {
        return aliveTicks>0;
    }
}