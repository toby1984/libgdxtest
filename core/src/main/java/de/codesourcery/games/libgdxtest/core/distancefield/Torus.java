package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public final class Torus extends SceneObject {

	private final float rInner;
	private final float rOuter;
	
	public Torus(Vector3 center,final float rInner,final float rOuter) 
	{
		super(center);
		if ( rOuter < 0 ) {
			throw new IllegalArgumentException("rInner must be >= 0");
		}
		if ( rInner < 0 ) {
			throw new IllegalArgumentException("rOuter must be >= 0 and > rInner ");
		}	
		this.rInner = rInner;
		this.rOuter = rOuter;
	}

	@Override
	protected float _distance(float px, float py, float pz) {
		final float qx = len(px,pz)-rOuter;
		final float qy = py;
		return len(qx,qy) - rInner;				
	}
	
	private float len(float x,float y) {
		return (float) Math.sqrt(x*x+y*y);
	}	
}
