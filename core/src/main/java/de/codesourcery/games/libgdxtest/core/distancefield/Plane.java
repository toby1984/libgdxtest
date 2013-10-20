package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public final class Plane extends SceneObject {

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
	
	public int getColor(float px,float py,float pz) 
	{
		boolean bx = Math.abs(px % 2) < 1;
		boolean bz = Math.abs(pz % 2) < 1;
		if ( bx  && bz || !bx && ! bz) {
			return 0x00ffffff; 
		} 
		return 0;
	}	
}