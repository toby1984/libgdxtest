package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public final class Plane extends SceneObject {

	private Vector3 pointOnPlane = new Vector3();
	private Vector3 planeNormal = new Vector3();
	
	public Plane(Vector3 pointOnPlane,Vector3 planeNormal) {
		super();
		this.pointOnPlane.set( pointOnPlane );
		this.planeNormal.set(planeNormal);
		this.planeNormal.nor();
	}
	
	@Override
	public float _distance(float px,float py,float pz) 
	{
		float x = px - pointOnPlane.x;
		float y = py - pointOnPlane.y;
		float z = pz - pointOnPlane.z;
		return planeNormal.dot( x,y,z);
	}	

}
