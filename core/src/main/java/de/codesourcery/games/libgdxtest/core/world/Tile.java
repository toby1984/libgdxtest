package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;

public class Tile 
{
	public static final int WIDTH = 1000;
	public static final int HEIGHT  = 1000;
	
	public static final int HALF_TILE_WIDTH = WIDTH/2;
	public static final int HALF_TILE_HEIGHT  = HEIGHT/2;
	
	public final int x;
	public final int y;
	
	private Texture backgroundTexture;
	
	public Tile(int x, int y) 
	{
		this.x = x;
		this.y = y;
	}
	
	protected Texture maybeCreateBackgroundTexture() {
	    return null;
	}
	
	public final Texture getBackgroundTexture()
    {
	  if ( backgroundTexture == null ) {
	      backgroundTexture = maybeCreateBackgroundTexture();
	  }
      return backgroundTexture;
    }
	
	@Override
	public String toString()
	{
	    return "Tile "+x+" / "+y;
	}
	
	public void activate() {
	}
	
	public void passivate() {
	}
	
	public final void dispose() 
	{
	    if ( backgroundTexture != null ) 
	    {
	        try {
	            backgroundTexture.dispose();
	        } finally  {
	            backgroundTexture=null;
	        }
	    }
	}
}