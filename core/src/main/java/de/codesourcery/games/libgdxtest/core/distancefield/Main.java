package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.games.libgdxtest.core.distancefield.Scene.ClosestHit;
import de.codesourcery.games.libgdxtest.core.distancefield.Scene.DebugRenderer;

public class Main 
{
	protected static final Dimension CALCULATED_IMAGE_SIZE = new Dimension(512,512);
	protected static final Dimension INITIAL_WINDOW_SIZE = new Dimension(640,480);

	protected static boolean RENDER_DISTANCE_FIELD = false;

	protected static final float MAX_MARCHING_DISTANCE = 150;
	protected static final float EPSILON = 0.1f;	

	protected static final Vector3 TEMP1 = new Vector3();
	protected static final Vector3 TEMP2 = new Vector3();

	protected static final boolean PRINT_TIMINGS = true;
	public static final boolean DEBUG_HIT_RATIO = false;	
	
	public static final boolean BENCHMARK_MODE = true;
	public static final int BENCHMARK_FRAMECOUNT = 300;

	private static final boolean ANIMATE = true;
	
	protected static boolean ENABLE_LIGHTING = true;
	protected static boolean ENABLE_SHADOWS = true;
	protected static boolean RENDER_TO_SCREEN = true;

	protected final static boolean PRECOMPUTE = false;

	protected static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	protected static final int SLICE_COUNT = (int) CPU_COUNT*2;

	protected static final char KEY_PRING_CAMERA = 'p';	
	protected static final char KEY_TOGGLE_SHOW_DISTANCE_FIELD = 'f';		
	protected static final char KEY_TOGGLE_RENDERING = 'r';		
	protected static final char KEY_TOGGLE_SMOOTH_BLEND = 'b';
	protected static final char KEY_TOGGLE_LERP = 'x';			
	protected static final char KEY_TOGGLE_OCCLUSION = 'o';	
	protected static final char KEY_TOGGLE_LIGHTING = 'l';	
	protected static final char KEY_TOGGLE_MOUSELOOK = 27;
	protected static final char KEY_FORWARD = 'w';
	protected static final char KEY_BACKWARD = 's';		
	protected static final char KEY_STRAFE_LEFT = 'a';
	protected static final char KEY_STRAFE_RIGHT = 'd';
	protected static final char KEY_UP = 'q';
	protected static final char KEY_DOWN = 'e';		

	private static final int AMBIENT_COLOR = Color.GRAY.getRGB();

	private static final int BACKGROUND_COLOR = Color.BLACK.getRGB();

	static 
	{
		System.loadLibrary("gdx64");
		System.setProperty("sun.java2d.opengl.fbobject","false");
		System.setProperty("sun.java2d.opengl","True");				
	}

	public static void main(String[] args) {

		final JFrame frame = new JFrame("distancefield");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.getContentPane().setLayout( new GridBagLayout() );

		final GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.weightx=1.0;
		cnstrs.weighty=1.0;
		cnstrs.fill = GridBagConstraints.BOTH;

		final Scene scene= new Scene( PRECOMPUTE );

		final Vector3 eyePosition = new Vector3( -9.672026f,5.9259033f,-31.723171f );
		final Vector3 viewDirection = new Vector3( 0.3916041f,-0.061048917f,0.9181042f );
		
		scene.add( Scene.pointLight( new Vector3(-10,1,-10) , Color.WHITE ) );
		// scene.add( Scene.pointLight( new Vector3(70,70,-50) , new Color( 210 , 0 ,0 ) ) );		

		final float radius = 1.5f;
		final SceneObject cube = Scene.cube( new Vector3(0,0,0) , radius );
		cube.setColor( 0x000000ff );
		scene.add( cube );
		
		final SceneObject torus1 = Scene.torus( new Vector3(0,0,0) , radius*0.25f , radius*2*2.5f ).rotate( new Vector3(1,0,0) , 90 );
		torus1.setColor( 0x00ff0000 );
		scene.add( torus1 );

		final SceneObject torus2 = Scene.torus( new Vector3(-10,0,0) , radius , radius*2 ).rotate( new Vector3(1,0,0) , 90 );
		torus2.setColor( 0x0000ff00 );		
		scene.add( torus2 );
		
//		SceneObject torus3 = Scene.torus( new Vector3(-10,10,0) , radius , radius*2 ).rotate( new Vector3(1,0,0) , 90 );
//		torus3.setColor( 0x00aaff00 );
//		scene.add( torus3 );
//		
//		SceneObject torus4 = Scene.torus( new Vector3(-10,10,10) , radius , radius*2 ).rotate( new Vector3(1,0,0) , 90 );
//		torus4.setColor( 0x0000ffaa );
//		scene.add( torus4 );

		scene.add( Scene.plane( new Vector3(0,-5,0) , new Vector3(0,1,0) ).setColor( 0xffffff ) );
//		scene.add( Scene.plane( new Vector3(50,0,0) , new Vector3(-1,0,0) ).setColor( 0xffffff ) );
//		scene.add( Scene.plane( new Vector3(0,0,50) , new Vector3(0,0,-1) ).setColor( 0xffffff ) );		

		final Animator animator = new Animator(scene,cube,torus1,torus2);

		final MyPanel panel = new MyPanel(scene, eyePosition,viewDirection , animator );
		panel.setMinimumSize( INITIAL_WINDOW_SIZE );
		panel.setPreferredSize( INITIAL_WINDOW_SIZE );

		frame.getContentPane().add( panel , cnstrs );

		frame.pack();
		frame.setVisible( true );
		panel.requestFocus();
		
		Thread t = new Thread() {
			@Override
			public void run() 
			{
				while(true) 
				{
					long time = -System.currentTimeMillis();
					try 
					{
						SwingUtilities.invokeAndWait( new Runnable() 
						{
							public void run() {
								panel.repaint();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					} 
					time += System.currentTimeMillis();
					if ( time < 20 ) {
						try { Thread.sleep( 20-time ); } catch(Exception e) {};
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	protected static final class Animator {

		private final Scene scene;
		private final SceneObject cube;
		private final SceneObject torus1;
		private final SceneObject torus2;

		private float angle1 = 0;
		private float angle2 = 0;
		private float angle3 = 0;		
		
		private final Matrix4 m = new Matrix4();

		public Animator(Scene scene, SceneObject cube, SceneObject torus1,SceneObject torus2) 
		{
			super();
			this.scene = scene;
			this.cube = cube;
			this.torus1 = torus1;
			this.torus2 = torus2;
		}

		public void animate() 
		{
			animate(scene, cube, torus1, torus2);
		}

		private void animate(final Scene scene, final SceneObject cube,final SceneObject torus1, final SceneObject torus2) {
			try 
			{
				m.idt();
				m.rotate( new Vector3(1,0,0) , 90 );
				angle1 = rotYZ(torus1,angle1,3,m);

				m.idt();
				angle3 = rotZ(torus2,angle3,-1.5f,m);

				m.idt();
				angle2 = rotY(cube,angle2,-3,m);

			} finally {
				scene.sceneChanged();
			}
		}

		private float rotY(SceneObject obj,float angle,float angleInc,Matrix4 m) {
			m.rotate( new Vector3(0,1,0) , angle );
			m.translate( -obj.center.x , -obj.center.y , -obj.center.z ); 
			obj.matrix.set( m );
			return incAngle( angle , angleInc );
		}
		
		private float rotYZ(SceneObject obj,float angle,float angleInc,Matrix4 m) {
			m.rotate( new Vector3(0,1,0) , angle );
			m.rotate( new Vector3(0,0,1) , angle );			
			m.translate( -obj.center.x , -obj.center.y , -obj.center.z ); 
			obj.matrix.set( m );
			return incAngle( angle , angleInc );
		}		

		private float incAngle(float angle,float angleInc) {
			angle += angleInc;
			if ( angle >= 360 ) {
				angle -= 360;
			} else if ( angle < 0 ) {
				angle += 360;
			}
			return angle;
		}

		private float rotZ(SceneObject obj,float angle,float angleInc,Matrix4 m) {
			m.rotate( new Vector3(0,0,-1) , angle );
			m.translate( obj.center.x , obj.center.y , obj.center.z ); 
			obj.matrix.set( m );
			return incAngle( angle , angleInc );
		}			
	}		

	protected static class MyPanel extends JPanel 
	{
		private final PerspectiveCamera cam;

		private final ThreadPoolExecutor threadPool;

		private BufferedImage backgroundImage; 
		private int[] imageData;

		private final Scene scene;

		private final Cursor blankCursor; 
		private boolean mouseLook = false;

		private final Animator animator;

		private final Object FPS_COUNTER_LOCK = new Object();

		// @GuardedBy(FPS_COUNTER_LOCK)
		private long frameCounter;

		// @GuardedBy(FPS_COUNTER_LOCK)
		private long totalFrameTime = 0;

		private final KeyAdapter adapter = new KeyAdapter() 
		{
			private final Vector3 tmp = new Vector3();

			public void keyTyped(java.awt.event.KeyEvent e) 
			{
				if ( e.getKeyChar() == KEY_TOGGLE_MOUSELOOK ) 
				{
					if ( mouseLook ) {
						mouseLook = false;
						setCursor( Cursor.getDefaultCursor() );
					}
					return;
				}

				float deltaTime = 1.0f;
				float velocity = 1f;
				switch( e.getKeyChar() ) 
				{
					case KEY_TOGGLE_SHOW_DISTANCE_FIELD:
						RENDER_DISTANCE_FIELD = ! RENDER_DISTANCE_FIELD;
						resetFpsCounter();
						break;
					case KEY_PRING_CAMERA:
						System.out.println( "new Vector3"+Main.toStringLong(cam.position)+","+
								"new Vector3"+Main.toStringLong(cam.direction) );
						break;
					case KEY_TOGGLE_SMOOTH_BLEND:
						for ( SceneObject obj : scene.objects ) {
							obj.setSmoothBlend( ! obj.smoothBlend );
							System.out.println("smooth blend: "+obj.smoothBlend);
						}
						resetFpsCounter();
						break;
					case KEY_TOGGLE_LERP:
						scene.setLerp( ! scene.isLerp() );
						System.out.println("linear interpolation: "+scene.isLerp());
						resetFpsCounter();
						break;
					case KEY_TOGGLE_RENDERING:
						RENDER_TO_SCREEN = ! RENDER_TO_SCREEN;
						System.out.println("render to screen: "+RENDER_TO_SCREEN );
						resetFpsCounter();
						break;
					case KEY_TOGGLE_LIGHTING:
						ENABLE_LIGHTING = ! ENABLE_LIGHTING;
						System.out.println("lighting: "+ENABLE_LIGHTING);
						resetFpsCounter();
						break;
					case KEY_TOGGLE_OCCLUSION:
						if ( ENABLE_LIGHTING ) {
							ENABLE_SHADOWS = ! ENABLE_SHADOWS;
							System.out.println("shadows: "+ENABLE_SHADOWS);
						}
						resetFpsCounter();
						break;
					case KEY_FORWARD:
						tmp.set(cam.direction).scl(deltaTime * velocity);
						cam.position.add(tmp);
						break;
					case KEY_BACKWARD:
						tmp.set(cam.direction).scl(-deltaTime * velocity);
						cam.position.add(tmp);
						break;
					case KEY_STRAFE_LEFT:
						tmp.set(cam.direction).crs(cam.up).nor().scl(-deltaTime * velocity);
						cam.position.add(tmp);
						break;
					case KEY_STRAFE_RIGHT:
						tmp.set(cam.direction).crs(cam.up).nor().scl(deltaTime * velocity);
						cam.position.add(tmp);
						break;
					case KEY_UP:
						tmp.set(cam.up).nor().scl(deltaTime * velocity);
						cam.position.add(tmp);
						break;
					case KEY_DOWN:
						tmp.set(cam.up).nor().scl(-deltaTime * velocity);
						cam.position.add(tmp);
						break;
				}
				cam.update();
				MyPanel.this.repaint();
			}
		};

		private final MouseAdapter mouseAdapter = new MouseAdapter() {

			private final float degreesPerPixel = 0.1f;
			private int lastX = 0;
			private int lastY = 0;

			{
				if ( mouseLook ) {
					updateLastCoordinates();
				}
			}

			private void updateLastCoordinates() {
				final Point point = MouseInfo.getPointerInfo().getLocation();
				lastX = point.x;
				lastY = point.y;		
			}

			public void mousePressed(MouseEvent e) {
				if ( ! mouseLook ) {
					mouseLook = true;
					setCursor( blankCursor );
					updateLastCoordinates();
				}
			}

			public void mouseMoved(MouseEvent e) 
			{
				if ( ! mouseLook ) {
					return;
				}

				final Point point = MouseInfo.getPointerInfo().getLocation();				

				int dx = point.x - lastX;
				int dy = point.y- lastY;

				lastX = point.x;
				lastY = point.y;

				float deltaX = - dx  * degreesPerPixel;
				float deltaY = - dy * degreesPerPixel;
				cam.direction.rotate(cam.up, deltaX);
				TEMP2.set(cam.direction).crs(cam.up).nor();
				cam.direction.rotate(TEMP2, deltaY);
				cam.update();
				MyPanel.this.repaint();
			}
		};

		public MyPanel(Scene scene,Vector3 eyePosition,Vector3 viewDirection,Animator animator) 
		{
			if (scene == null) {
				throw new IllegalArgumentException("scene must not be NULL");
			}
			this.scene = scene;
			this.animator = animator;

			cam = new PerspectiveCamera(60,INITIAL_WINDOW_SIZE.width,INITIAL_WINDOW_SIZE.height);
			cam.far = 1024;
			cam.position.set( eyePosition );
			cam.direction.set( viewDirection );
			cam.update();
			addKeyListener( adapter );
			addMouseListener( mouseAdapter );
			addMouseMotionListener( mouseAdapter );
			setFocusable( true );

			final BufferedImage cursorImage= new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB); 
			blankCursor = Toolkit.getDefaultToolkit().createCustomCursor( cursorImage ,  new Point(0,0), "blank" );

			if ( mouseLook ) {
				setCursor( blankCursor );
			}

			BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(50);
			final ThreadFactory threadFactory = new ThreadFactory() {

				private final AtomicInteger threadCount = new AtomicInteger(0);
				@Override
				public Thread newThread(Runnable r) 
				{
					final Thread t = new Thread(r);
					t.setName("rendering-thread-"+threadCount.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			};
			threadPool = new ThreadPoolExecutor( CPU_COUNT , CPU_COUNT , 60 , TimeUnit.MINUTES, queue , threadFactory , new CallerRunsPolicy() );

			if ( PRECOMPUTE ) 
			{
				System.out.print("\nPrecomputing distance field...");
				scene.precompute( threadPool , CPU_COUNT );
				System.out.println("Done");
			}
		}

		public void resetFpsCounter() 
		{
			synchronized(FPS_COUNTER_LOCK) {
				this.frameCounter = 0;
				this.totalFrameTime = 0;
			}
		}

		public Image blur(Image image)
		{
			final BufferedImage biSrc = new BufferedImage(image.getWidth(null),image.getHeight(null) , BufferedImage.TYPE_INT_RGB); 
			Graphics2D graphics = biSrc.createGraphics();
			graphics.drawImage( image , 0 , 0 , null );

			float data[] = { 0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f, 0.0625f, 0.125f, 0.0625f };
			Kernel kernel = new Kernel(3, 3, data);
			ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP,
					null);
			BufferedImage biDest = new BufferedImage( biSrc.getWidth() , biSrc.getHeight() , biSrc.getType() );
			convolve.filter(biSrc, biDest);
			return biDest;
		}

		@Override
		protected void paintComponent(Graphics g) 
		{
			long time = - System.currentTimeMillis();

			final Graphics2D graphics = (Graphics2D) g;
			
			if ( ANIMATE ) {
				animator.animate();
			}
			
			/*
			 * If enabled , pre-compute distance field. 
			 */
			final boolean printDebugInfo = false || ( PRINT_TIMINGS && (frameCounter%10) == 0 );
			if ( PRECOMPUTE && scene.hasChanged() ) 
			{
				if ( printDebugInfo ) 
				{
					scene.printHitRatio();
				}
				long time2 = -System.currentTimeMillis();
				scene.precompute( threadPool , CPU_COUNT+1 );
				time2 += System.currentTimeMillis();
				if ( printDebugInfo ) 
				{
					System.out.println("Precompute: "+time2+" ms");
				}
			}

			long time3 = -System.currentTimeMillis();

			final int imageWidth = CALCULATED_IMAGE_SIZE.width;
			final int imageHeight = CALCULATED_IMAGE_SIZE.height;
			
			final int[] imageData = renderToImage( imageWidth , imageHeight );
			time3 += System.currentTimeMillis();
			if ( printDebugInfo ) System.out.println("Actual calculation: "+time3+" ms");
			
			if ( RENDER_TO_SCREEN ) 
			{
				long time4 = -System.currentTimeMillis();
				backgroundImage.setRGB(0, 0, imageWidth, imageHeight, imageData, 0, imageWidth);
				time4 += System.currentTimeMillis();
				
				if ( printDebugInfo )  System.out.println("converting pixel array to image: "+time4+" ms");
				
				// graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				// graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
				// graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);	

				if ( PRECOMPUTE && RENDER_DISTANCE_FIELD ) 
				{
					scene.renderPrecomputedField( new DebugRenderer() {

						private final Vector3 min = new Vector3();
						private final Vector3 max = new Vector3();

						private final int w = CALCULATED_IMAGE_SIZE.width;
						private final int h = CALCULATED_IMAGE_SIZE.height;

						private Graphics g = backgroundImage.getGraphics();

						@Override
						public void render(float xStart, float yStart,
								float zStart, float xEnd, float yEnd,
								float zEnd) 
						{
							min.set( xStart , yStart , zStart );
							max.set( xEnd , yEnd , zEnd );

							cam.project( min , 0 , 0 , w , h );
							cam.project( max , 0 , 0 , w , h );

							// gdx assumes viewport origin in the BOTTOM left
							min.y = h - min.y;
							max.y = h - max.y;
							g.drawLine( (int) min.x , (int) min.y , (int) max.x , (int) max.y );
						}

						@Override
						public void setColor(Color color) {
							g.setColor(color);
						}
					} );
				} 
				long time5 = -System.currentTimeMillis();
				graphics.drawImage( backgroundImage , 0 , 0, getWidth() , getHeight() , null );
				time5 += System.currentTimeMillis();
				if ( printDebugInfo )  System.out.println("Rendering to screen: "+time5+" ms");				
			}

			time += System.currentTimeMillis();

			float fps;
			float avgFps;
			synchronized(FPS_COUNTER_LOCK) 
			{
				totalFrameTime += time;
				frameCounter++;
				if ( BENCHMARK_MODE && frameCounter == BENCHMARK_FRAMECOUNT ) 
				{
					System.out.println("Benchmark mode,exiting after "+BENCHMARK_FRAMECOUNT+" frames.");
					System.exit(0);
				}				
				fps = 1000f/time;
				avgFps = 1000f/ ( totalFrameTime / frameCounter );
			}

			if ( ! RENDER_TO_SCREEN ) {
				g.setColor( new Color( BACKGROUND_COLOR ) );
				g.fillRect( 0,0,getWidth(),getHeight());
			}
			final DecimalFormat format = new DecimalFormat("###0.0#");

			g.setColor(Color.BLUE);
			g.drawString( "FPS : "+format.format(avgFps)+" fps ( "+format.format(fps)+" fps, "+time+" ms)" , 10 , 10 );
			g.drawString( "Eye position  : "+Main.toString(cam.position) , 10, 25 );
			g.drawString( "View direction: "+Main.toString(cam.direction), 10, 40 );

			if ( (frameCounter%30) == 0 ) {
				System.out.println( "FPS : "+format.format(avgFps)+" fps ( "+format.format(fps)+" fps, "+time+" ms)");
			}			
		}

		private int[] renderToImage(final int width,final int height) 
		{
			if ( imageData == null || imageData.length != (width*height) ) {
				imageData = new int[ width*height ];
				backgroundImage = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);  
				System.out.println("Allocated image data.");
			} 

			final CountDownLatch latch = new CountDownLatch( SLICE_COUNT*SLICE_COUNT );
			int xStep = width / SLICE_COUNT;
			int yStep = width / SLICE_COUNT;			
			for ( int x = 0 ; x < width ; x+=xStep) 
			{
				final int x1 = x;
				final int x2 =  (x1+xStep) >= width ? width :  x1 + xStep;
				for ( int y = 0 ; y < height ; y+=yStep) 
				{
					final int y1 = y;
					final int y2 =  (y1+yStep) >= height ? height :  y1 + yStep;
					threadPool.execute( new Runnable() {
						@Override
						public void run() 
						{
							try {
								// xStart , yStart , xEnd , yEnd 
								renderImageRegion( x1 , y1 , x2, y2 , imageData , width , height );
							} catch(Exception e) {
								e.printStackTrace();
							} finally {
								latch.countDown();
							}
						}
					});
				}
			}

			try 
			{
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			if ( ! RENDER_TO_SCREEN ) {
				return null;
			}
			return imageData;
		}

		private void renderImageRegion(int xStart,int yStart,int xEnd,int yEnd,int[] imageData,int imageWidth,int imageHeight) 
		{
			final int w = imageWidth;
			final int h = imageHeight;

			final Vector3 currentPoint = new Vector3();
			final Vector3 rayDir = new Vector3();
			final Vector3 lightVec = new Vector3();
			final Vector3 normal = new Vector3();
			final ClosestHit hit = new ClosestHit();
			final TraceResult traceResult = new TraceResult();

			for ( int y = yStart ; y < yEnd ; y+=2 ) 
			{
inner:				
				for ( int x = xStart ; x < xEnd ; x+=2 )
				{
					final boolean xBoundardNotReached = (x+1) < xEnd;
					final boolean yBoundardNotReached = (y+1) < yEnd;	
					
					// trace ray #1
					currentPoint.set(x,y,0);
					unproject(currentPoint,w,h);
					rayDir.set(currentPoint).sub(cam.position).nor();

					int color = traceViewRay(currentPoint,rayDir,lightVec , normal , hit , traceResult );
					final boolean skipBundle = traceResult.rayLengthLimitReached && traceResult.minDistance > EPSILON;
					
					if ( skipBundle ) 
					{
						if ( xBoundardNotReached && yBoundardNotReached ) {
							imageData[ x     +  y * imageWidth ]    = BACKGROUND_COLOR;		
							imageData[ (x+1) +  y * imageWidth ]    = BACKGROUND_COLOR;
							imageData[ x     + (y+1) * imageWidth ] = BACKGROUND_COLOR;
							imageData[ (x+1) + (y+1) * imageWidth ] = BACKGROUND_COLOR;
						} else if ( ! xBoundardNotReached && yBoundardNotReached ) {
							imageData[ x     +  y * imageWidth ]    = BACKGROUND_COLOR;		
							imageData[ x     + (y+1) * imageWidth ] = BACKGROUND_COLOR;
						} else if ( xBoundardNotReached && ! yBoundardNotReached ) {
							imageData[ x     +  y * imageWidth ]    = BACKGROUND_COLOR;		
							imageData[ (x+1) +  y * imageWidth ]    = BACKGROUND_COLOR;
						} else {
							imageData[ x     +  y * imageWidth ]    = BACKGROUND_COLOR;		
						}
						continue inner;
					} 
					imageData[ x + y * imageWidth ] = color;
					
					if ( xBoundardNotReached ) {
						// trace ray #2
						currentPoint.set(x+1,y,0);
						unproject(currentPoint,w,h);
						rayDir.set(currentPoint).sub(cam.position).nor();
						imageData[ (x+1) + y * imageWidth ] = traceViewRay(currentPoint,rayDir,lightVec , normal , hit , traceResult );
					}
					
					// trace #ray #3
					if ( yBoundardNotReached ) 
					{
						currentPoint.set(x,y+1,0);
						unproject(currentPoint,w,h);
						rayDir.set(currentPoint).sub(cam.position).nor();
						imageData[ x + (y+1) * imageWidth ] = traceViewRay(currentPoint,rayDir,lightVec , normal , hit , traceResult );
						
						// trace ray #4
						if ( xBoundardNotReached ) {
							currentPoint.set(x+1,y+1,0);
							unproject(currentPoint,w,h);
							rayDir.set(currentPoint).sub(cam.position).nor();
							imageData[ (x+1) + (y+1) * imageWidth ] = traceViewRay(currentPoint,rayDir,lightVec , normal , hit , traceResult );
						}
					}
				}
			}
		}
		
		protected static final class TraceResult {
			public int pixelColor;
			public boolean rayLengthLimitReached;
			public float minDistance;
		}
		
		private int traceViewRay(Vector3 pointOnRay,Vector3 rayDir,Vector3 lightVec,Vector3 normal,ClosestHit hit,TraceResult result) 
		{
			float marched = 0;
			float minDistance = Float.MAX_VALUE;
			
			do
			{
				float distance;
				if ( PRECOMPUTE ) {
					distance = scene.distance( pointOnRay.x , pointOnRay.y, pointOnRay.z );
				} else {
					distance = scene.distanceUncached( pointOnRay.x , pointOnRay.y, pointOnRay.z , hit );
				}
				
				if ( distance <= EPSILON ) 
				{
					result.rayLengthLimitReached = false;
					
					if ( ! ENABLE_LIGHTING ) 
					{
						return AMBIENT_COLOR;	
					}
					
					// ===== BEGIN: Lighting calculation =====
					if ( PRECOMPUTE ) {
						distance = scene.distanceUncached( pointOnRay.x , pointOnRay.y, pointOnRay.z , hit );
					}

					final SceneObject hitObject = hit.closestObject;
					final int objColor = hit.closestObject.getColor(pointOnRay.x , pointOnRay.y, pointOnRay.z);

					scene.populateNormal( pointOnRay , normal ); // calculate normal

					int numLightSourcesHit = 0;
					int r = 0;
					int g = 0;
					int b = 0;
					
					final PointLight[] lights = scene.lights;
					final int lightCount = lights.length;
					for ( int i = 0 ; i < lightCount ; i++ ) 
					{
						final PointLight light = lights[i];
						float lvx = light.position.x - pointOnRay.x;
						float lvy = light.position.y - pointOnRay.y;
						float lvz = light.position.z - pointOnRay.z;
						float distToLight = (float) Math.sqrt( lvx*lvx + lvy*lvy + lvz*lvz );
						lvx /= distToLight;
						lvy /= distToLight;
						lvz /= distToLight;
						float dot = normal.x*lvx + normal.y*lvy + normal.z * lvz;
						if ( dot > 0 ) 
						{
							if ( ! ENABLE_SHADOWS || ! isOccluded( hitObject , pointOnRay , light.position , hit ) ) 
							{
								final float attenuation = Math.max( 1.0f / (200.0f/distToLight) , 1.0f ); 
								r += ( light.color.x * attenuation * dot );
								g += ( light.color.y * attenuation * dot );
								b += ( light.color.z * attenuation * dot );
								numLightSourcesHit++;
							} 
						}
					}
					
					if ( numLightSourcesHit > 1 ) {
						r /= numLightSourcesHit;
						g /= numLightSourcesHit;
						b /= numLightSourcesHit;
					}
					return Utils.addColors( objColor , r , g ,b );
					// END: Lighting calculation
				} 
				
				if ( distance < minDistance ) {
					minDistance = distance;
				}
				
				marched += distance;

				pointOnRay.x += rayDir.x*distance;
				pointOnRay.y += rayDir.y*distance;
				pointOnRay.z += rayDir.z*distance;
			} while ( marched < MAX_MARCHING_DISTANCE );

			result.rayLengthLimitReached = true;
			result.minDistance = minDistance;
			return BACKGROUND_COLOR;
		}

		private boolean isOccluded(SceneObject hitObject,Vector3 pointOnSurface,Vector3 lightPos,ClosestHit hit) 
		{
			// calculate normal vector from point on surface to light position
			float rayDirX = pointOnSurface.x - lightPos.x;
			float rayDirY = pointOnSurface.y - lightPos.y;
			float rayDirZ = pointOnSurface.z - lightPos.z;
			
			final float distToLightSource = (float) Math.sqrt( rayDirX*rayDirX + rayDirY*rayDirY + rayDirZ*rayDirZ );
			rayDirX /= distToLightSource;
			rayDirY /= distToLightSource;
			rayDirZ /= distToLightSource;

			// I'm marching from the light source to the intersection point and
			// not the other way around because it's hard to know how far
			// exactly to move the starting point away from it's original
			// location so it's clear off the surface that was hit in the first place			
			float currentPointX = lightPos.x;
			float currentPointY = lightPos.y;
			float currentPointZ = lightPos.z;

			float marched = 0;
			while ( marched < distToLightSource )
			{
				float distance = scene.distance( currentPointX , currentPointY, currentPointZ );
				if ( distance <= EPSILON ) 
				{
					scene.distanceUncached( currentPointX , currentPointY, currentPointZ , hit );
					return hit.closestObject != hitObject;
				} 
				marched += distance;

				currentPointX += rayDirX*distance;
				currentPointY += rayDirY*distance;
				currentPointZ += rayDirZ*distance;
			}
			return false;
		}

		private void unproject (Vector3 vec, float viewportWidth, float viewportHeight) {
			float x = vec.x;
			float y = vec.y;
			y = viewportHeight - y - 1;
			vec.x = (2 * x) / viewportWidth - 1;
			vec.y = (2 * y) / viewportHeight - 1;
			vec.z = 2 * vec.z - 1;
			vec.prj(cam.invProjectionView);
		}		
	}

	private static String toString(Vector3 v) {
		return "( "+v.x+" | "+v.y+" | "+v.z+" )";
	}

	private static String toStringLong(Vector3 v) {
		return "( "+v.x+"f,"+v.y+"f,"+v.z+"f )";
	}	
}