package de.codesourcery.games.libgdxtest.core.world;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class DefaultNoiseGenerator implements INoiseGenerator
{

    public static void main(String[] args)
    {
        final int NOISE_WIDTH = 256;
        final int NOISE_HEIGHT = 256;
        
        JPanel panel = new JPanel() {
            
            private int lastWidth=-1;
            private int lastHeight=-1;
            private BufferedImage image;
            private final ColorGradient gradient;
            
            {
                gradient = new ColorGradient();
                float left = 1.0f;  
             
//                left -= gradient.add( new Color(0,0,255) , 1f ).range;
                
                left -= gradient.add( new Color(0,0,255), 0.2f ).range;
                left -= gradient.add( new Color( 0x965A06 ) , 0.2f ).range; // brown
                left -= gradient.add( Color.GREEN, 0.39f ).range;
                left -= gradient.add( Color.WHITE, left ).range;                
                gradient.add( Color.GRAY, left );
                
                if ( left < 0 ) {
                    throw new RuntimeException("Bad color gradient range");
                }
                
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
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage( getImage() ,0,0, getWidth() , getHeight() , null ); 
            }
            
            private BufferedImage getImage() 
            {
                if ( image == null ) 
                {
                    lastWidth = getWidth();
                    lastHeight = getHeight();
                    byte[] noise = new DefaultNoiseGenerator().createNoise2D( NOISE_WIDTH , NOISE_HEIGHT, System.currentTimeMillis() );
                    image = toColorImage( noise , NOISE_WIDTH , NOISE_HEIGHT ,gradient , 64 );
                }
                return image;
            }
        };
        
        panel.setFocusable(true);
        panel.setPreferredSize(new Dimension(NOISE_WIDTH , NOISE_HEIGHT ));
        JFrame frame = new JFrame("test");
        
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( panel , BorderLayout.CENTER );
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible( true );
        panel.requestFocus();
    }
    
    @Override
    public byte[] createNoise2D(int width, int height,long seed)
    {
        return createFractalNoise2D(width, height, seed);
    }
    
    private byte[] createPerlinNoise2D(int width, int height,long seed) 
    {    
        final PerlinNoise noise = new PerlinNoise(seed);
        
        float[] floatNoise = new float[width*height];
        
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        
        for ( int y = 0 ; y < height ; y++ ) 
        {
            for ( int x = 0 ; x < width ; x++ ) 
            {
                float value = noise.noise2( x+10.5f , y +22.3f);
                min = Math.min(min, value);
                max = Math.max(max, value);
                floatNoise[ y * width + x ] = value;
            }
        }
        
        DecimalFormat df = new DecimalFormat("####0.0######");
        System.out.println("min/max = "+df.format(min)+" / "+df.format(max));
        System.out.flush();      
        
        int iMin = Integer.MAX_VALUE;
        int iMax = Integer.MIN_VALUE;
        
        byte[] result = new byte[ width * height ];
        int ptr = 0;
        for ( int y = 0 ; y < height ; y++ ) 
        {
            for (int x = 0 ; x < width ; x++ , ptr++) 
            {
                float v = (floatNoise[y * width + x]-min)*1f;
                int intValue = (int) (255.0f*v);
                if ( intValue > 255 ) {
                    intValue = 255;
                } else if ( intValue < 0 ) {
                    intValue = 0;
                }
                iMin = Math.min( iMin , intValue );
                iMax = Math.max( iMax , intValue );
                result[ptr] = (byte) intValue;
            }
        }
        System.out.println(" imin/imax = "+iMin+"/"+iMax);
        return result;        
    }
    
    private byte[] createFractalNoise2D(int width, int height,long seed) 
    {    
        float[][] baseNoise = FractalNoise.generateWhiteNoise(width,height,seed);
        float[][] floatNoise = FractalNoise.generateFractalNoise( baseNoise , 5 , 0.1f );
        
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        
        for ( float[] array : floatNoise ) 
        {
            for ( float f : array ) {
                min = Math.min(min, f);
                max = Math.max(max, f);
            }
        }
        float scale = 1.0f/(max-min);
        
        DecimalFormat df = new DecimalFormat("####0.0######");
        System.out.println("min/max = "+df.format(min)+" / "+df.format(max));
        System.out.flush();
        
        byte[] result = new byte[ width * height ];
        int ptr = 0;
        for ( int y = 0 ; y < height ; y++ ) 
        {
            for (int x = 0 ; x < width ; x++ , ptr++) 
            {
                float value = (floatNoise[x][y]-min)*scale;
                result[ptr] = (byte) (255.0f*value);
            }
        }
        return result;
    }
    
    protected static BufferedImage toGreyScaleImage(byte[] heightmap, int width,int height)  
    {
        final BufferedImage image = new BufferedImage( width , height , BufferedImage.TYPE_BYTE_GRAY);
        int ptr = 0;
        for ( int y = 0 ; y < height ; y++ ) 
        {
            for (int x = 0 ; x < width ; x++ , ptr++) 
            {
                final int value = heightmap[ptr]+128;
                final int rgb = value << 16 | value << 8 | value;
                image.setRGB( x , y, rgb );
            }
        }
        return image;
    }
    
    protected static BufferedImage toColorImage(byte[] heightmap, int width,int height,ColorGradient gradient,int groundLevel)  
    {
        final int[] colors = gradient.getColorArray();
        
        final BufferedImage image = new BufferedImage( width , height , BufferedImage.TYPE_INT_RGB);
        int ptr = 0;
        for ( int y = 0 ; y < height ; y++ ) 
        {
            for (int x = 0 ; x < width ; x++ , ptr++) 
            {
                final int value = heightmap[ptr]+128;
                if ( value < groundLevel ) {
                    image.setRGB( x , y, colors[ 0 ] );
                } else {
                    image.setRGB( x , y, colors[ value ] );
                }
            }
        }
        
        // TODO: remove this debug code (renders color gradient)
        Graphics graphics = image.getGraphics();
        int boxWidth = 1;
        int boxHeight = 10;
        int x = 1;
        int y = 10;
        for ( int i = 0 ; i < 256 ; i++ ) 
        {
            x += boxWidth;            
            graphics.setColor( new Color( colors[i] ) );
            graphics.fillRect( x , y , boxWidth , boxHeight ); 
        }
        return image;
    }    

    protected static final class GradientEntry {
        
        public final float range;
        public final Color color;
        
        public GradientEntry(float range, Color color) 
        {
            this.range = range;
            this.color = color;
        }
        
        public GradientEntry(float range, int rgb)
        {
        //  (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
            this(range, new Color( (rgb >> 16) & 0xff , (rgb >> 8) & 0xff , rgb & 0xff ) );
        }
    }
    
    protected static final class ColorGradient 
    {
        private final List<GradientEntry> entries = new ArrayList<>();
        
        private int[] colorArray;
        
        public GradientEntry add(Color c,float range) {
            GradientEntry result = new GradientEntry(range, c );
            entries.add( result );
            return result;
        }
        
        public GradientEntry add(int color,float range) {
            GradientEntry result = new GradientEntry(range, color );
            entries.add( result );
            return result;
        }
        
        public int getColor(int byteValue) 
        {
            if ( colorArray == null ) {
                colorArray = createColorArray();
            }
            return colorArray[byteValue];
        }

        public int[] getColorArray()
        {
            if ( colorArray == null ) {
                colorArray = createColorArray();
            }            
            return colorArray;
        }
        
        private int[] createColorArray()
        {
            final int[] result = new int[256];
            if ( entries.size() == 1 ) 
            {
                int r1 = entries.get(0).color.getRed();
                int g1 = entries.get(0).color.getGreen();
                int b1 = entries.get(0).color.getBlue();
                float inc = 1.0f / 255f;
                for ( int i = 0 ; i < 256 ; i++ ) 
                {
                    int r = Math.round( r1* (i*inc) );
                    int g = Math.round( g1* (i*inc) );
                    int b = Math.round( b1* (i*inc) );
                    result[i] = r << 16 | g << 8 | b;
                }
                return result;
            }
            
            int ptr = 0;
            for (int i = 0; i < entries.size(); i++) 
            {
                final GradientEntry e = entries.get(i);
                if ( (i+1) < entries.size() ) 
                {
                    final GradientEntry next = entries.get(i+1);
                    int steps = (int) Math.floor( 256 * e.range );
                    
                    float incR = (next.color.getRed() - e.color.getRed()) / (float) steps;
                    float incG = (next.color.getGreen() - e.color.getGreen()) / (float)steps;
                    float incB = (next.color.getBlue() - e.color.getBlue()) / (float)steps;
                    
                    float r = e.color.getRed();
                    float g = e.color.getGreen();
                    float b = e.color.getBlue();
                    
                    for ( ; steps > 0 && ptr < 256 ; steps-- ) 
                    {
                        //  (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).     
                        final int ri = (int) Math.floor( r );
                        final int gi = (int) Math.floor( g );
                        final int bi = (int) Math.floor( b );
                        result[ptr++] = ri << 16 | gi << 8 | bi;
                        r = r + incR;
                        g = g + incG;
                        b = b + incB;
                    }
                } 
                else 
                {
                    while ( ptr < 256 ) 
                    {
                        result[ptr++] = e.color.getRGB();
                    }
                }
            }
            return result;
        }
    }
}