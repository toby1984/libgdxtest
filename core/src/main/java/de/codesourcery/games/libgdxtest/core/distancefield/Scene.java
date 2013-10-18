package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.badlogic.gdx.math.Vector3;

public final class Scene {

	private final ReentrantLock LOCK = new ReentrantLock();
	
	private final List<SceneObject> objects = new ArrayList<>();
	public final List<PointLight> lights = new ArrayList<>();
	
	private static final float fieldExtend = 10f;
	private static final Vector3 fieldCenter = new Vector3(0,22,-50);
	private static final int fieldElements = 128;

	private final float normalCalcDelta;
	private final boolean precompute;
	private float[] fieldData = null;
	
	private final float xStart;
	private final float yStart;
	private final float zStart;
	private final float stepX;
	private final float stepY;
	private final float stepZ;
	
	private final AtomicBoolean sceneHasChanged = new AtomicBoolean(true);
	
	public Scene(boolean precompute) 
	{
		this.precompute = precompute;

		xStart = fieldCenter.x - (fieldExtend/2.0f);
		yStart = fieldCenter.y - (fieldExtend/2.0f);
		zStart = fieldCenter.z - (fieldExtend/2.0f);
		
		stepX = fieldExtend / fieldElements;
		stepY = fieldExtend / fieldElements;
		stepZ = fieldExtend / fieldElements;
		
		if ( precompute ) {
			normalCalcDelta = stepX;
		} else {
			normalCalcDelta = 0.1f;
		}		
	}
	public static SceneObject sphere(Vector3 center,float radius) {
		return new Sphere(center,radius);
	}
	
	public void sceneChanged() {
		this.sceneHasChanged.set( true );
	}
	
	public boolean hasChanged() 
	{
		return sceneHasChanged.get();
	}
	
	public void precompute(ThreadPoolExecutor pool,int sliceCount) 
	{
		if ( fieldData == null ) {
			fieldData = new float[ fieldElements * fieldElements * fieldElements ];
		}
		final float[] data = fieldData;
		final int sliceSize = fieldElements / sliceCount;
		final CountDownLatch latch = new CountDownLatch(sliceCount);
		
		for ( int x = 0 ; x < fieldElements ; x += sliceSize ) {
			final int x1 = x;
			final int x2;
			if ( (x1 + sliceSize) >= fieldElements ) {
				x2 = fieldElements;
			} else {
				x2 = x1 + sliceSize;
			}
			pool.execute( new Runnable() {
				@Override
				public void run() {
					try {
						calcSlice(data,x1,x2);
					} finally {
						latch.countDown();
					}
				}
			});
		}
		
		try {
			latch.await();
		} catch(Exception e) {
			e.printStackTrace();
		}
		sceneHasChanged.compareAndSet(true,false);
	}
	
	private void calcSlice(final float[] data,int x1 , int x2 ) 
	{
		final float xs = xStart + stepX/2.0f;
		final float ys = yStart + stepY/2.0f;
		final float zs = zStart + stepZ/2.0f;
		
		for ( int x = x1 ; x < x2 ; x++ ) 
		{
			float px = xs + ( x * stepX );
			for ( int y = 0 ; y < fieldElements ; y++ ) 
			{
				float py = ys + ( y * stepY );
				for ( int z = 0 ; z < fieldElements ; z++ ) {
					float pz = zs + ( z * stepZ );
					// blockX+BLOCKS_X*blockY+(BLOCKS_X*BLOCKS_Y)*blockZ
					data[x + y*fieldElements + z *fieldElements*fieldElements ] = distanceUncached(px,py,pz);
				}				
			}	
		}
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
	
//	public float getClosestHit(float px,float py,float pz,ClosestHit hit) 
//	{
//		SceneObject closestObj = objects.get(0);
//		float closest = closestObj.distance( px,py,pz);
//		final int len = objects.size();
//		for ( int i = 1 ; i < len ; i++) 
//		{
//			final SceneObject obj=objects.get(i);
//			final float d = obj.distance( px,py,pz);
//			if ( d < closest ) {
//				closest = d;
//				closestObj = obj;
//			}
//		}
//		hit.distance = closest;
//		hit.object = closestObj;
//		return closest;
//	}
	public float distance(float px,float py,float pz) 
	{
		if ( precompute ) 
		{
			int ix = (int) ( (px - xStart) / stepX );
			int iy = (int) ( (py - yStart) / stepY );
			int iz = (int) ( (pz - zStart) / stepZ );
			if ( ix >= 0 && iy >= 0 && iz >= 0 && ix < fieldElements && iy < fieldElements && iz < fieldElements ) {
				// blockX+BLOCKS_X*blockY+(BLOCKS_X*BLOCKS_Y)*blockZ
				return fieldData[ ix + iy*fieldElements + iz *fieldElements*fieldElements ];
			} 
			return fieldExtend*0.9f;
		}
		float distance = objects.get(0).distance( px,py,pz );
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
	
	public float distanceUncached(float px,float py,float pz) 
	{
		float distance = objects.get(0).distance( px,py,pz );
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
		final float deltaX = distance( p.x+normalCalcDelta , p.y    , p.z )     - distance( p.x-normalCalcDelta , p.y    , p.z );
		final float deltaY = distance( p.x    , p.y+normalCalcDelta , p.z )     - distance( p.x    , p.y-normalCalcDelta , p.z );
		final float deltaZ = distance( p.x    , p.y    , p.z +normalCalcDelta ) - distance( p.x    , p.y    , p.z-normalCalcDelta );
		normalVector.set(deltaX,deltaY,deltaZ).nor();
	}	
}