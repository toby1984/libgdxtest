package de.codesourcery.games.libgdxtest.core.distancefield;

public final class Utils {

	public static int addColors(int color1,int color2) {

		int r = (color1>>16 & 0xff) + (color2>>16 & 0xff);
		int g = (color1>>8 & 0xff) + (color2>>8 & 0xff);
		int b = (color1 & 0xff) + (color2 & 0xff);

		r /= 2;
		g /= 2;
		b /= 2;

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
	
	public static  int addColors(int color1,int r2,int g2,int b2) {

		int r = (color1>>16 & 0xff) + r2;
		int g = (color1>>8 & 0xff) + g2;
		int b = (color1 & 0xff) + b2;

		r /= 2;
		g /= 2;
		b /= 2;

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

	public static  int multiplyColors(int color1,int r2,int g2,int b2) {

		int r = (color1>>16 & 0xff) * r2;
		int g = (color1>>8 & 0xff) * g2;
		int b = (color1 & 0xff) * b2;

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
	
	public static  int multiplyColors(int color1,int color2) {

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

	public static  int scaleColor(int color,float factor) 
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
	
	public static float clamp(float a,float min,float max) {
		return a < min ? min : a > max ? max : a; 
	}
	
	public static float lerp(float a, float b, float w)
	{
	  return a + w*(b-a);
	}
	
	public float smoothMin( float a, float b)
	{
		final float k = 1f;
	    float h = clamp( 0.5f+0.5f*(b-a)/k, 0.0f, 1.0f );
	    return lerp( b, a, h ) - k*h*(1.0f-h);
	}		
}
