package de.codesourcery.games.libgdxtest.core.world;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.badlogic.gdx.math.Vector3;

public class TextureUtils
{
    public static BufferedImage blendTextures(BufferedImage texture1,BufferedImage texture2,float[] heightMap,int heightMapSize,float minValue) {
        
        if ( texture1.getWidth() != texture2.getWidth() || texture1.getHeight() != texture2.getHeight()) {
            throw new IllegalArgumentException("Textures have different sizes");
        }
        
        final BufferedImage result = new BufferedImage( texture1.getWidth() , texture1.getHeight() , BufferedImage.TYPE_INT_ARGB);
        
        float xScale = heightMapSize / (float) texture1.getWidth();
        float yScale = heightMapSize / (float) texture1.getHeight();

        for ( int x = 0 ; x < texture1.getWidth() ; x++ ) 
        {
            for ( int y = 0 ; y < texture1.getHeight() ; y++ ) 
            {
                int color1 = texture1.getRGB( x , y );              
                int hx = (int) (xScale*x);
                int hy = (int) (yScale*y);
                float alpha = heightMap[hx+hy*heightMapSize];
                
                if ( alpha >= minValue  ) 
                {
                    int color2 = texture2.getRGB( x , y );
                    int newColor = addRGB( applyAlpha( color1 , 1.0f - alpha ) , applyAlpha( color2 , alpha ) );
                    result.setRGB( x , y , newColor );
                } else {
                    result.setRGB( x , y , color1 );
                }
            }
        }
        return result;
    }
    
    private static int addRGB(int rgb1,int rgb2) 
    {
        int r = (rgb1 >> 16) & 0xff;
        int g = (rgb1 >> 8) & 0xff;
        int b = (rgb1 ) & 0xff;     
        
        r += (rgb2 >> 16) & 0xff;
        g += (rgb2 >> 8) & 0xff;
        b += (rgb2 ) & 0xff;
        
        return 0xff << 24 | r << 16 | g << 8 | b;
    }
    
    private static int applyAlpha(int argb , float factor) 
    {
        //  (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
        int a = (argb >> 24) & 0xff;
        float r = (argb >> 16) & 0xff;
        r *= factor;
        float g = (argb >> 8) & 0xff;
        g *= factor;
        float b = (argb ) & 0xff;
        b *= factor;
        return a << 24 | ((int) r) << 16 | ((int) g) << 8 | ((int) b);
    }

    public static BufferedImage heightMapToTexture(float[] heightMap, int heightMapSize,int[] colorGradient)
    {
        final BufferedImage img = new BufferedImage(heightMapSize,heightMapSize,BufferedImage.TYPE_INT_ARGB);
        int ptr = 0;
        for ( int z1 = 0 ; z1 < heightMapSize ; z1++ ) 
        {        
            for ( int x1 = 0 ; x1 < heightMapSize ; x1++ ) 
            {
                float height = heightMap[ ptr++ ];
                int index = (int) (height*255.0f);
                img.setRGB( x1 , z1 , colorGradient[ index & 0xff] | (255 << 24));
            }
        }
        return img;
    }
    
    public static BufferedImage createTexture(File file,int width,int height) 
    {
        final BufferedImage texture;
        try {
            texture = ImageIO.read( file );
        } 
        catch (IOException e) {
            throw new RuntimeException("Failed to load texture "+file,e);
        }
        
        final BufferedImage result = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = result.createGraphics();
        int xStep = texture.getWidth();
        int yStep = texture.getHeight();
        for ( int x = 0 ; x < width ; x+=xStep ) 
        {
            for ( int y = 0 ; y < height ; y+=yStep ) {
                g.drawImage( texture , x , y , xStep , yStep , null );
            }
        }
        return result;
    }

    public static int[] createLandscapeGradient() 
    {
        // color gradient
        Vector3 blue1=   new Vector3( 0 , 0f  , 0.6f);
        Vector3 blue2=   new Vector3( 0 , 0f  , 0.3f);        
        Vector3 green =  new Vector3( 0 , 0.8f, 0);
        Vector3 green2 = new Vector3( 0 , 0.5f, 0);        
        Vector3 brown =  new Vector3( 224f/255f ,132f/255f ,27f/255f);
        Vector3 brown2 = new Vector3( 171f/255f ,99f/255f ,70f/255f);
        Vector3 grey =   new Vector3( 0.5f , 0.5f , 0.5f);        
        Vector3 white =   new Vector3( 1 , 1 , 1);

        final int[] colorRange = generateColorGradient( 
                new Vector3[] { blue1,blue2,green,green2,brown,brown2,grey,white } ,
                new int[]     {   48 , 48   ,32   ,32    ,32   ,16    ,32   ,16   }
                );
        return colorRange;
    }
    
    public static int[] createBlackWhiteGradient() 
    {
        int[] result = new int[256];
        for ( int i = 0 ; i < 256 ; i++ ) {
            result[i] = i << 16 | i << 8 | i;
        }
        return result;
    }   

    public static int[] generateColorGradient(Vector3[] colors,int[] interpolatedColorCount) 
    {
        if ( colors.length != interpolatedColorCount.length ) {
            throw new IllegalArgumentException("Colors array needs to have same length as gradient position array");
        }

        final int[][] ranges = new int[colors.length][];

        int totalElements = 0;
        for( int i = 1 ; i < colors.length  ; i++) 
        {
            final int elements;
            if ( (i+1) < colors.length ) {
                elements = interpolatedColorCount[i];
            }  else {
                elements = 256-totalElements;
            }
            ranges[i] = interpolateColor(colors[i-1],colors[i],elements);
            totalElements+=elements;
        }

        // fill-up range with final color of gradient
        if ( totalElements < 256 ) 
        {
            final int delta = 256 - totalElements;
            System.out.println("Delta: "+delta);
            int[] tmp = new int[ delta ];
            ranges[ranges.length-1] = tmp;
            final int lastColor = toRGB( colors[ colors.length - 1 ] );
            for ( int i = 0 ; i < delta ; i++ ) {
                tmp[i]=lastColor;
            }
        }

        int dstPos = 0;
        final int[] colorRange = new int[256];
        for ( int[] range : ranges ) 
        {
            if ( range != null ) {
                System.arraycopy(range,0,colorRange,dstPos, range.length);
                dstPos+=range.length;
            }
        }      
        return colorRange;
    }

    public static final int toRGB(Vector3 v) 
    {
        //  (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
        int r = (((int) (v.x*255)) & 0xff) << 16;
        int g = (((int) (v.y*255)) & 0xff) << 8;
        int b = (((int) (v.z*255)) & 0xff);
        return r|g|b;
    }

    public static int[] interpolateColor(Vector3 start,Vector3 end,int elements) {

        final float incR = (end.x - start.x)/elements;
        final float incG = (end.y - start.y)/elements;
        final float incB = (end.z - start.z)/elements;

        final int[] result = new int[ elements ];
        Vector3 current = new Vector3(start);
        for ( int i = 0 ; i < elements ; i++ ) 
        {
            result[i] = toRGB( current );
            current.x = current.x + incR;
            current.y = current.y + incG;
            current.z = current.z + incB;
        }
        return result;
    }  
}
