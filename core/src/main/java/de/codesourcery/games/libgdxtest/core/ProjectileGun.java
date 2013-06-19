package de.codesourcery.games.libgdxtest.core;


public final class ProjectileGun extends Gun {
    
    public ProjectileGun()
    {
        super(Gun.Type.PROJECTILE);
    }
    
    public boolean canShoot(long ts) 
    {
        if ( lastShotTimestamp != 0 && ( ts - lastShotTimestamp ) < getShotIntervalMilliseconds() ) {
            return false;
        }
        return true;
    }      
    
    @Override
    public float getAccuracy()
    {
        return 0.1f;
    }
    
    @Override
    protected IBullet createBullet(Entity shooter, GameWorld world)
    {
        return new Projectile( shooter , applyAccuracy(shooter.orientation), getMaxBulletVelocity() , getMaxRangeSquared() );
    }
}