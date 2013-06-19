package de.codesourcery.games.libgdxtest.core.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.math.collision.BoundingBox;

import de.codesourcery.games.libgdxtest.core.Entity;
import de.codesourcery.games.libgdxtest.core.GameWorld;
import de.codesourcery.games.libgdxtest.core.GameWorld.IDrawableVisitor;
import de.codesourcery.games.libgdxtest.core.IDrawable;
import de.codesourcery.games.libgdxtest.core.ITickListener;
import de.codesourcery.games.libgdxtest.core.Utils;

public class AgentController implements ITickListener
{
    public static final float VIEW_RANGE = 200;
    
    private final BoundingBox tmpBounds = new BoundingBox();
    
    private final IState SEARCHING = new Searching();
    
    private final Map<Entity,StateHolder> statesByAgent = new HashMap<>();

    public interface IState {
        public IState act(Entity agent,GameWorld world);
    }
    
    protected static final class StateHolder 
    {
        public IState currentState;

        public StateHolder(IState currentState)
        {
            this.currentState = currentState;
        }
    }

    protected static final class FindNearestResult 
    {
    	public final Entity queryingAgent;
    	public Entity closestTarget = null;
        public float distanceSquared = Float.MAX_VALUE;
        
		public FindNearestResult(Entity queryingAgent) 
		{
			this.queryingAgent = queryingAgent;
		}
    }
    
    protected final class Searching implements IState {

    	final IDrawableVisitor<FindNearestResult> visitor = new IDrawableVisitor<FindNearestResult>() {
			
			@Override
			public boolean visit(IDrawable d,FindNearestResult result) 
			{
				if ( d != result.queryingAgent && d instanceof Entity) 
				{
		            final float dist = Utils.squaredDistance(result.queryingAgent,(Entity) d);
					if ( result.closestTarget == null || dist < result.distanceSquared ) 
					{
						result.closestTarget=(Entity)d;
						result.distanceSquared=dist;
					}
				}
				return true;
			}
		};
		
		
        @Override
        public String toString()
        {
            return "SEARCHING";
        }
        
        @Override
        public IState act(final Entity agent,GameWorld world)
        {
        	final FindNearestResult nearest = new FindNearestResult(agent);
            
			getAgentFieldOfView(agent, tmpBounds );
			world.visitEntities( tmpBounds , visitor , nearest );
			
            return nearest.closestTarget == null ? this : new Attacking(nearest.closestTarget);
        }
    }    
    
    protected final class Attacking implements IState 
    {
        private final Entity target;
        
        private Attacking(Entity target)
        {
            this.target = target;
        }
        
        @Override
        public String toString()
        {
            return "ATTACKING "+target;
        }        

        @Override
        public IState act(Entity agent,GameWorld world)
        {
            if ( ! target.isAlive ) {
                return SEARCHING;
            }
            
            float distanceSquared = Utils.squaredDistance( agent,target);
            
            // if target is too far away to reach it, move closer first
            if ( distanceSquared > agent.gun.getMaxRangeSquared() ) {
                return new MoveTowards( target , this );
            }
            
            // target is close enough, aim and shoot
            float aimX = target.position.x - agent.position.x;
            float aimY = target.position.y - agent.position.y;
            
            agent.setOrientation( aimX , aimY );
            agent.shoot( world );
            return this;
        }
    }
    
    protected final class MoveTowards implements IState 
    {
        private final Entity target;
        private final IState nextState;
        
        private MoveTowards(Entity target,IState nextState)
        {
            this.target = target;
            this.nextState = nextState;
        }
        
        @Override
        public String toString()
        {
            return "MOVE_TOWARDS "+target;
        }           
        
        @Override
        public IState act(Entity agent,GameWorld world)
        {
            if ( target.isAlive ) 
            {
                float dx = target.position.x - agent.position.x;
                if ( Math.abs(dx) > 10 ) 
                {
                    if ( dx > 0 ) {
                        agent.moveRight();
                    } else {
                        agent.moveLeft();
                    }
                }
                float dy = target.position.y - agent.position.y;
                if ( Math.abs(dy) > 10 ) 
                {
                    if ( dy > 0 ) {
                        agent.moveUp();
                    } else {
                        agent.moveDown();
                    }
                }     
            }
            return nextState;
        }
        
    }
    
    public void addAgent(Entity a) 
    {
        if (a == null) {
            throw new IllegalArgumentException("agent must not be NULL.");
        }
        this.statesByAgent.put( a , new StateHolder(SEARCHING) );
    }
    
    protected void getAgentFieldOfView(Entity agent,BoundingBox bounds)
    {
        float minX = agent.position.x - VIEW_RANGE;
        float maxX = agent.position.x + VIEW_RANGE;
        
        float minY = agent.position.y - VIEW_RANGE;
        float maxY = agent.position.y + VIEW_RANGE;        
        
        bounds.min.set( minX , minY , 0 );
        bounds.max.set( maxX , maxY , 0 );
    }

    @Override
    public boolean tick(GameWorld world,float deltaSeconds)
    {
        for ( Entry<Entity, StateHolder> entry : statesByAgent.entrySet() ) 
        {
        	if ( entry.getKey().isAlive ) 
        	{
        		final IState newState = entry.getValue().currentState.act( entry.getKey() , world );
        		entry.getValue().currentState = newState;
        	}
        }
        return true;
    }
}
