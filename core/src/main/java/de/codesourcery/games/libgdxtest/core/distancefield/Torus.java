package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public class Torus extends SceneObject {

	public Torus(Vector3 center,final float rInner,final float rOuter) 
	{
		super(center);
		if ( rOuter < 0 ) {
			throw new IllegalArgumentException("rInner must be >= 0");
		}
		if ( rInner < 0 ) {
			throw new IllegalArgumentException("rOuter must be >= 0 and > rInner ");
		}	
		
		final DistanceFunction func = new DistanceFunction() {
			
			@Override
			public float distance(float px, float py, float pz) {
				final float qx = len(px,pz)-rOuter;
				final float qy = py;
				return len(qx,qy) - rInner;				
			}
			private float len(float x,float y) {
				return (float) Math.sqrt(x*x+y*y);
			}			
		};
		this.distanceFunction = func; // repetition( 10, 10 , 10 , func);
	}
	
//	@Override
//	public int getColor(float px, float py, float pz) 
//	{
//		int r= (int) (Math.abs( Math.cos(px*0.5) )*255);
//		int g = (int) (Math.abs( Math.sin(py*2) )*255);
//		int b = (int) (Math.abs( Math.cos(pz*3) )*255);
//		return r << 16 | g << 8 | b;
//	}
}
