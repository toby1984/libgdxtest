package de.codesourcery.games.libgdxtest.core.world;

public class Tile 
{
	public static final int TILE_WIDTH = 1000;
	public static final int TILE_HEIGHT  = 1000;
	
	public static final int HALF_TILE_WIDTH = TILE_WIDTH/2;
	public static final int HALF_TILE_HEIGHT  = TILE_HEIGHT/2;
	
	public final int x;
	public final int y;
	
	public Tile(int x, int y) 
	{
		this.x = x;
		this.y = y;
	}
}