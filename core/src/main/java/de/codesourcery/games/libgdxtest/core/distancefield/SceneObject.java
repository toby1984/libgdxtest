package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;


public abstract class SceneObject implements DistanceFunction {

	public final Matrix4 matrix = new Matrix4().idt();
	public final Vector3 center = new Vector3();
	protected DistanceFunction distanceFunction;
	
	protected SceneObject() {
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

	@Override
	public final float distance(float px, float py, float pz) 
	{
		final float l_mat[] = matrix.val;
		float newX = px * l_mat[Matrix4.M00] + py * l_mat[Matrix4.M01] + pz * l_mat[Matrix4.M02] + l_mat[Matrix4.M03];
		float newY = px * l_mat[Matrix4.M10] + py * l_mat[Matrix4.M11] + pz * l_mat[Matrix4.M12] + l_mat[Matrix4.M13];
		float newZ = px * l_mat[Matrix4.M20] + py * l_mat[Matrix4.M21] + pz * l_mat[Matrix4.M22] + l_mat[Matrix4.M23];
		return distanceFunction.distance(newX,newY,newZ);
	}
	
	protected final DistanceFunction repetition(final float cx,final float cy,final float cz,final DistanceFunction func) {
		return new DistanceFunction() {
			
			@Override
			public float distance(float px, float py, float pz) {
				 px = ( px % cx )+0.5f*cx;
				 // py = ( py % cy );
				 pz = ( pz % cz )+0.5f*cz;
//				px = ( px % cx );
//				py = ( py % cy );
//				pz = ( pz % cz );					
				return func.distance(px,py,pz);
			}
		};
	}
}
