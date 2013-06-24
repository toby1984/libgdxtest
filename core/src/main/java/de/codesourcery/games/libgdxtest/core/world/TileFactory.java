package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;

public class TileFactory
{
    private static final int backgroundHeightmapSize=512;
    private static final float groundLevel=0.0f;
    private static final int[] colorGradient;
    
    private final long seed;
    private final DefaultNoiseGenerator noiseGenerator;
    
    static {
        colorGradient = TextureUtils.createLandscapeGradient();
    }
    
    public TileFactory(long seed) {
        this.seed = seed;
        this.noiseGenerator = new DefaultNoiseGenerator( backgroundHeightmapSize , seed ); 
    }
    
    public Tile createTile(int x,int y) 
    {
        final float tileSize = 0.07f;
        float realX = x *tileSize;
        float realY = y *tileSize;
        
        float[] noise = noiseGenerator.createNoise2D( realX , realY , 0.07f , 5 , 1f );
        final Texture texture = TextureUtils.heightMapToTexture( noise , backgroundHeightmapSize , colorGradient , groundLevel );
//        System.out.println("createTile(): "+x+" / "+y);
        return new Tile(x,y,texture);
    }
}
