package de.codesourcery.games.libgdxtest.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Frustum;

public class GameWorld implements ITickListener
{
    private static final int MAX_BULLETS = 50;
    
    private final List<ITickListener> tickAware = new ArrayList<>();    
    private final List<IDrawable> drawables = new ArrayList<>();
    
    private final List<Bullet> bullets = new ArrayList<>();
    private Player player;
    
    public GameWorld()
    {
    }
    
    public void render(ShapeRenderer renderer,Camera camera) 
    {
        final Frustum frustum = camera.frustum;
        for ( IDrawable d : drawables ) 
        {
            if ( d.isVisible( frustum ) ) 
            {
                d.render( renderer );
            } 
        }
        player.render(renderer);
    }
    
    public void setPlayer(Player p) 
    {
        if (p == null) {
            throw new IllegalArgumentException("p must not be NULL.");
        }
        if ( this.player != null ) {
            throw new IllegalStateException("Player already set");
        }
        this.player = p;
        this.tickAware.add( p );
    }
    
    public void addBullet(Bullet b) 
    {
        if ( bullets.size() > MAX_BULLETS ) 
        {
            Bullet removed = bullets.remove(0);
            tickAware.remove( removed );
            drawables.remove( removed );
        }
        bullets.add( b );
        addDrawable( b );
    }
    
    public Player getPlayer()
    {
        return player;
    }

    public void addDrawable(IDrawable d)
    {
        this.drawables.add( d );
        if ( d instanceof ITickListener) {
            tickAware.add( (ITickListener) d );
        }
    }
    
    public List<IDrawable> getDrawables(Camera camera) 
    {
        final List<IDrawable> result = new ArrayList<>();
        final Frustum frustum = camera.frustum;
        for ( IDrawable d : drawables ) 
        {
            if ( d.isVisible( frustum ) ) 
            {
                result.add( d );
            } 
        }
        return result;
    }

    @Override
    public boolean tick(float delta)
    {
        for (Iterator<ITickListener> it = tickAware.iterator(); it.hasNext();) 
        {
            ITickListener t = it.next();
            if ( ! t.tick( delta ) ) 
            {
                it.remove();                
                drawables.remove( t );
                bullets.remove( t ); // TODO: Inefficient for everything except Bullets...
            }
        }
        return true;
    }
}