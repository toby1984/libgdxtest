package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.badlogic.gdx.math.Vector3;

public final class Scene {

	private final ReentrantLock LOCK = new ReentrantLock();
	
	private final List<SceneObject> objects = new ArrayList<>();
	public final List<PointLight> lights = new ArrayList<>();
	
	public static SceneObject sphere(Vector3 center,float radius) {
		return new Sphere(center,radius);
	}

	public void lock() {
		LOCK.lock();
	}
	
	public void unlock() {
		LOCK.unlock();
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
	
	public static SceneObject plane(Vector3 pointOnPlane,Vector3 planeNormal) {
		return new Plane(pointOnPlane,planeNormal);
	}
	
	public static SceneObject cube(Vector3 center,float size) {
		return new Cube(center,size);
	}
	
	public static final class ClosestHit {
		public SceneObject object;
		public float distance;
	}
	
	public void add(SceneObject obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj must not be NULL");
		}
		this.objects.add( obj );
	}
	
	public void add(PointLight obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj must not be NULL");
		}
		this.lights.add(obj);
	}
	
	public float getClosestHit(float px,float py,float pz,ClosestHit hit) 
	{
		SceneObject closestObj = null;
		float closest = 0;
		for ( SceneObject obj : objects) 
		{
			float d = obj.distance( px,py,pz);
			if ( closestObj == null || d < closest ) {
				closest = d;
				closestObj = obj;
			}
		}
		hit.distance = closest;
		hit.object = closestObj;
		return closest;
	}
	
	private float distance(float px,float py,float pz) 
	{
		float distance = objects.get(0).distance( px,py,pz);
		final int len = objects.size();
		for ( int i = 1 ; i < len ; i++) 
		{
			final SceneObject obj=objects.get(i);
			final float d = obj.distance( px,py,pz);
			if ( d < distance ) {
				distance = d;
			}
		}
		return distance;
	}
	
	public void populateNormal(Vector3 p,Vector3 normalVector) 
	{
		final float delta = 0.01f;
		final float deltaX = distance( p.x+delta , p.y    , p.z )     - distance( p.x-delta , p.y    , p.z );
		final float deltaY = distance( p.x    , p.y+delta , p.z )     - distance( p.x    , p.y-delta , p.z );
		final float deltaZ = distance( p.x    , p.y    , p.z +delta ) - distance( p.x    , p.y    , p.z-delta );
		normalVector.set(deltaX,deltaY,deltaZ).nor();
	}	
}