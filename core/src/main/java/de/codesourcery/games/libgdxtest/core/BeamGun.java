package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.math.Vector2;

public final class BeamGun extends Gun {

    public BeamGun()
    {
        super(Gun.Type.BEAM);
    }
    
    public boolean canShoot(long ts) 
    {
        if ( lastShotTimestamp != 0 && ( ts - lastShotTimestamp ) < getShotIntervalMilliseconds() ) {
            return false;
        }
        
        if ( lastBullet == null ) {
            return true;
        }
        
        return ! lastBullet.isAlive(); 
    }
    
    @Override
    public float getAccuracy()
    {
        return 0.1f;
    }

    @Override
    protected IBullet createBullet(Entity shooter, GameWorld world)
    {
        return new Beam( shooter , getRange() ) {

            @Override
            protected Vector2 perturbAim(Vector2 aimDirection)
            {
                return BeamGun.this.applyAccuracy( aimDirection );
            }
        };
    }
}