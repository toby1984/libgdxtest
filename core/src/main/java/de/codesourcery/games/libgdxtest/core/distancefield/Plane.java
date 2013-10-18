package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public final class Plane extends SceneObject {

	protected Vector3 pointOnPlane = new Vector3();
	protected Vector3 planeNormal = new Vector3();
	
	public Plane(Vector3 pointOnPlane,Vector3 planeNormal) 
	{
		super();
		this.pointOnPlane.set( pointOnPlane );
		this.planeNormal.set(planeNormal);
		this.planeNormal.nor();
		this.distanceFunction = new DistanceFunction() {
			@Override
			public float distance(float px, float py, float pz) {
				float x = px - Plane.this.pointOnPlane.x;
				float y = py - Plane.this.pointOnPlane.y;
				float z = pz - Plane.this.pointOnPlane.z;
				return Plane.this.planeNormal.dot( x,y,z);
			}
		};
	}
}