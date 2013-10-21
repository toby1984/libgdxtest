package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public class Plane extends SceneObject {

	private final float nx;
	private final float ny;
	private final float nz;
	
	public Plane(Vector3 pointOnPlane,Vector3 planeNormal) 
	{
		super(pointOnPlane);
		
		final Vector3 pn = new Vector3(planeNormal).nor();
		this.nx = pn.x;
		this.ny = pn.y;
		this.nz = pn.z;
	}
	
	@Override
	protected float _distance(float px, float py, float pz) {
		return nx*px + ny*py + nz*pz; // Plane.this.planeNormal.dot( px,py,pz );
	}	
}