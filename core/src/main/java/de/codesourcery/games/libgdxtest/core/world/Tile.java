package de.codesourcery.games.libgdxtest.core.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

public class Tile 
{
	public static final int WIDTH = 1000;
	public static final int HEIGHT  = 1000;
	
	public static final int HALF_TILE_WIDTH = WIDTH/2;
	public static final int HALF_TILE_HEIGHT  = HEIGHT/2;
	
	public final int x;
	public final int y;
	public final Texture background;
	
	public Tile(int x, int y,Texture background) 
	{
		this.x = x;
		this.y = y;
		this.background = background;
//		if ( background != null ) {
//		    this.background.setWrap(TextureWrap.Repeat,TextureWrap.Repeat);
//		}
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
	
	public void dispose() 
	{
	    background.dispose();
	}
}