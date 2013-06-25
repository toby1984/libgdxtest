package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;

public class TileFactory
{
	private static final boolean DEBUG_LABEL_TILES = false;

	private static final int backgroundHeightmapSize=512;
	private static final float groundLevel=0.6f;
	private static final int[] colorGradient;

	private final float tileSize = 0.1f;

	private final DefaultNoiseGenerator noiseGenerator;

	static {
		colorGradient = TextureUtils.createLandscapeGradient(); //  TextureUtils.createBlackWhiteGradient();
	}

	public TileFactory(long seed) {
		this.noiseGenerator = new DefaultNoiseGenerator( backgroundHeightmapSize , seed ); 
	}

	public Tile createTile(int x,int y) 
	{
		float realX = x * tileSize;
		float realY = -y * tileSize;

		float[] noise = createNoise( realX , realY );
		final Texture texture;
		if ( DEBUG_LABEL_TILES ) {
			texture = TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel ,"Tile "+x+" / "+y);
		} else {
			texture = TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel , null );
		}
		//        System.out.println("createTile(): "+x+" / "+y);
		return new Tile(x,y,texture);
	}

	private float[] createNoise(float x,float y) 
	{
		final int heightMapSize = noiseGenerator.heightMapSize;
		float[] high = noiseGenerator.createNoise2D( x ,y ,  tileSize , 8 , 1.23f );
		final int middleSize= heightMapSize / 8;
		final float middleScale = (heightMapSize / 8.0f)/heightMapSize;
		float[] middle = NavMeshGenerator.sample( high , heightMapSize , 8 );

		final int lowSize = heightMapSize / 2;
		final float lowScale = (heightMapSize / 2.0f)/heightMapSize;
		float[] low = NavMeshGenerator.sample( high  , heightMapSize , 2 );

		float[] result = new float[heightMapSize*heightMapSize];
		for ( int iy = 0 ; iy < heightMapSize ; iy++) 
		{
			for ( int ix = 0 ; ix < heightMapSize ; ix++) 
			{
				final int lix = (int) (ix*lowScale);
				final int liy = (int) (iy*lowScale);

				final int mix = (int) (ix*middleScale);
				final int miy = (int) (iy*middleScale);

				result[ix+iy*heightMapSize] = low[lix+liy*lowSize] * 0.6f + 
						middle[mix+miy*middleSize] * 0.2f + 
						high[ix+iy*heightMapSize]*0.2f;
			}
		}
		return result;
	}
}
