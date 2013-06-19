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
    private static final int MAX_TEMPORARY_OBJECTS = 50;
    
    private final List<ITickListener> tickListener = new ArrayList<>();  
    
    private final List<IDrawable> drawables = new ArrayList<>();
    
    private final List<IDrawable> temporaryObjects = new ArrayList<>();
    
    private Entity humanPlayer;
    
    private final AgentController agentController = new AgentController();
    
    public GameWorld()
    {
    }
    
    public AgentController getAgentController()
    {
        return agentController;
    }
    
    public List<IDrawable> getEntities(BoundingBox box) 
    {
        final List<IDrawable>  result = new ArrayList<>();
        for ( IDrawable d : drawables ) 
        {
            if ( d.intersects( box ) ) {
                result.add(d);
            }
        }
        if ( humanPlayer.intersects( box ) ) {
            result.add( humanPlayer );
        }
        return result;
    }
    
    public void render(ShapeRenderer renderer,Camera camera) 
    {
        for ( IDrawable d : drawables ) 
        {
            if ( d.isVisible( camera ) ) 
            {
                d.render( renderer );
            } else {
//                System.out.println("Invisible: "+d);
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
        this.tickListener.add( p );
    }
    
    public void addTemporaryObject(IDrawable b) 
    {
        if ( temporaryObjects.size() > MAX_TEMPORARY_OBJECTS ) 
        {
            IDrawable removed = temporaryObjects.remove(0);
            tickListener.remove( removed );
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
            tickListener.add( (ITickListener) d );
        }
    }
    
    public boolean tick(float deltaSeconds)
    {
        agentController.tick( this , deltaSeconds );
        
        for (Iterator<ITickListener> it = tickListener.iterator(); it.hasNext();) 
        {
            ITickListener t = it.next();
            if ( ! t.tick( this , deltaSeconds ) ) 
            {
                it.remove();                
                drawables.remove( t );
                temporaryObjects.remove( t ); 
            }
        }
        return true;
    }

    public void addAgent(Entity agent1)
    {
        agentController.addAgent( agent1 );
        addDrawable( agent1 );
    }
}