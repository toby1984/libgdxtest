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
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.badlogic.gdx.math.Vector3;

public class DefaultNoiseGenerator 
{
	private static final float PERSISTANCE = 0.3f;
	private static final int OCTAVES = 3;
	public static final float TILE_SIZE=2f;
	private static final int MAP_SIZE = 256;
	
	private static final boolean ANTI_ALIAS = false;	
	
	private final int[] blackWhiteGradient = createBlackWhiteGradient();
	private final int[] landscapeGradient = createLandscapeGradient();
	private int[] colorRange = landscapeGradient;

	private final int heightMapSize;

	private SimplexNoise simplexNoise;
	private long seed;
	
	private final ThreadPoolExecutor threadPool;

	public DefaultNoiseGenerator(int heightMapSize) {
		this.heightMapSize = heightMapSize;
		
		final int poolSize = Runtime.getRuntime().availableProcessors()+1;
		BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
		ThreadFactory threadFactory = new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable r) 
			{
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		};
		
		threadPool = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.MINUTES, workQueue, threadFactory, new CallerRunsPolicy() );
	}

	public static void main(String[] args)
	{
		new DefaultNoiseGenerator(MAP_SIZE).test();
	}

	protected void test() 
	{
		JPanel panel = new JPanel() {

			private long seed = 0xdeadbeef; // System.currentTimeMillis();
			
			private boolean doBlending = false;
			
			private int tileX = 0;
			private int tileY = 0;
			
			private BufferedImage grass;
			private BufferedImage stones;			
			
			{
				stones = createTexture( new File("/home/tobi/workspace/libgdxtest/assets/stones.png") , 1024 , 1024 );				
				grass = createTexture( new File("/home/tobi/workspace/libgdxtest/assets/grass.png") , 1024 , 1024 );
				
				addKeyListener(new KeyAdapter() {
					@Override
					public void keyTyped(KeyEvent e)
					{
						switch( e.getKeyChar() ) {
							case 'g':
								if ( colorRange == blackWhiteGradient ) {
									colorRange = landscapeGradient;
								} else {
									colorRange = blackWhiteGradient;
								}
								repaint();
								break;
							case 'b':
								doBlending = !doBlending;
								repaint();
								break;
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
				super.paintComponent(g);
				
				if ( ANTI_ALIAS ) {
					((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
				}
			
				// 2x2 images
				float xOffset= tileX*TILE_SIZE*0.1f;
				float yOffset= tileY*TILE_SIZE*0.1f;
				
				int halfWidth = getWidth() / 2;
				int halfHeight = getHeight() / 2;
				
				BufferedImage[] image1={null};
				BufferedImage[] image2={null};
				BufferedImage[] image3={null};
				BufferedImage[] image4={null};		
				CountDownLatch latch = new CountDownLatch(4);
				if ( doBlending ) {
					
					image1[0] = createBlendedImage( xOffset , yOffset  );
					image2[0] = createBlendedImage( xOffset+TILE_SIZE , yOffset  );
					image3[0] = createBlendedImage( xOffset , yOffset+TILE_SIZE  );
					image4[0] = createBlendedImage( xOffset+TILE_SIZE , yOffset+TILE_SIZE  );
					
				} else {
					getImage( latch , xOffset , yOffset , image1  );
					getImage( latch , xOffset+TILE_SIZE , yOffset , image2 );
					getImage( latch , xOffset , yOffset+TILE_SIZE , image3 );
					getImage( latch , xOffset+TILE_SIZE , yOffset+TILE_SIZE , image4 );
					try {
						latch.await();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				g.drawImage( image1[0] ,0,0, halfWidth , halfHeight , null );
				g.drawImage( image2[0] ,halfWidth,0, halfWidth , halfHeight , null ); 
				g.drawImage( image3[0] ,0,halfHeight, halfWidth , halfHeight , null ); 
				g.drawImage( image4[0] ,halfWidth,halfHeight, halfWidth , halfHeight , null ); 

				g.setColor( Color.RED );
				g.drawString( "X = "+xOffset +" / Y = "+yOffset+" / tile size: "+TILE_SIZE , 15,15 );
			}
			
			private BufferedImage createBlendedImage(float x,float y) 
			{
				final float[] noise = createNoise2D( x, y , seed );
				return blendTextures( stones , grass , noise , 0.0f );
			}			
			
			private void getImage(final CountDownLatch latch,final float x,final float y,final BufferedImage[] image) 
			{
				final Runnable r = new Runnable() {

					@Override
					public void run() 
					{
						try {
						long time = -System.currentTimeMillis();
						float[] noise2d = createNoise2D(x,y,seed);
						time += System.currentTimeMillis();
						System.out.println("Noise generation: "+time+" ms");
						
						time = -System.currentTimeMillis();
						image[0]= heightMapToTexture(noise2d, heightMapSize );
						time += System.currentTimeMillis();
						System.out.println("Image generation: "+time+" ms");
						} finally {
							latch.countDown();
						}
					}
				};
				threadPool.submit( r );
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
	
	private float[] createNoise2D(float x,float y,long seed) 
	{
		if ( simplexNoise == null || this.seed != seed ) {
			simplexNoise = new SimplexNoise(seed);
		}
		return simplexNoise.createHeightMap( x ,y , heightMapSize , TILE_SIZE , OCTAVES , PERSISTANCE );
	}

	private BufferedImage blendTextures(BufferedImage texture1,BufferedImage texture2,float[] heightMap,float minValue) {
		
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

	private BufferedImage heightMapToTexture(float[] heightMap, int heightMapSize)
	{
		final BufferedImage img = new BufferedImage(heightMapSize,heightMapSize,BufferedImage.TYPE_INT_ARGB);
		int ptr = 0;
		for ( int z1 = 0 ; z1 < heightMapSize ; z1++ ) 
		{        
			for ( int x1 = 0 ; x1 < heightMapSize ; x1++ ) 
			{
				float height = heightMap[ ptr++ ];
				int index = (int) (height*255.0f);
				img.setRGB( x1 , z1 , colorRange[ index & 0xff] | (255 << 24));
			}
		}
		return img;
	}
	
	private BufferedImage createTexture(File file,int width,int height) 
	{
		final BufferedImage texture;
		try {
			texture = ImageIO.read( file );
		} 
		catch (IOException e) {
			throw new RuntimeException(e);
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