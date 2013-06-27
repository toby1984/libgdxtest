package de.codesourcery.games.libgdxtest.core.world;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class NavMeshGenerator
{
    public static final class NavMesh {
        
        public final int width;
        public final float[] height;
        
        public NavMesh(int width,float[] data) 
        {
            this.width = width;
            this.height = data;
        }
        
        public float get(int x,int y) {
            return height[x+y*width];
        }
        
        public void set(int x,int y,float h) {
            height[x+y*width] = h;
        }        
    }
    
    public NavMesh generateGrid(float[] heightmap,int heightmapSize,int tileSize) 
    {
        final int meshSize = heightmapSize / tileSize;
        final NavMesh result = new NavMesh( meshSize , resample( heightmap , heightmapSize , meshSize ) );
        return result;
    }
    
    public static void main(String[] args)
    {
        final int initialSize = 2048;
        final int sampledSize = initialSize >> 2;
        float[] in = new float[initialSize*initialSize];
        for ( int i = 0 ; i < initialSize*initialSize ; i++ ) {
            in[i] = 0.5f;
        }
        
        for ( int i = 0 ; i < 10 ; i++ ) 
        {
            long time = -System.currentTimeMillis();
    //        float[] out = fastSample( in , initialSize , sampledSize );
            float[] out = doResample( in , initialSize , sampledSize );
            time += System.currentTimeMillis();
            System.out.println("Sampling from "+initialSize+"x"+initialSize+" -> "+sampledSize+"x"+sampledSize+" took "+time+" ms");
        }
    }
    
    private static float[] doResample(float[] heightmap,int inputSize,int newSize) 
    {
        // convert to 8-bit greyscale image (SEVERE loss of precision ...)
        final BufferedImage srcImage = new BufferedImage(inputSize,inputSize,BufferedImage.TYPE_BYTE_GRAY );
        final byte[] srcPixels = ((DataBufferByte) srcImage.getRaster().getDataBuffer()).getData();
        final int srcLen=inputSize*inputSize;
        for ( int i = 0 ; i < srcLen; i++) {
            srcPixels[i] = (byte) (heightmap[i]*255f);
        }
        
        // render "image" in desired target resolution  
        final BufferedImage dstImage = new BufferedImage(newSize,newSize,BufferedImage.TYPE_BYTE_GRAY);
        
        final Graphics2D graphics = dstImage.createGraphics();
        
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage( srcImage, 0 , 0 , newSize, newSize , null );

        // convert image back to float values in range 0....1
        final byte[] dstPixels = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
        final int dstLen = newSize*newSize;
        final float[] result = new float[ dstLen ];
        for ( int i = 0 ; i < dstLen; i++) 
        {
            int value = dstPixels[i];
            value = value & 0xff;
            result[i] = value/255.0f;
        }
        return result;
    } 
    
    /**
     * Resamples a square NxN height map into size MxM.
     * 
     * @param heightMap input heightmap with width == height and a value range of 0...1
     * @param inputSize input width/height 
     * @param outputSize output width/height
     * @return
     */
    public static float[] resample(float[] heightMap,int inputSize,int outputSize) {
        return doResample(heightMap, inputSize, outputSize);
    }    
}