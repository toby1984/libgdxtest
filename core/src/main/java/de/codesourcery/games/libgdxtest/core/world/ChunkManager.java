package de.codesourcery.games.libgdxtest.core.world;

import org.apache.commons.lang.StringUtils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;


public class ChunkManager 
{
	// the current chunk (3x3 tiles)
	private Tile[] currentChunk;
	
	private TileManager tileManager;
	
    public final OrthographicCamera camera = new OrthographicCamera();
    
    // the tile the camera currently is in
    // tiles are layed out in a cartesian coordinate system
    // with the y-axis pointing up and the x-axis pointing right
    
    // world coordinates (0,0) are right in the center of tile (0,0)
    private int cameraTileX = 0;
    private int cameraTileY = 0;
    
	public ChunkManager() 
	{
	}
	
	public Tile[] getCurrentChunk() 
	{
		if ( currentChunk == null ) 
		{
			currentChunk = new Tile[3*3];
			
			int x = cameraTileX-1; 
			for ( int col = 0 ; col < 3 ; col++ , x++ ) 
			{ 
				int y = cameraTileY+1;
				for ( int row = 0 ; row < 3 ; row++ , y-- ) 
				{
					final Tile tile = tileManager.loadTile( x , y );
					currentChunk[ row * 3 + col ] = tile;
				}
			}
		}
		return currentChunk;
	}
	
	protected void printChunk() 
	{
		getCurrentChunk();
		printChunk(currentChunk);
	}
	
	protected static void printChunk(Tile[] currentTiles) 
	{
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 0 ].toString() , 15 ) );
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 1 ].toString() , 15 ) );
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 2 ].toString() , 15 ) );
		System.out.println();
		
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 3 ].toString() , 15 ) );
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 4 ].toString() , 15 ) );
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 5 ].toString() , 15 ) );
		System.out.println();
		
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 6 ].toString() , 15 ) );
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 7 ].toString() , 15 ) );
		System.out.print( " | "+StringUtils.rightPad( currentTiles[ 8 ].toString() , 15 ) );
		System.out.println();		
	}
	
	public void moveCameraRelative(float deltaX,float deltaY) 
	{
		camera.position.add(deltaX,deltaY,0);
		camera.update(true);
		
		// world (0,0) is at center of tile at (0,0), adjust coordinates by half tile size
		float trueX = camera.position.x - Tile.HALF_TILE_WIDTH;
		float trueY = camera.position.y - Tile.HALF_TILE_HEIGHT;
		
		// tile 'world' coordinates 
		int newCameraTileX = (int) (trueX / Tile.TILE_WIDTH);
		int newCameraTileY = (int) (trueY / Tile.TILE_HEIGHT);
		
		if ( newCameraTileX == cameraTileX && newCameraTileY == cameraTileY ) 
		{
			// camera still on same tile, nothing to do
			return;
		}
		// camera moved to different tile, load adjacent tiles
		moveCameraToTile( newCameraTileX , newCameraTileY );
	}
	
	protected void moveCameraToTile(int newCameraTileX,int newCameraTileY) 
	{
		final int dx = newCameraTileX - cameraTileX;
		final int dy = newCameraTileY - cameraTileY;
		
		if ( dx < -1 || dx > 1 || dy < -1 || dy > 1 ) {
			throw new IllegalArgumentException("Invalid movement: dx="+dx+",dy="+dy);
		}
		
		cameraTileX = newCameraTileX;
		cameraTileY = newCameraTileY;	
		
		switch( dx ) 
		{
			case -1:
				switch( dy ) 
				{
					case -1:
						// left , down
						tileManager.unloadTiles( currentChunk[0],currentChunk[1],currentChunk[2],currentChunk[5],currentChunk[8]);
						
						currentChunk[1] = currentChunk[3];
						currentChunk[2] = currentChunk[4];
						currentChunk[4] = currentChunk[6];
						currentChunk[5] = currentChunk[7];
						
						currentChunk[0] = tileManager.loadTile( newCameraTileX-1, newCameraTileY+1);
						currentChunk[3] = tileManager.loadTile( newCameraTileX-1, newCameraTileY);
						currentChunk[6] = tileManager.loadTile( newCameraTileX-1, newCameraTileY-1);
						currentChunk[7] = tileManager.loadTile( newCameraTileX, newCameraTileY-1);
						currentChunk[8] = tileManager.loadTile( newCameraTileX+1, newCameraTileY-1);						
												
						break;						
					case 0:
						// left
						tileManager.unloadTiles( currentChunk[2],currentChunk[5],currentChunk[8]);
						
						currentChunk[2] = currentChunk[1];
						currentChunk[5] = currentChunk[4];
						currentChunk[8] = currentChunk[7];
						
						currentChunk[1] = currentChunk[0];
						currentChunk[4] = currentChunk[3];
						currentChunk[7] = currentChunk[6];
						
						currentChunk[0] = tileManager.loadTile( newCameraTileX-1, newCameraTileY+1 );
						currentChunk[3] = tileManager.loadTile( newCameraTileX-1, newCameraTileY );
						currentChunk[6] = tileManager.loadTile( newCameraTileX-1, newCameraTileY-1 );
						break;
					case 1:
						// left , up
						tileManager.unloadTiles( currentChunk[2],currentChunk[5],currentChunk[6],currentChunk[7],currentChunk[8]);

						currentChunk[8] = currentChunk[4];							
						currentChunk[4] = currentChunk[0];
						currentChunk[5] = currentChunk[1];
						currentChunk[7] = currentChunk[3];
						
						currentChunk[0] = tileManager.loadTile( newCameraTileX-1, newCameraTileY+1 );
						currentChunk[1] = tileManager.loadTile( newCameraTileX, newCameraTileY+1 );
						currentChunk[2] = tileManager.loadTile( newCameraTileX+1, newCameraTileY+1 ); 
						currentChunk[3] = tileManager.loadTile( newCameraTileX-1, newCameraTileY );
						currentChunk[6] = tileManager.loadTile( newCameraTileX-1, newCameraTileY-1 );
						break;						
				}
				break;
			case 0:
				switch( dy ) {
					case -1:
						// down
						tileManager.unloadTiles( currentChunk[0],currentChunk[1],currentChunk[2]);
						
						currentChunk[0] = currentChunk[3];
						currentChunk[1] = currentChunk[4];
						currentChunk[2] = currentChunk[5];
						
						currentChunk[3] = currentChunk[6];
						currentChunk[4] = currentChunk[7];
						currentChunk[5] = currentChunk[8];
						
						currentChunk[6] = tileManager.loadTile( newCameraTileX-1, newCameraTileY-1 );
						currentChunk[7] = tileManager.loadTile( newCameraTileX, newCameraTileY-1 );
						currentChunk[8] = tileManager.loadTile( newCameraTileX+1, newCameraTileY-1 );						
						break;
					case 0:
						// dx = 0 , dy = 0 => no changes
						break;
					case 1:
						// up						
						currentChunk[6] = currentChunk[3];
						currentChunk[7] = currentChunk[4];
						currentChunk[8] = currentChunk[5];
						
						currentChunk[3] = currentChunk[0];
						currentChunk[4] = currentChunk[1];
						currentChunk[5] = currentChunk[2];
						
						currentChunk[0] = tileManager.loadTile( newCameraTileX-1, newCameraTileY+1 );
						currentChunk[1] = tileManager.loadTile( newCameraTileX, newCameraTileY+1 );
						currentChunk[2] = tileManager.loadTile( newCameraTileX+1, newCameraTileY+1 );						
						break;						
				}				
				break;
			case 1:
				switch( dy ) {
					case -1:
						// right , down
						tileManager.unloadTiles( currentChunk[0],currentChunk[1],currentChunk[2],currentChunk[3],currentChunk[6]);
						
						currentChunk[0] = currentChunk[4];
						currentChunk[1] = currentChunk[5];
						currentChunk[3] = currentChunk[7];
						currentChunk[4] = currentChunk[8];
						
						currentChunk[2] = tileManager.loadTile( newCameraTileX+1 , newCameraTileY+1);
						currentChunk[5] = tileManager.loadTile( newCameraTileX+1 , newCameraTileY);
						currentChunk[6] = tileManager.loadTile( newCameraTileX-1 , newCameraTileY-1);
						currentChunk[7] = tileManager.loadTile( newCameraTileX , newCameraTileY-1);
						currentChunk[8] = tileManager.loadTile( newCameraTileX+1 , newCameraTileY-1);							
						break;
					case 0:
						// right
						tileManager.unloadTiles( currentChunk[0],currentChunk[3],currentChunk[6]);
						
						currentChunk[0] = currentChunk[1];
						currentChunk[3] = currentChunk[4];
						currentChunk[6] = currentChunk[7];
						
						currentChunk[1] = currentChunk[2];
						currentChunk[4] = currentChunk[5];
						currentChunk[7] = currentChunk[8];
						
						currentChunk[2] = tileManager.loadTile( newCameraTileX+1, newCameraTileY-1 );
						currentChunk[5] = tileManager.loadTile( newCameraTileX+1, newCameraTileY );
						currentChunk[8] = tileManager.loadTile( newCameraTileX+1, newCameraTileY+1 );						
						break;
					case 1:
						// right , up
						tileManager.unloadTiles( currentChunk[0],currentChunk[3],currentChunk[6],currentChunk[7],currentChunk[8]);
						
						currentChunk[3] = currentChunk[1];
						currentChunk[6] = currentChunk[4];
						currentChunk[4] = currentChunk[2];
						currentChunk[7] = currentChunk[5];
						
						currentChunk[0] = tileManager.loadTile( newCameraTileX-1, newCameraTileY+1 );
						currentChunk[1] = tileManager.loadTile( newCameraTileX, newCameraTileY+1 );
						currentChunk[2] = tileManager.loadTile( newCameraTileX+1, newCameraTileY+1 );
						currentChunk[5] = tileManager.loadTile( newCameraTileX+1, newCameraTileY );
						currentChunk[8] = tileManager.loadTile( newCameraTileX+1, newCameraTileY-1 );						
												
						break;						
				}				
				break;
		}
	}
	
	public void setTileManager(TileManager tileManager) {
		this.tileManager = tileManager;
	}
	
	public void viewportResized(int width,int height) 
	{
        	Vector3 oldPos = new Vector3(camera.position);
        	Vector3 oldDir = new Vector3(camera.direction);
        	
        	camera.setToOrtho(false,width,height);
        	
        	camera.position.set(oldPos);
        	camera.direction.set(oldDir);
        camera.update(true);
	}
}