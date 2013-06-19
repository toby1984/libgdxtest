package de.codesourcery.games.libgdxtest.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.collision.BoundingBox;

import de.codesourcery.games.libgdxtest.core.ai.AgentController;

public class GameWorld 
{
	private static final int MAX_TEMPORARY_OBJECTS = 150;

	private final List<ITickListener> tickListeners = new ArrayList<>();  
	private final List<IDrawable> drawables = new ArrayList<>();
	private final List<IDrawable> temporaryObjects = new ArrayList<>(MAX_TEMPORARY_OBJECTS);

	private final List<ITickListener> temporaryObjectsAddedDuringTick = new ArrayList<>(MAX_TEMPORARY_OBJECTS);
	private final List<ITickListener> temporaryObjectsRemovedDuringTick = new ArrayList<>(MAX_TEMPORARY_OBJECTS);        

	private Entity humanPlayer;

	// flag used to signal that we're currently inside tick() and
	// thus iterating over all ITickListeners. While inTick == true, adding/removing
	// tick listeners gets queued until tick() returns. That way I don't have to
	// copy the "tickListeners" ArrayList on each tick just to prevent a ConcurrentModificationException
	private boolean inTick = false;

	private final AgentController agentController = new AgentController();

	public interface IDrawableVisitor<T> {
		public boolean visit(IDrawable d,T data);
	}

	public GameWorld()
	{
		addTickListener( agentController );
	}

	public AgentController getAgentController()
	{
		return agentController;
	}

	public <T> void visitEntities(BoundingBox box,IDrawableVisitor<T> visitor,T data) 
	{
		for ( IDrawable d : drawables ) 
		{
			if ( d.intersects( box ) ) 
			{
				if ( ! visitor.visit( d , data ) ) {
					return;
				}
			}
		}
		if ( humanPlayer.intersects( box ) ) {
			visitor.visit( humanPlayer , data );
		}
	}

	public void render(ShapeRenderer renderer,Camera camera) 
	{
		for ( IDrawable d : drawables ) 
		{
			if ( d.isVisible( camera ) ) 
			{
				d.render( renderer );
			}
		}
		humanPlayer.render(renderer);
	}

	public void setPlayer(Entity p) 
	{
		if (p == null) {
			throw new IllegalArgumentException("p must not be NULL.");
		}
		if ( this.humanPlayer != null ) {
			throw new IllegalStateException("Player already set");
		}
		this.humanPlayer = p;
		addTickListener( p );
	}

	public void addTemporaryObject(IDrawable b) 
	{
		if ( temporaryObjects.size() == MAX_TEMPORARY_OBJECTS ) 
		{
			System.out.println("Max. temp objects exceeded");
			System.out.flush();
			final IDrawable removed = temporaryObjects.remove(0);
			if ( removed instanceof ITickListener) 
			{
				removeTickListener( (ITickListener) removed);
			}
			drawables.remove( removed );
		}
		temporaryObjects.add( b );
		addDrawable( b );
	}

	public Entity getPlayer()
	{
		return humanPlayer;
	}

	public void addDrawable(IDrawable d)
	{
		this.drawables.add( d );
		if ( d instanceof ITickListener) 
		{
			addTickListener( (ITickListener) d);
		}
	}

	public synchronized boolean tick(float deltaSeconds)
	{
		inTick = true;
		try 
		{
			for (Iterator<ITickListener> it = tickListeners.iterator(); it.hasNext();) 
			{
				final ITickListener t = it.next();
				if ( ! t.tick( this , deltaSeconds ) ) 
				{
					it.remove();                
					drawables.remove( t );
					temporaryObjects.remove( t ); 
				}
			}
			return true;
		} 
		finally 
		{
			try 
			{
				if ( ! temporaryObjectsRemovedDuringTick.isEmpty() ) 
				{
					tickListeners.removeAll( temporaryObjectsRemovedDuringTick );
					temporaryObjectsAddedDuringTick.removeAll(temporaryObjectsRemovedDuringTick);
					temporaryObjectsRemovedDuringTick.clear();
				}
				if ( ! temporaryObjectsAddedDuringTick.isEmpty() ) {
					tickListeners.addAll( temporaryObjectsAddedDuringTick );
					temporaryObjectsAddedDuringTick.clear();
				}
			} finally {
				inTick = false;
			}
		}
	}

	public void addAgent(Entity agent1)
	{
		agentController.addAgent( agent1 );
		addDrawable( agent1 );
	}

	private void addTickListener(ITickListener l) 
	{
		if ( inTick ) {
			temporaryObjectsAddedDuringTick.add(l);
		} else {
			tickListeners.add( l );
		}
	}

	private void removeTickListener(ITickListener l) 
	{
		if ( inTick ) {
			temporaryObjectsRemovedDuringTick.add(l);
		} else {
			tickListeners.remove( l );
		}
	}    
}