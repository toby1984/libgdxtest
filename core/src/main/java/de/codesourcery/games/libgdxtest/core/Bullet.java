package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.math.collision.BoundingBox;

public abstract class Bullet implements IBullet
{
    protected final BoundingBox aabb=new BoundingBox();    
    
    protected final Entity shooter;
    
    public Bullet(Entity shooter) {
        this.shooter = shooter;
    }

    @Override
    public final Entity getShooter()
    {
        return shooter;
    }

}
