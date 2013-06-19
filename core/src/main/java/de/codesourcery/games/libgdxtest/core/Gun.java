package de.codesourcery.games.libgdxtest.core;

public class Gun 
{
	public static Gun getDefaultGun() {
		return new Gun();
	}
	
	public int getShotDeltaMilliseconds() {
		return 100;
	}
	
    public float getMaxBulletVelocity() {
    	return 200f;
    }
    
    public float getMaxRangeSquared() {
    	return 250*250;
    }    
}
