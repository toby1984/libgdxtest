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

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.badlogic.gdx.math.Vector3;

public class DefaultNoiseGenerator 
{
	private static final boolean ANTI_ALIAS = false;
	public static final float TILE_SIZE=0.1f;
	
	private final int[] colorRange = createLandscapeGradient() ;// createBlackWhiteGradient();

	private final int heightMapSize;

	private PerlinNoise perlinNoise;
	private long seed;

	public DefaultNoiseGenerator(int heightMapSize) {
		this.heightMapSize = heightMapSize;
	}

	public static void main(String[] args)
	{
		new DefaultNoiseGenerator(128).test();
	}

	protected void test() 
	{
		JPanel panel = new JPanel() {

			private long seed = System.currentTimeMillis();
			
			private int tileX = 0;
			private int tileY = 0;
			
			{
				addKeyListener(new KeyAdapter() {
					@Override
					public void keyTyped(KeyEvent e)
					{
						switch( e.getKeyChar() ) {
							case ' ':
								seed = System.currentTimeMillis();
								repaint();
								break;
							case 'a':
								tileX--;
								repaint();
								break;
							case 'd':
								tileX++;
								repaint();
								break;  
							case 'w':
								tileY--;
								repaint();
								break;
							case 's':
								tileY++;
								repaint();
								break;  								
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
				
				// 2x2 images
				float xOffset= tileX*TILE_SIZE;
				float yOffset= tileY*TILE_SIZE;
				
				float scale = 1f/ ( (xOffset+TILE_SIZE)*(yOffset+TILE_SIZE) );
				BufferedImage image1 = getImage( xOffset , yOffset , scale );
				BufferedImage image2 = getImage( xOffset+TILE_SIZE , yOffset , scale );
				BufferedImage image3 = getImage( xOffset , yOffset+TILE_SIZE , scale );
				BufferedImage image4 = getImage( xOffset+TILE_SIZE , yOffset+TILE_SIZE , scale );
				
				int halfWidth = getWidth() / 2;
				int halfHeight = getHeight() / 2;
				
				g.drawImage( image1 ,0,0, halfWidth , halfHeight , null );
				g.drawImage( image2 ,halfWidth,0, halfWidth , halfHeight , null ); 
				g.drawImage( image3 ,0,halfHeight, halfWidth , halfHeight , null ); 
				g.drawImage( image4 ,halfWidth,halfHeight, halfWidth , halfHeight , null ); 
				
				g.setColor( Color.RED );
				g.drawString( "X = "+xOffset +" / Y = "+yOffset+" / tile size: "+TILE_SIZE , 15,15 );
			}

			private BufferedImage getImage(float x,float y,float scale) 
			{
				final float[] noise = createNoise2D( x, y , seed ,scale );
				return heightMapToTexture(noise, heightMapSize );
			}
		};

		panel.setFocusable(true);
		panel.setPreferredSize(new Dimension(heightMapSize , heightMapSize ));
		JFrame frame = new JFrame("test");

		frame.getContentPane().setLayout( new BorderLayout() );
		frame.getContentPane().add( panel , BorderLayout.CENTER );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible( true );
		panel.requestFocus();    	
	}

	private float[] generatePerlinNoise(float xOffset,float yOffset, long seed,float scale) {

		float[] result = new float[ heightMapSize*heightMapSize ];
		
		float factor = TILE_SIZE/heightMapSize;
		
		int ptr = 0;
		for ( int y = 0 ; y < heightMapSize ; y++ ) 
		{		
			for ( int x = 0 ; x < heightMapSize ; x++ ) 
			{
				float value = scale*perlinNoise.tileableNoise2(xOffset + x*factor , yOffset + y*factor , TILE_SIZE , TILE_SIZE  );
				result[ptr++] = value;
			}
		}
		return result;
	}

	public float[] createNoise2D(float xOffset,float yOffset,long seed,float scale)
	{
		if ( seed != this.seed || perlinNoise == null ) 
		{
			perlinNoise = new PerlinNoise(seed);
			this.seed = seed;
		}
		return generatePerlinNoise(xOffset,yOffset,seed,scale);		
	}

	private BufferedImage heightMapToTexture(float[] heightMap, int heightMapSize)
	{
		final BufferedImage img = new BufferedImage(heightMapSize,heightMapSize,BufferedImage.TYPE_INT_RGB);
		for ( int z1 = 0 ; z1 < heightMapSize ; z1++ ) 
		{        
			for ( int x1 = 0 ; x1 < heightMapSize ; x1++ ) 
			{
				float height = heightMap[ x1 + z1 * heightMapSize ];
				int index = (int) (height*255.0f);
				if ( index < 0 ) {
					index = 0;
				} else if ( index > 255 ) {
					index = 255;
				}
				img.setRGB( x1 , z1 , colorRange[ index ] );
			}
		}
		return img;
	}

	private int[] createLandscapeGradient() 
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
	
	@SuppressWarnings("unused")
	private int[] createBlackWhiteGradient() 
	{
		int[] result = new int[256];
		for ( int i = 0 ; i < 256 ; i++ ) {
			result[i] = i << 16 | i << 8 | i;
		}
		return result;
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