package de.codesourcery.games.libgdxtest.core;

import java.util.Random;

import com.badlogic.gdx.math.Vector2;


public abstract class Gun 
{
    private static final float DEG_TO_RAD = (float) Math.PI/180.0f;
    
    private static final Random rnd = new Random(System.currentTimeMillis());
    
    protected final Type type;
    
    protected long lastShotTimestamp;
    protected IBullet lastBullet;
    
    public static enum Type {
        PROJECTILE,
        BEAM;
    }
    
	protected Gun(Type type)
    {
        this.type = type;
    }
	
    public static Gun newDefaultGun() {
		return newProjectileGun();
	}
    
    public static Gun newBeamGun() {
        return new BeamGun();
    }  
    
    public static Gun newProjectileGun() {
        return new ProjectileGun();
    }        
    
    protected final Vector2 perturbAim(Vector2 direction)
    {
        Vector2 aimDir = direction;
        if ( getAccuracy() != 1.0f ) 
        {
            float r = 1.0f-rnd.nextFloat();
            if ( r > getAccuracy() ) 
            {
                // perturb aim by random angle
                
                final float SCALE = 30; 
                float angleInDeg = (1.0f -  rnd.nextFloat() - 0.5f )*SCALE; 
                float angleInRad = angleInDeg * DEG_TO_RAD; 
                
                float cs = (float) Math.cos(angleInRad);
                float sn = (float) Math.sin(angleInRad);
                
                float px = aimDir.x * cs - aimDir.y * sn; 
                float py = aimDir.x * sn + aimDir.y * cs;
                aimDir = new Vector2(px,py);
            } 
        }
        return aimDir;
    }      
    
    public Type getType()
    {
        return type;
    }
    
    public boolean hasType(Type type) {
        return type.equals(getType());
    }
	
	public int getShotIntervalMilliseconds() {
		return 200;
	}
	
	public float getAccuracy() {
	    return 1.0f;
	}
	
    public float getMaxBulletVelocity() {
    	return 50f;
    }
    
    public float getMaxRangeSquared() {
    	return 200*200;
    }
    
    public float getRange() {
        return 200;
    }
    
    public boolean isInRange(Entity shooter,Vector2 target) 
    {
        float dx = target.x - shooter.gunTip.x;
        float dy = target.y - shooter.gunTip.y;
        return (dx*dx+dy*dy) <= getMaxRangeSquared();
    }
    
    public abstract boolean canShoot(long ts); 
    
    protected abstract IBullet doShoot(Entity shooter,GameWorld world);
    
    public boolean shoot(Entity shooter,GameWorld world) 
    {
        final long now = System.currentTimeMillis();
        if ( ! canShoot(now) ) {
            return false;
        }
        
        lastShotTimestamp = now;
        final IBullet bullet = doShoot(  shooter , world );
        lastBullet = bullet;
        world.addTemporaryObject( bullet );   
        return true;
    }
}