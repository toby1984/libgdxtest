package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public abstract class SceneObject {

	// matrix used to convert points to object space
	public final Matrix4 matrix = new Matrix4().idt();
	public final Vector3 center = new Vector3();
	
	protected int color;
	
	public boolean smoothBlend;
	
	protected SceneObject() {
	}
	
	public final void setSmoothBlend(boolean smoothBlend) {
		this.smoothBlend = smoothBlend;
	}
	
	public final SceneObject setColor(int color) {
		this.color = color;
		return this;
	}
	
	protected SceneObject(Vector3 center) {
		this.center.set(center);
		matrix.idt();
		matrix.translate( -center.x , -center.y , -center.z );
	}
	
	public SceneObject rotate(Vector3 axis,float degrees) 
	{
		matrix.idt();		
		matrix.rotate( axis , degrees );
		matrix.translate( -center.x , -center.y , -center.z );		
		return this;
	}

	public final float distance(float px, float py, float pz) 
	{
		final float coeff[] = matrix.val;
		// transform point to object space
		float newX = px * coeff[Matrix4.M00] + py * coeff[Matrix4.M01] + pz * coeff[Matrix4.M02] + coeff[Matrix4.M03];
		float newY = px * coeff[Matrix4.M10] + py * coeff[Matrix4.M11] + pz * coeff[Matrix4.M12] + coeff[Matrix4.M13];
		float newZ = px * coeff[Matrix4.M20] + py * coeff[Matrix4.M21] + pz * coeff[Matrix4.M22] + coeff[Matrix4.M23];
		return _distance(newX,newY,newZ);
	}
	
	protected abstract float _distance(float px, float py, float pz);
	
	public int getColor(float px,float py,float pz) {
		// TODO: More advanced ubclasses will probably want to transform the point into local/object space before determining the color...
		return color;
	}
}