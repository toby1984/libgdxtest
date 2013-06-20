package de.codesourcery.games.libgdxtest.core.world;

public abstract class TileManager 
{
	public abstract Tile getTile(int tileX,int tileY); 
	
	public Tile[] getTiles(int chunkX,int chunkY) 
	{
		Tile[] result = new Tile[ ChunkManager.TILES_PER_ROW * ChunkManager.TILES_PER_COLUMN ];
		
		int xStart = chunkX * ChunkManager.TILES_PER_ROW;
		for ( int x = 0 ; x < ChunkManager.TILES_PER_COLUMN ; x++,xStart++ ) 
		{
			int yStart = chunkY * ChunkManager.TILES_PER_COLUMN;
			for ( int y = 0 ; y < ChunkManager.TILES_PER_ROW ; y++ , yStart++ ) 
			{
				result[y * ChunkManager.TILES_PER_ROW + x ] = getTile( xStart , yStart );
			}
		}
			
		return result;
	}	
	
	public void unloadTiles(Tile tile1,Tile tile2,Tile tile3) {
		
	}
	
	public void unloadTiles(Tile tile1,Tile tile2,Tile tile3,Tile tile4 , Tile tile5) {
		
	}	
	
	public void unloadTile(Tile tile) {
		
	}		
}
