package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public class Torus extends SceneObject {

	private final float rOuter;
	private final float rInner;

	public Torus(Vector3 center,float rInner,float rOuter) 
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
	public float _distance(float px, float py, float pz) 
	{
		final float qx = len(px,pz)-rOuter;
		final float qy = py;
		return len(qx,qy) - rInner;
	}

	private float len(float x,float y) {
		return (float) Math.sqrt(x*x+y*y);
	}

}
