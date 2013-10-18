package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public class Cube extends SceneObject {

	private final float size;
	
	public Cube(Vector3 center,float size) 
	{
		super(center);
		if (size < 0 ) {
			throw new IllegalArgumentException("size must be >= 0");
		}
		this.size = size; 
	}
	
	@Override
	public float _distance(float px, float py, float pz) 
	{
		float dx = Math.abs( px ) - size;
		float dy = Math.abs( py ) - size;
		float dz = Math.abs( pz ) - size;
		
		float maxDx = Math.max( dx ,  0 );
		float maxDy = Math.max( dy ,  0 );
		float maxDz = Math.max( dz ,  0 );
		
	    return length( maxDx , maxDy , maxDz );
	}
	
	private float length(float x,float y,float z) {
		return (float) Math.sqrt(x*x+y*y+z*z);
	}

}
