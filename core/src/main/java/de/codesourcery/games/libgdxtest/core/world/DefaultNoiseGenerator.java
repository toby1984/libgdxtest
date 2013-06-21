package de.codesourcery.games.libgdxtest.core.world;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.badlogic.gdx.math.Vector3;

public class DefaultNoiseGenerator implements INoiseGenerator
{
    private final int[] colorRange = createColorGradient();
    
    public static void main(String[] args)
    {
    	new DefaultNoiseGenerator().test();
    }
    
    protected void test() 
    {
    	final boolean ANTI_ALIAS = false;
    	final int MAP_SIZE = 128;    	
        JPanel panel = new JPanel() {
            
            private BufferedImage image;
            
            {
                addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e)
                    {
                        if ( e.getKeyChar() == ' ' ) 
                        {
                            image = null;
                            repaint();
                        }
                    }
                });
            }
            
            @Override
            protected void paintComponent(Graphics g)
            {
            	if ( ANTI_ALIAS ) {
            		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            	}
                g.drawImage( getImage() ,0,0, getWidth() , getHeight() , null ); 
            }
            
            private BufferedImage getImage() 
            {
                if ( image == null ) 
                {
                	long time = - System.currentTimeMillis();
                    float[] noise = createNoise2D( MAP_SIZE , System.currentTimeMillis() );
                    time += System.currentTimeMillis();
                    System.out.println("Time: "+time+" ms");
                    image = heightMapToTexture(noise, MAP_SIZE );
                }
                return image;
            }
        };
        
        panel.setFocusable(true);
        panel.setPreferredSize(new Dimension(MAP_SIZE , MAP_SIZE ));
        JFrame frame = new JFrame("test");
        
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( panel , BorderLayout.CENTER );
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible( true );
        panel.requestFocus();    	
    }
    
    @Override
    public float[] createNoise2D(int heightMapSize,long seed)
    {
        final float[][] whiteNoise = FractalNoise.generateWhiteNoise( heightMapSize , heightMapSize , seed);
        final float[][] perlinNoise = FractalNoise.generateFractalNoise( whiteNoise, 6 , 0.4f );

        float[] map = new float[ heightMapSize * heightMapSize ];

        int ptr = 0;
        for ( int x = 0 ; x < heightMapSize ;x++ ) 
        {
            for ( int y = 0 ; y < heightMapSize ; y++ ) 
            {
                map[ ptr++ ] = perlinNoise[x][y];
            }
        }
        return map;
    }
    
    private BufferedImage heightMapToTexture(float[] heightMap, int heightMapSize)
    {
        final BufferedImage img = new BufferedImage(heightMapSize,heightMapSize,BufferedImage.TYPE_INT_RGB);
        for ( int z1 = 0 ; z1 < heightMapSize ; z1++ ) 
        {        
            for ( int x1 = 0 ; x1 < heightMapSize ; x1++ ) 
            {
                float height = heightMap[ x1 + z1 * heightMapSize ];
                img.setRGB( x1 , z1 , colorRange[ (int) (height * 255) ] );
            }
        }
        return img;
    }

	private int[] createColorGradient() 
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
    
    private int[] generateColorGradient(Vector3[] colors,int[] interpolatedColorCount) 
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
    
    private final int toRGB(Vector3 v) 
    {
        //  (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
    	int r = (((int) (v.x*255)) & 0xff) << 16;
    	int g = (((int) (v.y*255)) & 0xff) << 8;
    	int b = (((int) (v.z*255)) & 0xff);
    	return r|g|b;
    }
    
    private int[] interpolateColor(Vector3 start,Vector3 end,int elements) {
        
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