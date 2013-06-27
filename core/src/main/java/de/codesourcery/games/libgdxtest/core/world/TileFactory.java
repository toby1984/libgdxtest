package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;

public class TileFactory
{
	private static final boolean DEBUG_LABEL_TILES = false;

	private static final int backgroundHeightmapSize=512;
	private static final float groundLevel=0.0f;
	private static final int[] colorGradient;

	private final float tileSize = 0.5f;

	private final DefaultNoiseGenerator noiseGenerator1;

	static {
		colorGradient = TextureUtils.createLandscapeGradient();
	}

	public TileFactory(long seed) 
	{
		this.noiseGenerator1 = new DefaultNoiseGenerator( backgroundHeightmapSize , seed );
	}

	public Tile createTile(int x,int y) 
	{
		float realX = x * tileSize;
		float realY = -y * tileSize;

		final float[] noise = createNoise( realX , realY );
		//        System.out.println("createTile(): "+x+" / "+y);
		return new Tile(x,y) 
		{
		    @Override
		    protected Texture maybeCreateBackgroundTexture()
		    {
		        if ( DEBUG_LABEL_TILES ) {
		            return TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel ,"Tile "+x+" / "+y);
		        } 
		        return TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel , null );
		    }
		};
	}

	private float[] createNoise(float x,float y) 
	{
		final int heightMapSize = noiseGenerator1.heightMapSize;
		float[] high = noiseGenerator1.createNoise2D( x ,y ,  tileSize , 8 , 1.23f );
		
		final int middleSize= heightMapSize / 2;
		final float middleScale = middleSize / (float) heightMapSize;
		float[] middle = NavMeshGenerator.resample( high , heightMapSize , middleSize );

        final int lowSize= heightMapSize / 4;
        final float lowScale = lowSize / (float) heightMapSize;
        float[] low = NavMeshGenerator.resample( high , heightMapSize , lowSize );		

		float[] result = new float[heightMapSize*heightMapSize];
		for ( int iy = 0 ; iy < heightMapSize ; iy++) 
		{
			for ( int ix = 0 ; ix < heightMapSize ; ix++) 
			{
                final int lix = (int) (ix*lowScale);
                final int liy = (int) (iy*lowScale);
                
				final int mix = (int) (ix*middleScale);
				final int miy = (int) (iy*middleScale);

				result[ix+iy*heightMapSize] = low[lix+liy*lowSize] * 0.5f + 
						middle[mix+miy*middleSize] * 0.25f + 
						high[ix+iy*heightMapSize]*0.25f;
			}
		}
		return result;
	}
}
