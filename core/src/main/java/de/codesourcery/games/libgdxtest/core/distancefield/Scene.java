package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.math.Vector3;

public final class Scene {

	public SceneObject[] objects = new SceneObject[0];
	public PointLight[] lights = new PointLight[0];
	
	private final float normalCalcDelta;
	
	public Scene() 
	{
		normalCalcDelta = 0.1f;
	}
	
	public static SceneObject sphere(Vector3 center,float radius) {
		return new Sphere(center,radius);
	}
	
	public static SceneObject torus(Vector3 center,float rInner,float rOuter) {
		return new Torus(center,rInner,rOuter);
	}
	
	public static PointLight pointLight(Vector3 position,Color color) {
		return pointLight(position,color.getRGB());
	}
	
	public static PointLight pointLight(Vector3 position,int color) {
		return new PointLight(position, color);
	}
	
	public static SceneObject checkerPlane(Vector3 pointOnPlane,Vector3 planeNormal) {
		return new Plane(pointOnPlane,planeNormal) 
		{
			public int getColor(float px,float py,float pz) 
			{
				boolean bx = Math.abs(px % 2) < 1;
				boolean bz = Math.abs(pz % 2) < 1;
				if ( bx  && bz || !bx && ! bz) {
					return 0x00ffffff; 
				} 
				return 0;
			}				
		};
	}	
	
	public static SceneObject plane(Vector3 pointOnPlane,Vector3 planeNormal) {
		return new Plane(pointOnPlane,planeNormal);
	}
	
	public static SceneObject cube(Vector3 center,float size) {
		return new Cube(center,size);
	}
	
	public static final class ClosestHit {
		public SceneObject closestObject;
		public float occlusionFactor;
	}
	
	public void add(SceneObject obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj must not be NULL");
		}
		SceneObject[] newArray = new SceneObject[ this.objects.length+1 ];
		System.arraycopy( this.objects , 0 , newArray , 0 , this.objects.length );
		newArray[ newArray.length - 1 ] = obj;
		this.objects = newArray;
	}
	
	public void add(PointLight obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj must not be NULL");
		}
		PointLight[] newArray = new PointLight[ this.lights.length+1 ];
		System.arraycopy( this.lights , 0 , newArray , 0 , this.lights.length );
		newArray[ newArray.length - 1 ] = obj;
		this.lights = newArray;
	}
	
	private float smoothMin( float a, float b)
	{
		final float k = 1f;
	    float h = Utils.clamp( 0.5f+0.5f*(b-a)/k, 0.0f, 1.0f );
	    // return Utils.lerp( b, a, h ) - k*h*(1.0f-h);
	    return Utils.lerp( b, a, h ) - k*h*(1.5f-h);
	}	
	
	public float distance(float px,float py,float pz) 
	{
		float distance = objects[0].distance( px,py,pz );
		final int len = objects.length;
		for ( int i = 1 ; i < len ; i++) 
		{
			final SceneObject obj=objects[i];
			final float d = obj.distance( px,py,pz);
			if ( obj.smoothBlend ) {
				distance = smoothMin( distance , d );
			} else if ( d < distance ) {
				distance = d;
			}
		}
		return distance;
	}	
	
	public float distance(float px,float py,float pz,ClosestHit hit) 
	{
		SceneObject closest = objects[0];
		float distance = closest.distance( px,py,pz );
		final int len = objects.length;
		for ( int i = 1 ; i < len ; i++) 
		{
			final SceneObject obj=objects[i];
			final float d = obj.distance( px,py,pz);
			if ( obj.smoothBlend ) 
			{
				if ( d < distance ) {
					closest = obj;
				}					
				distance = smoothMin( distance , d );
			} else if ( d < distance ) {
				distance = d;
				closest = obj;
			}
		}
		hit.closestObject = closest;
		return distance;
	}	
	
	public void populateNormal(Vector3 p,Vector3 normalVector) 
	{
		final float deltaX = distance( p.x+normalCalcDelta , p.y    , p.z )     - distance( p.x-normalCalcDelta , p.y    , p.z );
		final float deltaY = distance( p.x    , p.y+normalCalcDelta , p.z )     - distance( p.x    , p.y-normalCalcDelta , p.z );
		final float deltaZ = distance( p.x    , p.y    , p.z +normalCalcDelta ) - distance( p.x    , p.y    , p.z-normalCalcDelta );
		normalVector.set(deltaX,deltaY,deltaZ).nor();
	}	
}