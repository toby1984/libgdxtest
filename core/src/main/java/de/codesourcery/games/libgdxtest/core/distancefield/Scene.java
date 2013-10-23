package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;

import com.badlogic.gdx.math.Vector3;

import de.codesourcery.games.libgdxtest.core.world.SimplexNoise;

public final class Scene {

	public SceneObject[] objects = new SceneObject[0];
	public PointLight[] lights = new PointLight[0];
	
	private final float normalCalcDelta;
	
	private static final int HEIGHTMAP_ELEMENTS = 32;
	
	private static final float HEIGHTMAP_SCALE = 20f;
	
	private static final float HEIGHTMAP_EXTEND = 60;
	private static final Vector3 HEIGHTMAP_CENTER = new Vector3(0,0,0);
	private static final float CELL_SIZE = HEIGHTMAP_EXTEND / (float) HEIGHTMAP_ELEMENTS;
	
	private final float[] heightMap;
	
	public Scene() 
	{
		normalCalcDelta = 0.1f;
		SimplexNoise noise = new SimplexNoise( 0xdeadbeef );
		heightMap = noise.createHeightMap( 0 , 0 , HEIGHTMAP_ELEMENTS , CELL_SIZE , 6 , 0.5f );
		
		final int len =HEIGHTMAP_ELEMENTS*HEIGHTMAP_ELEMENTS;
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		for ( int i = 0 ; i < len ; i++ ) {
			float h = heightMap[i];
			if ( h > max ) {
				max = h;
			}
			if ( h < min ) {
				min = h;
			}
		}
		
		float scale = 1.0f/(max-min);
		for ( int i = 0 ; i < len ; i++ ) {
			heightMap[i] = HEIGHTMAP_SCALE * ( (heightMap[i]-min)*scale );
		}
		
		max = Float.MIN_VALUE;
		min = Float.MAX_VALUE;
		for ( int i = 0 ; i < len ; i++ ) {
			float h = heightMap[i];
			if ( h > max ) {
				max = h;
			}
			if ( h < min ) {
				min = h;
			}
		}	
		System.out.println("SCALED max: "+max+"/min:"+min);
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
	
	public int getColor(float px,float py,float pz) {
		int x = (int) ((px - HEIGHTMAP_CENTER.x) / CELL_SIZE);
		int z = (int) ((pz - HEIGHTMAP_CENTER.z) / CELL_SIZE);
		
		if ( x > 0 && x < HEIGHTMAP_ELEMENTS && z > 0 && z < HEIGHTMAP_ELEMENTS ) {
			float factor = heightMap[x+z*HEIGHTMAP_ELEMENTS] / HEIGHTMAP_SCALE;
			return (int) (factor*255);
		}
		return 0;
	}
	
	public float distance(float px,float py,float pz) 
	{
		float dx = (px - HEIGHTMAP_CENTER.x);
		float dy = (py - HEIGHTMAP_CENTER.y);
		float dz = pz - HEIGHTMAP_CENTER.z;
		
		float distToCenter = (float) Math.sqrt( dx*dx+dy*dy+dz*dz);
		if ( distToCenter > Math.sqrt( HEIGHTMAP_EXTEND + HEIGHTMAP_EXTEND ) ) {
			return distToCenter - (float) Math.sqrt( HEIGHTMAP_EXTEND + HEIGHTMAP_EXTEND );
		}
		
		int x = (int) ( dx / CELL_SIZE);
		int z = (int) (dz / CELL_SIZE);
		
		if ( x > 0 && x < HEIGHTMAP_ELEMENTS && z > 0 && z < HEIGHTMAP_ELEMENTS ) {
			float height = heightMap[x+z*HEIGHTMAP_ELEMENTS];
			return py - height;
		}
		return 100;
	}
	
	public void populateNormal(Vector3 p,Vector3 normalVector) 
	{
		final float deltaX = distance( p.x+normalCalcDelta , p.y    , p.z )     - distance( p.x-normalCalcDelta , p.y    , p.z );
		final float deltaY = distance( p.x    , p.y+normalCalcDelta , p.z )     - distance( p.x    , p.y-normalCalcDelta , p.z );
		final float deltaZ = distance( p.x    , p.y    , p.z +normalCalcDelta ) - distance( p.x    , p.y    , p.z-normalCalcDelta );
		normalVector.set(deltaX,deltaY,deltaZ).nor();
	}	
}