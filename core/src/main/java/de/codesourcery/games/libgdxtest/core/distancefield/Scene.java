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
	
	public final List<SceneObject> objects = new ArrayList<>();
	public final List<PointLight> lights = new ArrayList<>();
	
	private static final float fieldExtend = 20f;
	private static final Vector3 fieldCenter = new Vector3(0,22,-50);
	
	private static final int fieldMask = ~0b1111111;
	private static final int fieldElements = 128;

	private final float normalCalcDelta;
	private final boolean precompute;
	private float[] fieldData = null;
	
	private final float xStart;
	private final float yStart;
	private final float zStart;
	public final float step;
	
	private final float halfStep;
	
	private boolean lerp = true;
	
	private final float[] distanceBetweenCells = new float[3*3*3];
	
	private final AtomicBoolean sceneHasChanged = new AtomicBoolean(true);
	
	public Scene(boolean precompute) 
	{
		this.precompute = precompute;

		xStart = fieldCenter.x - (fieldExtend/2.0f);
		yStart = fieldCenter.y - (fieldExtend/2.0f);
		zStart = fieldCenter.z - (fieldExtend/2.0f);
		
		step = fieldExtend / fieldElements;
		
		halfStep = step / 2.0f;
		
		if ( precompute ) {
			normalCalcDelta = step;
		} else {
			normalCalcDelta = 0.1f;
		}
		
		// calculate lookup table for distances
		// between neighboring cells so we save a sqrt()
		// op when lerp()ing in the distance() function
		float cx = xStart + halfStep;
		float cy = yStart + halfStep;
		float cz = zStart + halfStep;
		for ( int dx = -1 ; dx <= 1 ; dx ++) 
		{
			for ( int dy = -1 ; dy <= 1 ; dy ++) 
			{
				for ( int dz = -1 ; dz <= 1 ; dz ++) 
				{
					float nx = xStart + halfStep + (dx*step);
					float ny = yStart + halfStep + (dy*step);
					float nz = zStart + halfStep + (dz*step);
					float cellDist = (float) Math.sqrt( (nx-cx)*(nx-cx) + (ny-cy)*(ny-cy) + (nz-cz)*(nz-cz) );
					distanceBetweenCells[ (dx+1) + (dy+1)*3 + (dz+1)*3*3 ] = cellDist;
				}
			}
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
	
	public void setLerp(boolean yesNo) {
		this.lerp = yesNo;
	}
	
	public boolean isLerp() {
		return lerp;
	}
	
	private void calcSlice(final float[] data,int x1 , int x2 ) 
	{
		final float xs = xStart + halfStep;
		final float ys = yStart + halfStep;
		final float zs = zStart + halfStep;
		
		for ( int x = x1 ; x < x2 ; x++ ) 
		{
			float px = xs + ( x * step );
			for ( int y = 0 ; y < fieldElements ; y++ ) 
			{
				float py = ys + ( y * step );
				for ( int z = 0 ; z < fieldElements ; z++ ) {
					float pz = zs + ( z * step );
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
	
	public float distance(float px,float py,float pz) 
	{
		if ( precompute ) 
		{
			int ix = (int) ( (px - xStart) / step );
			int iy = (int) ( (py - yStart) / step );
			int iz = (int) ( (pz - zStart) / step );
			if ( (ix & fieldMask) == 0 && (iy & fieldMask) == 0 && (iz & fieldMask) == 0 ) 
			{
				float cx = xStart+halfStep+(ix*step);
				float cy = yStart+halfStep+(iy*step);
				float cz = zStart+halfStep+(iz*step);
				
				int inx = px > cx ? +1 : - 1;
				int iny = py > cy ? +1 : - 1;
				int inz = pz > cz ? +1 : - 1;
				
				float result = fieldData[ ix + iy*fieldElements + iz *fieldElements*fieldElements ];
				if ( lerp && ((ix+inx) & fieldMask) == 0 && ((iy+iny) & fieldMask) == 0 && ((iz+inz) & fieldMask) == 0 ) 
				{
					float dx = px - cx;
					float dy = py - cy;
					float dz = pz - cz;
					
					float cellDist = distanceBetweenCells[ inx+1 + (iny+1)*3 + (inz+1)*3*3];
					float len2 = (float) Math.sqrt(dx*dx+dy*dy+dz*dz);
					float factor = len2/cellDist;
					
					float nv = fieldData[ ix+inx + (iy+iny)*fieldElements + (iz+inz) *fieldElements*fieldElements ];
					result = lerp(result, nv , factor );
				}
				// blockX+BLOCKS_X*blockY+(BLOCKS_X*BLOCKS_Y)*blockZ
				return result;
			} 
		}
		return distanceUncached(px, py, pz);
	}
	
	private float smoothMin( float a, float b)
	{
		final float k = 0.9f;
	    float h = clamp( 0.5f+0.5f*(b-a)/k, 0.0f, 1.0f );
	    return lerp( b, a, h ) - k*h*(1.0f-h);
	}	
	
	private float clamp(float a,float min,float max) {
		if ( a < min ) {
			return min;
		}
		if ( a > max ) {
			return max;
		}
		return a;
	}
	
	private float lerp(float a, float b, float w)
	{
	  return a + w*(b-a);
	}
	
	public float distanceUncached(float px,float py,float pz) 
	{
		float distance = objects.get(0).distance( px,py,pz );
		final int len = objects.size();
		for ( int i = 1 ; i < len ; i++) 
		{
			final SceneObject obj=objects.get(i);
			final float d = obj.distance( px,py,pz);
			if ( obj.smoothBlend ) {
				distance = smoothMin( distance , d );
			} else if ( d < distance ) {
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