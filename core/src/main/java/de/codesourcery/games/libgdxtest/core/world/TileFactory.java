package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;

public class TileFactory
{
	private static final boolean DEBUG_LABEL_TILES = false;
	
    private static final int backgroundHeightmapSize=256;
    private static final float groundLevel=0.3f;
    private static final int[] colorGradient;
    
    private final float tileSize = 0.2f;
    
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
        
        float[] noise = noiseGenerator.createNoise2D( realX , realY , tileSize , 5 , 1f );
//        float[] noise = createNoise( realX , realY );
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
        float[] low = noiseGenerator.createNoise2D( x ,y ,  tileSize , 1 , 32 );
        float[] middle  = noiseGenerator.createNoise2D( x ,y , tileSize , 3 , 32 );
        float[] high = noiseGenerator.createNoise2D( x ,y , tileSize , 6 ,1f );
        
        final int heightMapSize = noiseGenerator.heightMapSize;
        float[] result = new float[heightMapSize*heightMapSize];
        for ( int i = 0 ; i < heightMapSize*heightMapSize ; i++ ) 
        {
        	result[i] = low[i] * 0.5f + middle[i] * 0.2f + high[i]*0.3f;
        }
        return result;    	
    }
}
