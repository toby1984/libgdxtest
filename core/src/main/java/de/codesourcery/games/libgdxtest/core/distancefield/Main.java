package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class Main {

	static {
		System.loadLibrary("gdx64");
	}
	public static void main(String[] args) {
		
		final JFrame frame = new JFrame("distancefield");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		frame.getContentPane().setLayout( new GridBagLayout() );
		
		final GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.weightx=1.0;
		cnstrs.weighty=1.0;
		cnstrs.fill = GridBagConstraints.BOTH;
		final MyPanel panel = new MyPanel();
		panel.setMinimumSize( new Dimension(640,480) );
		panel.setPreferredSize( new Dimension(640,480) );
		
		frame.getContentPane().add( panel , cnstrs );
		
		frame.pack();
		frame.setVisible( true );
	}
	
	protected static class MyPanel extends JPanel 
	{
		private final float maxSteps = 50;
		
		private final float epsilon = 0.3f;
		
		private final float xMin = -10;
		private final float xMax = 10;
		
		private final float yMin = -10;
		private final float yMax = 10;
		
		private final float xStep = (xMax - xMin ) / 800;
		private final float yStep = (yMax - yMin ) / 600;
		
		private final Vector3 lightPos = new Vector3(20,50,-50);	
		
		private final PerspectiveCamera cam;
		
		public MyPanel() {
			cam = new PerspectiveCamera(67,640,480);
			cam.position.set( new Vector3(0,0,30) );
			cam.direction.set( 0 , 0 , -1 );
			cam.update();
		}
		
		@Override
		protected void paintComponent(Graphics g) 
		{
			super.paintComponent(g);
			
			int w = getWidth();
			int h = getHeight();
			
			Vector3 r = new Vector3(cam.direction);
			Vector3 s = new Vector3();
			Vector3 t = new Vector3();
			fromZAxis( s , t , r );
			
			for ( float y = yMin ; y <= yMax ; y += yStep ) 
			{
				final Vector3 yComp = new Vector3( s ).scl( y );
				for ( float x = xMin ; x <= xMax ; x += xStep ) 
				{
					Vector3 xComp = new Vector3( t ).scl( x );
					Vector3 pointOnViewPlane= new Vector3( xComp ).add( yComp ).add( cam.position ).add( cam.direction );
					Vector3 rayDir = pointOnViewPlane.sub( cam.position ).nor();
					int step = 1;
					Vector3 p = new Vector3( cam.position );
					for ( ; step < maxSteps ; step++ ) 
					{
						float distance = distance( p );
						if ( distance <= epsilon ) {
							break;
						} 
						p.x = p.x + rayDir.x*distance;
						p.y = p.y + rayDir.y*distance;
						p.z = p.z + rayDir.z*distance;
					}
					
					final Color color;
					if ( step < maxSteps ) 
					{
						Vector3 lightVec = new Vector3(lightPos).sub( p ).nor();
						float dot = normal( p ).dot( lightVec );
						if ( dot > 0) {
							double angle = Math.acos( dot )*(180d/Math.PI);
							float lightFactor =(float) ( (90.0-angle)/90.0);
							color = new Color( 0,0,lightFactor);
						} else {
							color = Color.BLACK;
						}
						// System.out.println( step );
					} else {
						color = Color.BLACK;
					}
					
					// transform point using camera matrix
					System.out.print( p +" => ");
					p.mul( cam.combined );
					System.out.println( p );
					// scale to screen coordinates
					int scrX = Math.round( w*p.x);
					int scrY = Math.round( h*p.y);
					
					g.setColor(color);
					g.drawRect( scrX , scrY , 1 , 1 );
				}
			}
		}
		
		private Vector3 normal(Vector3 p) 
		{
			float d0 = distance(p);
			float dx = 0.01f;
			
			Vector3 p1 = new Vector3(p);
			p1.x+=dx;
			float deltaX = d0-distance( p1 );
			
			Vector3 p2 = new Vector3(p);
			p2.y+=dx;
			float deltaY = d0-distance( p2 );
			
			Vector3 p3 = new Vector3(p);
			p3.z+=dx;
			float deltaZ = d0 - distance( p3 );
			return new Vector3(-deltaX,-deltaY,-deltaZ).nor();
		}
		
		private float distance(Vector3 p) 
		{
			float result = distanceToSphere( p , new Vector3(-2,0,-50) , 10 );
			
			float[] d = { distanceToSphere( p , new Vector3(15,0,-60)   , 10 ),
						  distanceToSphere( p , new Vector3(0,15,-50)   , 10 ),
						  distanceToSphere( p , new Vector3(-15,15,-50) , 10),
						  distanceToSphere( p , new Vector3(-15,0,-50) , 10),
						  distanceToSphere( p , new Vector3(0,-10,-20) , 5)
			};
			for ( float val : d ) {
				if ( val < result ) {
					result = val;
				}
			}
			return result;
		}

		protected float distanceToSphere(Vector3 p,Vector3 sphereCenter,float radius) 
		{
			Vector3 temp = new Vector3( sphereCenter );
			float dist = temp.sub( p ).len();
			return dist - radius;
		}
		
		protected float distanceToCube(Vector3 point,float sx) 
		{
			Vector3 p = new Vector3(0,0,50);
			final float sy = sx;
			final float sz = sx;
			
			p.sub( point );
			return Math.max( Math.max(Math.abs(p.x)-sx, Math.abs(p.y)-sy), Math.abs(p.z)-sz);
		}
		
		protected float union(float d1,float d2) {
			return Math.min(d1,d2);
		}
		
		protected float intersection(float d1,float d2) {
			return Math.max(d1,d2);
		}	
		
		protected float subtraction(float d1,float d2) {
			return Math.max(d1,-d2);
		}		
	}
	
    public static void fromZAxis(Vector3 xAxis,Vector3 yAxis,Vector3 zAxis)
    {
            /*
         - Set the smallest (in absolute value) component of R to zero.
         
         - Exchange the other two components of R, and then negate the first one.
            S = ( 0, -Rz, Ry ), in case Rx is smallest.
            
         - Normalize vector S.
            S = S / |S|
         - Last vector T is a cross product of R and S then.
            T = R x S
            */

            float x = zAxis.x;
            float y = zAxis.y;
            float z = zAxis.z;

            final Vector3 r;
            final float temp;
            if ( x <= y && x <= z ) // ( Rx* , Ry , Rz ) 
            {
                    x = 0;
                    r = new Vector3(x,y,z);
                    temp = y;
                    y = -z;
                    z= temp;
            } else if ( y <= x && y <= z ) { // ( Rx , Ry* , Rz )
                    y = 0;
                    r = new Vector3(x,y,z);
                    temp = x;
                    x = -z;
                    z = temp;
            } else { // ( Rx , Ry , Rz* )
                    z = 0;
                    r = new Vector3(x,y,z);
                    temp=x;
                    x = -y;
                    y=temp;
            }

            yAxis.x = x;
            yAxis.y = y;
            yAxis.z = z;

            yAxis.nor();
            xAxis.set( r.crs( yAxis ) );
            
            // yAxis.scl(-1);
    }
}