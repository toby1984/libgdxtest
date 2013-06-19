package de.codesourcery.games.libgdxtest.core;

public interface IBullet extends IDrawable,ITickListener
{
    public boolean isAlive();
    
    public Entity getShooter();
}
