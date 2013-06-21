package de.codesourcery.games.libgdxtest.core.world;

public abstract class TileManager 
{
	public abstract Tile loadTile(int tileX,int tileY); 
	
	public void unloadTiles(Tile tile1,Tile tile2,Tile tile3) {
	}
	
	public void unloadTiles(Tile tile1,Tile tile2,Tile tile3,Tile tile4 , Tile tile5) {
	}	
	
	public void unloadTile(Tile tile) {
	}		
}
