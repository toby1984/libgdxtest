package de.codesourcery.games.libgdxtest.core.midpoint;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.games.libgdxtest.core.world.TextureUtils;

/**
 * Diamond-star mid-point displacement.
 *
 * @author tobias.gierke@voipfuture.com
 */
public class MidpointDisplacement 
{
	public final int size;
	public final float[] points;
	
	private final Random rnd;
	private long seed;
	
	public static void main(String[] args) 
	{
		final int SIZE = (1<<9)+1;
		
		long seed = System.currentTimeMillis();
		final MidpointDisplacement midpoint = new MidpointDisplacement(SIZE,seed);
		midpoint.generate(seed);
		
		final int[] colorGradient = TextureUtils.createLandscapeGradient();
		final Color[] gradient = new Color[256];
		for ( int i = 0 ; i < 256;i++) {
			gradient[i]=new Color(colorGradient[i]);
		}
		
		final JPanel panel = new JPanel() {
			
			private final KeyAdapter adapter = new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) 
				{
					midpoint.generate(System.currentTimeMillis());
					repaint();
				}
			};
			
			{
				addKeyListener( adapter );
			}
			
			private int floor(float value) {
				return (int) Math.floor(value);
			}
			
			@Override
			protected void paintComponent(Graphics g) 
			{
				super.paintComponent(g);
				final float scaleX = getWidth() / SIZE;
				final float scaleY = getHeight() / SIZE;
				
				int width = floor(scaleX);
				if ( width < 1 ) {
					width = 1;
				}
				int height = floor(scaleY);
				if ( height < 1 ) {
					height = 1;
				}
				for ( int y = 0 ; y < SIZE ; y++ ) 
				{
					for ( int x = 0 ; x < SIZE ; x++ ) 
					{
						int value = (int) (midpoint.points[x+y*SIZE]*255.0f);
						g.setColor(gradient[value]);
						g.fillRect( floor(x * scaleX) ,floor( y * scaleY), width , height );  
					}
				}
			}
		};
		
		panel.setFocusable(true);
		panel.setPreferredSize( new Dimension(400,200 ) );
		
		final JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add( panel , BorderLayout.CENTER );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		panel.requestFocus();
	}
	
	public MidpointDisplacement(int size,long seed) {
		this.size = size;
		this.points=new float[size*size];
		this.seed = seed;
		this.rnd=new Random(seed);
	}
	
	public void generate(long seed) 
	{
		this.seed = seed;
		this.rnd.setSeed( seed );
		generate(0,0,rnd.nextFloat() , rnd.nextFloat() , rnd.nextFloat() , rnd.nextFloat() );
	}
	public void generate(int xOffset,int yOffset , float topLeft,float topRight,float bottomRight,float bottomLeft) 
	{
		this.rnd.setSeed( seed );
		
		Arrays.fill( points , 0);
		
		points[0] = topLeft;
		points[size-1] = topRight;
		points[(size-1)*size] = bottomLeft;
		points[(size-1)+(size-1)*size] = bottomRight;
		
		subdivide(xOffset,yOffset,0,0,size-1,size-1,1);
		
		for ( int i = (size*size)-1 ; i >= 0 ; i--) {
			float value = points[i];
			if ( value > 1.0f ) {
				points[i]= 1.0f;
			} else if ( value < 0 ) {
				points[i] = 0f;
			}
		}
	}
	
	private int hashCode(int x,int y) 
	{
		int result = 31 + x;
		result = 31 * result + y;
		return result;
	}

	private void subdivide(int xOffset,int yOffset,int x1,int y1,int x2,int y2,int depth) 
	{
		final int dx = x2-x1;
		final int dy = y2-y1;
		
		if ( dx < 2 && dy < 2 ) {
			return;
		}
		
		int xMiddle=(x1+x2)/2;
		int yMiddle=(y1+y2)/2;
		
		float c1 = points[x1+y1*size];
		float c2 = points[x2+y1*size];
		float c3 = points[x2+y2*size];
		float c4 = points[x1+y2*size];
		
		float factor= 1.0f/(1<<depth);
		rnd.setSeed( hashCode( xOffset+xMiddle,yOffset+yMiddle ) );
		float random = factor*(rnd.nextFloat()-0.5f);
		float newCenter = ((c1+c2+c3+c4)/4.0f)+random;		
		
		float h1 = (c1+c2)/2.0f;
		float h2 = (c2+c3)/2.0f;
		float h3 = (c3+c4)/2.0f;
		float h4 = (c1+c4)/2.0f;	
		
		points[xMiddle+yMiddle*size]=newCenter;
		
		points[xMiddle+y1*size] = h1;
		points[x2+yMiddle*size] = h2;
		points[xMiddle+y2*size] = h3;
		points[x1+yMiddle*size] = h4;
		
		subdivide( xOffset,yOffset,x1 , y1 , xMiddle , yMiddle , depth+1 );
		subdivide( xOffset,yOffset,xMiddle , y1 , x2, yMiddle , depth+1 );
		subdivide( xOffset,yOffset,x1 , yMiddle , xMiddle , y2 , depth+1 );
		subdivide( xOffset,yOffset,xMiddle , yMiddle , x2 , y2 , depth+1 );
	}
}