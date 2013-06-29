package de.codesourcery.games.libgdxtest.core.maze;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Voronoi {

	private static final Dimension DIM = new Dimension(400,200);
	
	public static final int POINT_COUNT = 10;
	
	protected static final class Parabola 
	{
		private final Point focus;
		/*
		 * (x-h)^2 = 4p(y-k)
		 */
		public final Point2D.Float v = new Point2D.Float();
		public float h;
		public float k;
		public float p;
		
		public float a; 
		public float b;
		public float c;		
		
		public Parabola(Point focus) 
		{
			this.focus = focus;
		}
		
		public float a() {
			return a;
		}
		
		public float b() {
			return b;
		}
		
		public float c() {
			return c;
		}
		
		public float[] intersect(Parabola pb) 
		{
			float a = a();
			float b = b();
			float c = c();
			
			float d = pb.a();
			float e = pb.b();
			float f = pb.c();
			
			float disc = -4*a*c+4*a*f+b*b-2*e*b+4*c*d-4*d*f+e*e;
			if ( disc < 0 ) {
				return null;
			}
			
			/*
			 * (-sqrt(-4*a*c+4*a*f+b*b-2*e*b+4*c*d-4*d*f+e*e)-b+e)/(2 (a-d))
			 */
			/*
			 * a-d!=0 and x = (-Math.sqrt(-4*a*c+4*a*f+b*b-2*e*b+4*c*d-4*d*f+e*e)-b+e)/(2*(a-d)) 
			 */
			if ( a == d && (e-b)!= 0) {
				return new float[]{ (c-f)/(e-b) };
			}
			double x1 = (-Math.sqrt(disc)-b+e)/(2*(a-d));
			double x2 = (Math.sqrt(disc)-b+e)/(2*(a-d));
			return new float[]{(float) x1,(float) x2};
		}
		
		public float evaluateAt(float x) {
			return a()*x*x+b()*x+c();
		}
		
		public void calculateCoefficients(float directrixY) 
		{
			v.x = focus.x;
			v.y = (focus.y + directrixY)/2.0f;
			
			// vx = h
			// vy = k
			
			// dy = vy-p => vy - dy = p  
			h = v.x;
			k = v.y;
			p = v.y - directrixY;
			
			a = 1/(4*p);
			b = (-2*h)/(4*p);
			c = ((h*h)/(4*p))+k;
		}

		@Override
		public String toString() {
			return "Parabola [focus=" + focus + ", h=" + h + ", k=" + k+ ", p=" + p + "]";
		}
		
		public void render2(Graphics g,int width,float scaleX,float scaleY) {
			
			float inc = 0.1f;
			
			float a = a();
			float b = b();
			float c = c();
			
			for ( float x = 0 ; x < width ; x+= inc) 
			{
				float y = a*x*x+b*x+c;
				if ( y > 0 ) {
					g.drawLine( floor( x*scaleX ) , floor(y*scaleY) , floor( x*scaleX ) , floor(y*scaleY) );
				}
			} 
		}
		
		public void render1(Graphics g,float scaleX,float scaleY) 
		{
			final float inc = 1f;
			
			float y1=0,y2=0;
			int step = 0;
			do {
			  	/* (x-h)(x-h) = 4p(y-k)
			  	 * 
			  	 * (x-h)(x-h) 
			  	 * ---------- = y-k
			  	 *     4p
			  	 *     
			  	 * (x-h)(x-h) 
			  	 * ---------- + k = y
			  	 *     4p   
			  	 */
				float x1 = v.x+step*inc;
				float x2 = x1 + inc;
				
				y1 = ( ((x1-h)*(x1-h))/(4*p))+k;
				y2 = ( ((x2-h)*(x2-h))/(4*p))+k;
				
				g.drawLine( floor(x1*scaleX) , floor(y1*scaleY) , floor(x2*scaleX) , floor(y2*scaleY) );
				
				x1 = v.x-step*inc;
				x2 = x1 - inc;				
				
				g.drawLine( floor(x1*scaleX) , floor(y1*scaleY) , floor(x2*scaleX) , floor(y2*scaleY) );				
				step++;
			} while ( y1 >= 0 && y2 >= 0);
		}
	}
	
	public static void main(String[] args) {
		new Voronoi().run();
	}
	
	private void run() 
	{
		final Random rnd = new Random(System.currentTimeMillis());
		final List<Point> randomPoints = new ArrayList<>();
		for ( int i = 0 ; i < POINT_COUNT ; i++ ) 
		{
			int x = rnd.nextInt( DIM.width );
			int y = rnd.nextInt( DIM.height );
			randomPoints.add( new Point(x,y ) );
		}
		
		MyPanel panel = new MyPanel(randomPoints);
		panel.setPreferredSize( DIM );
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add( panel,BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		panel.requestFocus();
	}

	protected final class MyPanel extends JPanel {
		
		private int sweepLineY=0;
		
		private final List<Point> sites;
		
		private final Map<Point,Parabola> parabolas = new HashMap<>();
		
		private final List<Point> discoveredSites = new ArrayList<>();
		private final List<Point> pointsOnBeachLine = new ArrayList<>();
		
		private final KeyAdapter keyListener = new KeyAdapter() 
		{
			public void keyTyped(java.awt.event.KeyEvent e) {
				moveSweepLine();
				repaint();
			}
		};
		
		private MyPanel(List<Point> points) 
		{
			this.sites = points;
			addKeyListener( keyListener );
			setFocusable(true);
		}


		@Override
		protected void paintComponent(Graphics g) 
		{
			final int w = getWidth();
			final int h = getHeight();
			
			float scaleX = w / (float) DIM.width;
			float scaleY = h / (float) DIM.height;
			
			// clear background
			g.setColor(Color.WHITE);
			g.fillRect(0,0,w,h);
			
			// draw sweep line
			g.setColor(Color.RED);
			g.drawLine(0, floor(sweepLineY*scaleY),w, floor(sweepLineY*scaleY) );
			
			// draw parabolas
			for ( Parabola pb : parabolas.values() ) 
			{
				pb.calculateCoefficients( sweepLineY );
				g.setColor(Color.GREEN);
				pb.render1( g ,scaleX ,scaleY );
				
//				g.setColor(Color.PINK);
//				pb.render2( g , w , scaleX ,scaleY );
			}
			
			/*
			 * breakpoints are the intersections of
			 * parabolas that form the beach line.
			 * 
			 * A parabola belongs to the beach line if
			 * d( vertex - sweepline ) < 
			 */
			
			// draw intersections
			g.setColor(Color.BLUE);
			List<Parabola> tmpParabolas= new ArrayList<>( this.parabolas.values() );
			for ( int i = 0 ; i < tmpParabolas.size() ; i++ ) 
			{
				Parabola pb1 = tmpParabolas.get(i);
				for ( int j = i+1 ; j < tmpParabolas.size() ; j++ ) 
				{
					Parabola pb2 = tmpParabolas.get(j);
					float[] intersections = pb1.intersect( pb2 );
					if ( intersections != null ) 
					{
						for ( float x : intersections ) 
						{
							float y = pb1.evaluateAt( x );
							g.fillArc( floor(scaleX*x)-2 , floor(scaleY*y) - 2 , 4 , 4 , 0 , 360 );
						} 
					}
				}
			}
			
			// draw points
			g.setColor(Color.GRAY);
			for ( Point p : sites ) 
			{
				g.fillArc( floor(scaleX*p.x - 2) , floor(scaleY*p.y) - 2 , 4 , 4 , 0 , 360 );
			}
			
			// render beach line
			g.setColor(Color.BLACK);
			renderBeachLine(g,scaleX,scaleY);	
		}
		
		protected void renderBeachLine(Graphics g,float scaleX,float scaleY) 
		{
			for ( float x = 0 ; x < DIM.width ; x+=0.1 ) 
			{
				float yMax = Float.MIN_VALUE;
				for ( Parabola pb : parabolas.values() ) {
					float y = pb.evaluateAt( x );
					if ( y > 0 && y > yMax ) 
					{
						yMax = y;
					}
				}
				if ( yMax != Float.MIN_VALUE ) {
					g.drawLine(floor(x*scaleX),floor(yMax*scaleY),floor(x*scaleX),floor(yMax*scaleY));
				}
			}
		}
		
		protected List<Point> calculateBeachLine(Graphics g,float scaleX,float scaleY) 
		{
			final List<Point> contributingSites = new ArrayList<>();
			for ( float x = 0 ; x < DIM.width ; x+=0.1 ) 
			{
				float yMax = Float.MIN_VALUE;
				Parabola p = null;
				for ( Parabola pb : parabolas.values() ) {
					float y = pb.evaluateAt( x );
					if ( y > 0 && y > yMax ) 
					{
						yMax = y;
						p = pb;
					}
				}
				if ( yMax != Float.MIN_VALUE ) {
					contributingSites.add( p.focus );
					g.drawLine(floor(x*scaleX),floor(yMax*scaleY),floor(x*scaleX),floor(yMax*scaleY));
				}
			}
			return contributingSites;
		}
		
		protected void moveSweepLine() 
		{
			for ( Point p : getPointsOnSweepLine() ) 
			{
				boolean added = false;
				for ( int i = 0 ; i < discoveredSites.size() ; i++ ) {
					if ( discoveredSites.get(i).x > p.x ) {
						discoveredSites.add(i,p);
						added = true;
						break;
					}
				}
				if ( ! added ) {
					discoveredSites.add( p );
				}
				parabolas.put( p , new Parabola( p ) );
			}
			sweepLineY++;
		}
		
		private List<Point> getPointsOnSweepLine() {
			List<Point> result = new ArrayList<>();
			for ( Point p : sites ) {
				if ( p.y == sweepLineY ) {
					result.add(p);
				}
			}
			return result;
		}
	}
	
	protected static final int floor(float f) {
		return (int) f;
	}
}
