package de.codesourcery.games.libgdxtest.core.world;

import org.apache.commons.lang.StringUtils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;


public class ChunkManager 
{
	public static final int TILES_PER_ROW=3;
	public static final int TILES_PER_COLUMN=3;
	
	// chunk coordinates
	private Tile[] currentTiles;
	
	private TileManager tileManager;
	
    public final OrthographicCamera camera = new OrthographicCamera();
    
    // the tile the camera currently is in
    private int cameraTileX = 0;
    private int cameraTileY = 0;
    
	public ChunkManager() 
	{
	}
	
	public Tile[] getCurrentChunk() 
	{
		if ( currentTiles == null ) 
		{
			currentTiles = new Tile[TILES_PER_ROW*TILES_PER_COLUMN];
			
			int x = cameraTileX-1; 
			for ( int col = 0 ; col < TILES_PER_ROW ; col++ , x++ ) 
			{ 
				int y = cameraTileY+1;
				for ( int row = 0 ; row < TILES_PER_COLUMN ; row++ , y-- ) 
				{
					final Tile tile = tileManager.getTile( x , y );
					currentTiles[ row * TILES_PER_ROW + col ] = tile;
				}
			}
		}
		return currentTiles;
	}
	
	protected void printChunk() 
	{
		getCurrentChunk();
		printChunk(currentTiles);
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
		
		// world (0,0) is at center of middle tile, adjust coordinates by half tile size
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
						tileManager.unloadTiles( currentTiles[0],currentTiles[1],currentTiles[2],currentTiles[5],currentTiles[8]);
						
						currentTiles[1] = currentTiles[3];
						currentTiles[2] = currentTiles[4];
						currentTiles[4] = currentTiles[6];
						currentTiles[5] = currentTiles[7];
						
						currentTiles[0] = tileManager.getTile( newCameraTileX-1, newCameraTileY+1);
						currentTiles[3] = tileManager.getTile( newCameraTileX-1, newCameraTileY);
						currentTiles[6] = tileManager.getTile( newCameraTileX-1, newCameraTileY-1);
						currentTiles[7] = tileManager.getTile( newCameraTileX, newCameraTileY-1);
						currentTiles[8] = tileManager.getTile( newCameraTileX+1, newCameraTileY-1);						
												
						break;						
					case 0:
						// left
						tileManager.unloadTiles( currentTiles[2],currentTiles[5],currentTiles[8]);
						
						currentTiles[2] = currentTiles[1];
						currentTiles[5] = currentTiles[4];
						currentTiles[8] = currentTiles[7];
						
						currentTiles[1] = currentTiles[0];
						currentTiles[4] = currentTiles[3];
						currentTiles[7] = currentTiles[6];
						
						currentTiles[0] = tileManager.getTile( newCameraTileX-1, newCameraTileY+1 );
						currentTiles[3] = tileManager.getTile( newCameraTileX-1, newCameraTileY );
						currentTiles[6] = tileManager.getTile( newCameraTileX-1, newCameraTileY-1 );
						break;
					case 1:
						// left , up
						tileManager.unloadTiles( currentTiles[2],currentTiles[5],currentTiles[6],currentTiles[7],currentTiles[8]);

						currentTiles[8] = currentTiles[4];							
						currentTiles[4] = currentTiles[0];
						currentTiles[5] = currentTiles[1];
						currentTiles[7] = currentTiles[3];
						
						currentTiles[0] = tileManager.getTile( newCameraTileX-1, newCameraTileY+1 );
						currentTiles[1] = tileManager.getTile( newCameraTileX, newCameraTileY+1 );
						currentTiles[2] = tileManager.getTile( newCameraTileX+1, newCameraTileY+1 ); 
						currentTiles[3] = tileManager.getTile( newCameraTileX-1, newCameraTileY );
						currentTiles[6] = tileManager.getTile( newCameraTileX-1, newCameraTileY-1 );
						break;						
				}
				break;
			case 0:
				switch( dy ) {
					case -1:
						// down
						tileManager.unloadTiles( currentTiles[0],currentTiles[1],currentTiles[2]);
						
						currentTiles[0] = currentTiles[3];
						currentTiles[1] = currentTiles[4];
						currentTiles[2] = currentTiles[5];
						
						currentTiles[3] = currentTiles[6];
						currentTiles[4] = currentTiles[7];
						currentTiles[5] = currentTiles[8];
						
						currentTiles[6] = tileManager.getTile( newCameraTileX-1, newCameraTileY-1 );
						currentTiles[7] = tileManager.getTile( newCameraTileX, newCameraTileY-1 );
						currentTiles[8] = tileManager.getTile( newCameraTileX+1, newCameraTileY-1 );						
						break;
					case 0:
						// dx = 0 , dy = 0 => no changes
						break;
					case 1:
						// up
						tileManager.unloadTiles( currentTiles[6],currentTiles[7],currentTiles[8]);
						
						currentTiles[6] = currentTiles[3];
						currentTiles[7] = currentTiles[4];
						currentTiles[8] = currentTiles[5];
						
						currentTiles[3] = currentTiles[0];
						currentTiles[4] = currentTiles[1];
						currentTiles[5] = currentTiles[2];
						
						currentTiles[0] = tileManager.getTile( newCameraTileX-1, newCameraTileY+1 );
						currentTiles[1] = tileManager.getTile( newCameraTileX, newCameraTileY+1 );
						currentTiles[2] = tileManager.getTile( newCameraTileX+1, newCameraTileY+1 );						
						break;						
				}				
				break;
			case 1:
				switch( dy ) {
					case -1:
						// right , down
						tileManager.unloadTiles( currentTiles[0],currentTiles[1],currentTiles[2],currentTiles[3],currentTiles[6]);
						
						currentTiles[0] = currentTiles[4];
						currentTiles[1] = currentTiles[5];
						currentTiles[3] = currentTiles[7];
						currentTiles[4] = currentTiles[8];
						
						currentTiles[2] = tileManager.getTile( newCameraTileX+1 , newCameraTileY+1);
						currentTiles[5] = tileManager.getTile( newCameraTileX+1 , newCameraTileY);
						currentTiles[6] = tileManager.getTile( newCameraTileX-1 , newCameraTileY-1);
						currentTiles[7] = tileManager.getTile( newCameraTileX , newCameraTileY-1);
						currentTiles[8] = tileManager.getTile( newCameraTileX+1 , newCameraTileY-1);							
						break;
					case 0:
						// right
						tileManager.unloadTiles( currentTiles[0],currentTiles[3],currentTiles[6]);
						
						currentTiles[0] = currentTiles[1];
						currentTiles[3] = currentTiles[4];
						currentTiles[6] = currentTiles[7];
						
						currentTiles[1] = currentTiles[2];
						currentTiles[4] = currentTiles[5];
						currentTiles[7] = currentTiles[8];
						
						currentTiles[2] = tileManager.getTile( newCameraTileX+1, newCameraTileY-1 );
						currentTiles[5] = tileManager.getTile( newCameraTileX+1, newCameraTileY );
						currentTiles[8] = tileManager.getTile( newCameraTileX+1, newCameraTileY+1 );						
						break;
					case 1:
						// right , up
						tileManager.unloadTiles( currentTiles[0],currentTiles[3],currentTiles[6],currentTiles[7],currentTiles[8]);
						
						currentTiles[3] = currentTiles[1];
						currentTiles[6] = currentTiles[4];
						currentTiles[4] = currentTiles[2];
						currentTiles[7] = currentTiles[5];
						
						currentTiles[0] = tileManager.getTile( newCameraTileX-1, newCameraTileY+1 );
						currentTiles[1] = tileManager.getTile( newCameraTileX, newCameraTileY+1 );
						currentTiles[2] = tileManager.getTile( newCameraTileX+1, newCameraTileY+1 );
						currentTiles[5] = tileManager.getTile( newCameraTileX+1, newCameraTileY );
						currentTiles[8] = tileManager.getTile( newCameraTileX+1, newCameraTileY-1 );						
												
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
