package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.math.Vector3;

public final class Scene {

	public SceneObject[] objects = new SceneObject[0];
	public PointLight[] lights = new PointLight[0];
	
	private static final float FIELD_EXTEND = 30f;
	private static final Vector3 FIELD_CENTER = new Vector3(0,0,0);
	
	private static final int FIELD_LVL0_ELEMENTS = 16; 
	private static final int FIELD_LVL1_ELEMENTS = 16; 

	private static final float LVL0_CELL_SIZE = FIELD_EXTEND / FIELD_LVL0_ELEMENTS;
	private static final float LVL1_CELL_SIZE = LVL0_CELL_SIZE / FIELD_LVL1_ELEMENTS;
	
	private static final float HALF_LVL0_CELLSIZE = LVL0_CELL_SIZE / 2.0f;
	
	private static final float SQRT_LVL0_CELLSIZE = HALF_LVL0_CELLSIZE * HALF_LVL0_CELLSIZE;
	
	private static final float SUBDIVIDE_DISTANCE = (float) Math.sqrt( SQRT_LVL0_CELLSIZE + SQRT_LVL0_CELLSIZE+SQRT_LVL0_CELLSIZE);

	private final float normalCalcDelta;
	private final boolean precompute;
	
	public volatile DistanceField distanceField = null;
	
	private boolean lerp = false;
	
	private final AtomicBoolean sceneHasChanged = new AtomicBoolean(true);
	
	public Scene(boolean precompute) 
	{
		this.precompute = precompute;
		normalCalcDelta = precompute ? LVL1_CELL_SIZE : 0.1f;
	}
	
	public final class CellEntry 
	{
		public float value;
		public DistanceField details;
		
		public boolean gotDetails;
		
		public CellEntry(float value) {
			this.value = value;
		}
		
		public CellEntry(float value, DistanceField details) {
			this.value = value;
			this.details = details;
			gotDetails = true;
		}
		
		public void set(float value) {
			this.value = value;
			gotDetails = false;
		}
		
		public void set(float value,DistanceField details) {
			this.value = value;
			this.details = details;
			gotDetails = true;
		}		
		
		public float distance(float cx,float cy,float cz) {
			return gotDetails ? details.secondLevelDistance( cx , cy , cz ) : value;
		}
	}
	
	public final class DistanceField {
		
		private final int elements;
		
		private final CellEntry[] cells;
		
		private final float cellSize;
		private final float halfCellSize;
		
		private final float minX;
		private final float minY;
		private final float minZ;
		
		private final float maxX;
		private final float maxY;
		private final float maxZ;		
		
		public int hits;
		public int misses;
		
		public DistanceField(ThreadPoolExecutor pool,int cpuCount, float centerX, float centerY, float centerZ,float extend, int elements,boolean subdivide) 
		{
			super();
			
			this.elements = elements;
			
			this.cellSize = extend / (float) elements;
			this.halfCellSize = cellSize / 2.0f;
			
			this.minX = centerX - (extend/2.0f);
			this.minY = centerY - (extend/2.0f);
			this.minZ = centerZ - (extend/2.0f);
			
			this.maxX = centerX + (extend/2.0f);
			this.maxY = centerY + (extend/2.0f);
			this.maxZ = centerZ + (extend/2.0f);			
			
			final int len = elements*elements*elements;
			this.cells = new CellEntry[ len ];
			
			for ( int i = 0 ; i < len ; i++ ) 
			{
				cells[ i ] = new CellEntry(0);
			}
			update( pool,cpuCount , subdivide );
		}
		
		public void update(final ThreadPoolExecutor pool,final int cpuCount, final boolean subdivide) 
		{
			if ( subdivide ) 
			{
				final int sliceSize = elements / cpuCount;
				final CountDownLatch latch = new CountDownLatch(cpuCount);
				for ( int i = 0 ; i < elements ; i+= sliceSize ) {
					final int x0 = i;
					final int x1 = (x0+sliceSize) < elements ? x0+sliceSize : elements;
					pool.execute( new Runnable() 
					{
						@Override
						public void run() {
							try {
								update(pool,cpuCount,x0,x1,true);
							} finally {
								latch.countDown();
							}
						}
						
					} );
				}
				
				try {
					latch.await();
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else {
				update(pool,cpuCount,0,elements,false);
			}
		}
		
		private void update(ThreadPoolExecutor pool,int cpuCount,int x0,int x1,boolean subdivide) 
		{
			for ( int x = x0 ; x < x1 ; x++ ) 
			{
				float cx = minX + halfCellSize + x*cellSize;
				for ( int y = 0 ; y < elements ; y++ ) 
				{
					float cy = minY + halfCellSize + y*cellSize;
					for ( int z = 0 ; z < elements ; z++ ) 
					{
						float cz = minZ + halfCellSize + z*cellSize;
						float d = distanceUncached( cx , cy , cz );
						final CellEntry entry = cells[ x + y*elements + z*elements*elements ];
						if ( subdivide && Math.abs( d ) <= SUBDIVIDE_DISTANCE ) 
						{
							if ( entry.details != null ) 
							{
								entry.details.update(pool,cpuCount,false);
								entry.set( d , entry.details );								
							} 
							else 
							{
								DistanceField nextLevel = new DistanceField( pool,cpuCount , cx,cy,cz,cellSize,FIELD_LVL1_ELEMENTS,false);
								entry.set( d , nextLevel );
							}
						} else {
							entry.set( d );
						}
					}
				}
			}			
		}
		
		public float firstLevelDistance(float px,float py,float pz) 
		{
			if ( px < minX || px >= maxX | py < minY || py >= maxY || pz < minZ || pz >= maxZ ) {
				if ( Main.DEBUG_HIT_RATIO )  misses++;
				return distanceUncached(px,py,pz);				
			}
			
			int ix = (int) ( (px - minX) / cellSize );
			int iy = (int) ( (py - minY) / cellSize );
			int iz = (int) ( (pz - minZ) / cellSize );
			
			if ( Main.DEBUG_HIT_RATIO )  hits++;
			
			try {
				CellEntry cell = cells[ix+iy*elements+iz*elements*elements];
				if ( cell.gotDetails ) {
					 return cell.details.secondLevelDistance( px,py,pz);
				}
				return cell.value;
			} 
			catch(ArrayIndexOutOfBoundsException e) 
			{
				// on rare occasions, the sloppy floating point comparisons above 
				// (should've used Math.abs(x - limit ) < EPSILON ) cause an AIOOBE because of rounding issues ,I'll rather
				// catch those here and go on instead of always taking the performance hit
				System.out.println("Oops...should've used Math.abs() ...");
				return distanceUncached( px,py,pz );
			}
		}
		
		public float secondLevelDistance(float px,float py,float pz) 
		{
			int ix = (int) ( ( px - minX ) / cellSize );
			int iy = (int) ( ( py - minY ) / cellSize );
			int iz = (int) ( ( pz - minZ ) / cellSize );
			
			try {
				return cells[ix+iy*elements+iz*elements*elements].value;
			} 
			catch(ArrayIndexOutOfBoundsException e) 
			{
				// on rare occasions, rounding issues cause an AIOOBE here...
				System.out.println("Something went wrong...");
				System.err.println("px: "+px+" | py: "+py+" | pz: "+pz);
				System.err.println("ix: "+ix+" | iy: "+iy+" | iz: "+iz);
				System.err.println("Field: "+this);
				return distanceUncached( px,py,pz );
			}
		}		
		
		@Override
		public String toString() 
		{
			float xMax = minX+(elements*cellSize);
			float yMax = minY+(elements*cellSize);
			float zMax = minZ+(elements*cellSize);
			String result = "DistanceField[ minX: "+minX+" | minY: "+minY+" | minZ: "+minZ+" | " +
					         " maxX: "+xMax+" | maxY: "+yMax+" | maxZ: "+zMax+" | "+
 					        "cellSize: "+cellSize+" | elements: "+elements+" ]";
			return result;
		}
		
		public void render(DebugRenderer renderer) {

			Color redAlpha = new Color( 255 , 0 , 0 , 128 );
			Color blueAlpha = new Color( 0 , 0 , 255 , 128 );
			
			// renderThisLevelOnly( renderer , redAlpha );
			
			renderer.setColor( blueAlpha );
			
			for ( int i = 0 ; i < cells.length ; i++ ) {
				CellEntry cell = cells[i];
				if ( cell.gotDetails ) {
					cell.details.renderOutline( renderer );
				}
			}
		}
		
		protected void renderOutline(DebugRenderer renderer) {

			float maxX = minX+(elements*cellSize);
			float maxY = minY+(elements*cellSize);
			float maxZ = minZ+(elements*cellSize);
			
			final int last = elements-1;
			for ( int x = 0 ; x < elements ; x++ ) 
			{
				if ( x != 0 && x != last ) {
					continue;
				}
				float xPos = minX + x*cellSize;				
				for ( int y = 0 ; y < elements ; y++ ) 
				{
					if ( y != 0 && y != last ) {
						continue;
					}
					float yPos = minY + y*cellSize;
					for ( int z = 0 ; z < elements ; z++ ) 
					{
						if ( z != 0 && z != last ) {
							continue;
						}						
						float zPos = minZ + z*cellSize;
						renderer.render( xPos , yPos , minZ , xPos, yPos , maxZ );
						renderer.render( minX, yPos , zPos, maxX , yPos , zPos );
						renderer.render( xPos , minY , zPos, xPos , maxY , zPos );
					}					
				}					
			}			
		}
		
		protected void renderThisLevelOnly(DebugRenderer renderer, Color color) 
		{
			float maxX = minX+(elements*cellSize);
			float maxY = minY+(elements*cellSize);
			float maxZ = minZ+(elements*cellSize);
			
			renderer.setColor( color );
			for ( int x = 0 ; x < elements ; x++ ) 
			{
				float xPos = minX + x*cellSize;				
				for ( int y = 0 ; y < elements ; y++ ) 
				{
					float yPos = minY + y*cellSize;
					for ( int z = 0 ; z < elements ; z++ ) 
					{
						float zPos = minZ + z*cellSize;
						renderer.render( xPos , yPos , minZ , xPos, yPos , maxZ );
						renderer.render( minX, yPos , zPos, maxX , yPos , zPos );
						renderer.render( xPos , minY , zPos, xPos , maxY , zPos );
					}					
				}					
			}
		}
	}
	
	public interface DebugRenderer {
		
		public void setColor(Color color);
		
		public void render(float xStart,float yStart,float zStart,float xEnd,float yEnd,float zEnd);
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
	
	public void precompute(ThreadPoolExecutor pool,int cpuCount) 
	{
		if ( !sceneHasChanged.get() ) 
		{
			return;
		}
		
		if ( distanceField == null ) 
		{
			distanceField = new DistanceField( pool,cpuCount,FIELD_CENTER.x , FIELD_CENTER.y , FIELD_CENTER.z , FIELD_EXTEND, FIELD_LVL0_ELEMENTS , true );
			sceneHasChanged.compareAndSet(true,false);
			return;
		} 
		
		if ( Main.DEBUG_HIT_RATIO ) {
			distanceField.hits = 0;
			distanceField.misses = 0;
		}			
		distanceField.update( pool,cpuCount, true );
		sceneHasChanged.compareAndSet(true,false);
	}
	
	public void renderPrecomputedField(DebugRenderer renderer) {
		distanceField.render( renderer );
	}
	
	public void printHitRatio() 
	{
		if ( distanceField != null ) {
			float hits = distanceField.hits;
			float misses = distanceField.misses;
			float hitRatio = 100*( hits / (hits+misses) );
			System.out.println("Hits: "+hits+" / misses: "+misses+" / hit ratio: "+hitRatio);
		}
	}
	
	public void setLerp(boolean yesNo) {
		this.lerp = yesNo;
	}
	
	public boolean isLerp() {
		return lerp;
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
		public float distance;
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
	
	public float distance(float px,float py,float pz) 
	{
		if ( precompute ) 
		{
			return distanceField.firstLevelDistance(px,py,pz);
		}
		return distanceUncached(px, py, pz);
	}
	
	private float smoothMin( float a, float b)
	{
		final float k = 1f;
	    float h = Utils.clamp( 0.5f+0.5f*(b-a)/k, 0.0f, 1.0f );
	    // return lerp( b, a, h ) - k*h*(1.0f-h);
	    return Utils.lerp( b, a, h ) - k*h*(1.5f-h);
	}	
	
	public float distanceUncached(float px,float py,float pz) 
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
	
	public float distanceUncached(float px,float py,float pz,ClosestHit hit) 
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
		hit.distance = distance;
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
	
	public static void main(String[] args) {
		
		/*
		 * xWidth = 1
		 * yWidth = 2
		 * zWidth = 3
		 * 
		 * index = x + y*3 + 3*4*z
		 * 
		 * x=0 , y = 0 , z = 0 => 0
		 * x=0 , y = 1 , z = 0 => 3
		 * x=1 , y = 1 , z = 0 => 4
		 * x=0 , y = 0 , z = 1 => 12
		 * x=0 , y = 1 , z = 1 => 15
		 * 
		 */
		
		int maxX = 2;
		int maxY = 3;
		int maxZ = 4;
		final Map<Integer,String> map = new TreeMap<>();
		for ( int x = 0 ; x < maxX ; x++ ) {
			for ( int y = 0 ; y < maxY ; y++ ) {
				for ( int z = 0 ; z < maxZ ; z++ ) 
				{
					int index = x + y*maxX + maxX*maxY*z;
					String s = "("+x+","+y+","+z+") => "+index;
					map.put( index , s );
				}	
			}
		}
		for ( Entry<Integer, String> entry : map.entrySet() ) {
			System.out.println( entry.getKey() +" => "+entry.getValue() );
		}
	}	
}