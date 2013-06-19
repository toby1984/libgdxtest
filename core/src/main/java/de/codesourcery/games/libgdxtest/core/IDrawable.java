package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.collision.BoundingBox;

public interface IDrawable
{
    public abstract BoundingBox getBounds();
    
    public abstract boolean isVisible(Frustum frustum);
    
    public abstract void render(ShapeRenderer renderer);
    
}
