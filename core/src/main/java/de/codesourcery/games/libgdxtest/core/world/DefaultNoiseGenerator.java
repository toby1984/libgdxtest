package de.codesourcery.games.libgdxtest.core.world;



public class DefaultNoiseGenerator 
{
	public final int heightMapSize;
	private SimplexNoise simplexNoise;
	private long seed;
	
	public DefaultNoiseGenerator(int heightMapSize,long seed) 
	{
		this.heightMapSize = heightMapSize;
		this.seed = seed;
        this.simplexNoise = new SimplexNoise(seed);		
	}

	public float[] createNoise2D(float x,float y,float tileSize,int octaves,float persistance) 
	{
		return simplexNoise.createHeightMap( x ,y , heightMapSize , tileSize , octaves, persistance);
	}

	public void setSeed(long seed) 
	{
        if ( simplexNoise == null || this.seed != seed ) {
            simplexNoise = new SimplexNoise(seed);
        }	    
	    this.seed = seed;
	}
}