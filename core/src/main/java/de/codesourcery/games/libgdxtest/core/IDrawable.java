package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.collision.BoundingBox;

public interface IDrawable
{
    public abstract BoundingBox getBounds();
    
    public abstract boolean isVisible(Camera camera);
    
    public abstract boolean intersects(BoundingBox box);
    
    public abstract void render(ShapeRenderer renderer);
}
