package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;

public class TileFactory
{
	private static final boolean DEBUG_LABEL_TILES = false;
	
    private static final int backgroundHeightmapSize=256;
    private static final float groundLevel=0.5f;
    private static final int[] colorGradient;
    
    private final long seed;
    private final DefaultNoiseGenerator noiseGenerator;
    
    static {
        colorGradient = TextureUtils.createBlackWhiteGradient();
    }
    
    public TileFactory(long seed) {
        this.seed = seed;
        this.noiseGenerator = new DefaultNoiseGenerator( backgroundHeightmapSize , seed ); 
    }
    
    public Tile createTile(int x,int y) 
    {
        final float tileSize = 0.2f;
        float realX = x * tileSize;
        float realY = -y * tileSize;
        
        float[] noise = noiseGenerator.createNoise2D( realX , realY , tileSize , 5 , 1f );
        final Texture texture;
        if ( DEBUG_LABEL_TILES ) {
        	texture = TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel ,"Tile "+x+" / "+y);
        } else {
        	texture = TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel , null );
        }
//        System.out.println("createTile(): "+x+" / "+y);
        return new Tile(x,y,texture);
    }
}
