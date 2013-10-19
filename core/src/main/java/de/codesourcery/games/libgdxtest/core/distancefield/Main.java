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

public class Main 
{
	protected static final Dimension INITIAL_WINDOW_SIZE = new Dimension(640,480);

	protected static final float MAX_MARCHING_DISTANCE = 100;
	protected static final float EPSILON = 0.1f;	

	protected static final Vector3 TEMP1 = new Vector3();
	protected static final Vector3 TEMP2 = new Vector3();
	
	protected static final boolean PRINT_TIMINGS = false;

	protected static boolean ENABLE_LIGHTING = true;
	protected static boolean ENABLE_SHADOWS = true;
	protected static boolean RENDER_TO_SCREEN = true;
	
	protected static boolean PRECOMPUTE = true;

	protected static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();	
	protected static final int SLICE_COUNT = CPU_COUNT+1;

	protected static final char KEY_PRING_CAMERA = 'p';	
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

		scene.add( Scene.pointLight( new Vector3(-70,70,50) , new Color( 255 , 255 ,255 ) ) );
		scene.add( Scene.pointLight( new Vector3(70,70,-50) , new Color( 210 , 0 ,0 ) ) );		

		final float radius = 1.5f;
		final SceneObject cube = Scene.cube( new Vector3(2.5f,22,-50) , radius );
		cube.setColor( 0x000000ff );
		scene.add( cube );
		// scene.add( Scene.sphere( new Vector3(0,20,-50) , 20 ) );
		final SceneObject torus1 = Scene.torus( new Vector3(0,22,-50) , radius*0.25f , radius*2*2 ).rotate( new Vector3(1,0,0) , 90 );
		torus1.setColor( 0x00ff0000 );
		scene.add( torus1 );
		
		final SceneObject torus2 = Scene.torus( new Vector3(-2.5f,22,-50) , radius , radius*2 ).rotate( new Vector3(1,0,0) , 90 );
		torus2.setColor( 0x0000ff00 );		
		scene.add( torus2 );
		
		scene.add( Scene.plane( new Vector3(0,torus1.center.y - radius ,0) , new Vector3(0,1,0) ) );

		final MyPanel panel = new MyPanel(scene, new Vector3( 5.656235f,29.03066f,-38.483734f ),new Vector3( -0.4204809f,-0.45482975f,-0.78411967f ) );
		panel.setMinimumSize( INITIAL_WINDOW_SIZE );
		panel.setPreferredSize( INITIAL_WINDOW_SIZE );

		frame.getContentPane().add( panel , cnstrs );

		frame.pack();
		frame.setVisible( true );
		panel.requestFocus();

		final Thread animator = new Thread() 
		{
			private float angle1 = 0;
			private float angle2 = 0;
			private float angle3 = 0;			

			@Override
			public void run() 
			{
				final Runnable r = new Runnable() {

					@Override
					public void run() {
						panel.repaint();
					}
				};

				while(true) 
				{
					try 
					{
						scene.lock();

						final Matrix4 m = new Matrix4();
						m.rotate( new Vector3(1,0,0) , 90 );
						angle1 = rotY(torus1,angle1,3,m);

						m.idt();
						angle3 = rotZ(torus2,angle3,-1.5f,m);
						
						m.idt();
						angle2 = rotY(cube,angle2,-3,m);

					} finally {
						scene.sceneChanged();
						scene.unlock();
					}

					try 
					{
						SwingUtilities.invokeAndWait( r );
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			private float rotY(SceneObject obj,float angle,float angleInc,Matrix4 m) {
				m.rotate( new Vector3(0,1,0) , angle );
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
				m.translate( -obj.center.x , -obj.center.y , -obj.center.z ); 
				obj.matrix.set( m );
				return incAngle( angle , angleInc );
			}			
		};
		animator.setName("animation-thread");
		animator.setDaemon(true);
		animator.start();
	}

	protected static class MyPanel extends JPanel 
	{
		private final PerspectiveCamera cam;

		private final ThreadPoolExecutor threadPool;

		private int[] imageData;

		private final Scene scene;

		private final Cursor blankCursor; 
		private boolean mouseLook = false;

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

		public MyPanel(Scene scene,Vector3 eyePosition,Vector3 viewDirection) 
		{
			if (scene == null) {
				throw new IllegalArgumentException("scene must not be NULL");
			}
			this.scene = scene;

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
			
		    float data[] = { 0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f,
		        0.0625f, 0.125f, 0.0625f };
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

			Graphics2D graphics = (Graphics2D) g;
			try 
			{
				scene.lock();
				
				if ( PRECOMPUTE && scene.hasChanged() ) 
				{
					long time2 = -System.currentTimeMillis();
					scene.precompute( threadPool , SLICE_COUNT );
					time2 += System.currentTimeMillis();
					if ( PRINT_TIMINGS ) System.out.println("Precompute: "+time2+" ms");
				}
				
				long time3 = -System.currentTimeMillis();
				final Image image = renderToImage( 512 , 512 );
				time3 += System.currentTimeMillis();
				if ( PRINT_TIMINGS ) System.out.println("Actual calculation: "+time3+" ms");
				if ( RENDER_TO_SCREEN ) 
				{
//					graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//					graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
//					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);					
					graphics.drawImage( image , 0 , 0, getWidth() , getHeight() , null );
				}
			}
			finally {
				scene.unlock();
			}

			time += System.currentTimeMillis();

			float fps;
			float avgFps;
			synchronized(FPS_COUNTER_LOCK) 
			{
				totalFrameTime += time;
				frameCounter++;
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

		private Image renderToImage(final int width,final int height) 
		{
			if ( imageData == null || imageData.length != (width*height) ) {
				imageData = new int[ width*height ];
			} 

			final CountDownLatch latch = new CountDownLatch( SLICE_COUNT );
			int xStep = width / SLICE_COUNT;
			for ( int x = 0 ; x < width ; x+=xStep) 
			{
				final int x1 = x;
				final int x2 =  (x1+xStep) >= width ? width :  x1 + xStep;

				threadPool.execute( new Runnable() {
					@Override
					public void run() 
					{
						try {
							renderImageRegion( x1 , 0 , x2, height , imageData , width , height );
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							latch.countDown();
						}
					}
				});
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
			
			final BufferedImage pixelImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);    
			pixelImage.setRGB(0, 0, width, height, imageData, 0, width);			 

			return pixelImage;
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

			for ( int y = yStart ; y < yEnd ; y++ ) 
			{
				for ( int x = xStart ; x < xEnd ; x++ )
				{
					currentPoint.set(x,y,0);
					unproject(currentPoint,w,h);
					rayDir.set(currentPoint).sub(cam.position);

					final int color = traceViewRay(currentPoint,rayDir,lightVec , normal , hit );
					imageData[ x + y * imageWidth ] = color;					
				}
			}
		}

		private int traceViewRay(Vector3 pointOnRay,Vector3 rayDir,Vector3 lightVec,Vector3 normal,ClosestHit hit) 
		{
			float marched = 0;
			while ( marched < MAX_MARCHING_DISTANCE )
			{
				float distance = scene.distance( pointOnRay.x , pointOnRay.y, pointOnRay.z );
				if ( distance <= EPSILON ) {
					distance = scene.distanceUncached( pointOnRay.x , pointOnRay.y, pointOnRay.z , hit );
					break;
				} 
				marched += distance;

				pointOnRay.x += rayDir.x*distance;
				pointOnRay.y += rayDir.y*distance;
				pointOnRay.z += rayDir.z*distance;
			}

			if ( marched < MAX_MARCHING_DISTANCE ) 
			{

				if ( ! ENABLE_LIGHTING ) 
				{
					return AMBIENT_COLOR;	
				}
				
				final int objColor = hit.closestObject.getColor(pointOnRay.x , pointOnRay.y, pointOnRay.z);
				
				scene.populateNormal( pointOnRay , normal ); // calculate normal

				float totalBrightness = 0;
				for ( int i = 0 ; i < scene.lights.size() ; i++ ) 
				{
					PointLight light = scene.lights.get(i);
					lightVec.set( light.position ).sub( pointOnRay ).nor();
					float dot =normal.dot( lightVec );
					if ( dot > 0 ) 
					{
						if ( ! ENABLE_SHADOWS ) {
							totalBrightness += dot;
						} else if ( ! isOccluded( pointOnRay , light.position ) ) {
							totalBrightness += dot;
						} else {
							totalBrightness = 0.2f;
						}
					}
				}
				return scaleColor( objColor , Math.min(totalBrightness,1.0f));
			}
			return BACKGROUND_COLOR;
		}

		private int addColors(int color1,int color2) {

			int r = (color1>>16 & 0xff) + (color2>>16 & 0xff);
			int g = (color1>>8 & 0xff) + (color2>>8 & 0xff);
			int b = (color1 & 0xff) + (color2 & 0xff);

//			r /= 2;
//			g /= 2;
//			b /= 2;
			
			if (r > 255 ) {
				r = 255;
			}			
			if (g > 255 ) {
				g = 255;
			}		
			if (b > 255 ) {
				b = 255;
			}				
			
			return ((int) r)<< 16 | ((int) g) << 8 | (int) b;
		}
		
		private int multiplyColors(int color1,int color2) {

			int r = (color1>>16 & 0xff) * (color2>>16 & 0xff);
			int g = (color1>>8 & 0xff) * (color2>>8 & 0xff);
			int b = (color1 & 0xff) * (color2 & 0xff);

			if (r > 255 ) {
				r = 255;
			}			
			if (g > 255 ) {
				g = 255;
			}		
			if (b > 255 ) {
				b = 255;
			}				
			return ((int) r)<< 16 | ((int) g) << 8 | (int) b;
		}		

		private int scaleColor(int color,float factor) 
		{
			float r = (color>>16 & 0xff)*factor;
			if (r > 255 ) {
				r = 255;
			}
			float g = (color>>8 & 0xff)*factor;
			if (g > 255 ) {
				g = 255;
			}						
			float b = (color& 0xff)*factor;
			if (b > 255 ) {
				b = 255;
			}						
			return ((int) r)<< 16 | ((int) g) << 8 | (int) b;			
		}

		private boolean isOccluded(Vector3 pointOnSurface,Vector3 lightPos) 
		{
			final Vector3 rayDir = new Vector3(lightPos).sub( pointOnSurface ).nor();

			// adjust starting point slightly towards the light source so we're clear of 
			// the surface we just intersected
			final Vector3 currentPoint = new Vector3(rayDir).scl(2f).add( pointOnSurface );

			float distanceToLight = new Vector3( lightPos ).sub( currentPoint ).len();
			float marched = 0;
			while ( marched < distanceToLight )
			{
				float distance = scene.distance( currentPoint.x , currentPoint.y, currentPoint.z);
				if ( distance <= EPSILON ) {
					return true;
				} 
				marched += distance;

				currentPoint.x += rayDir.x*distance;
				currentPoint.y += rayDir.y*distance;
				currentPoint.z += rayDir.z*distance;
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