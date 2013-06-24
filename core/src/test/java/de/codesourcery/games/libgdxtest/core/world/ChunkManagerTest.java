package de.codesourcery.games.libgdxtest.core.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class ChunkManagerTest extends TestCase {

	protected static class Location 
	{
		protected static Location CENTER=new Location(0,0,"C");
		protected static Location NW=new Location(-1,1,"NW");
		protected static Location N=new Location(0,1,"N");
		protected static Location NE=new Location(1,1,"NE");
		protected static Location W=new Location(-1,0,"W");
		protected static Location E=new Location(1,0,"E");
		protected static Location SW=new Location(-1,-1,"SW");
		protected static Location S=new Location(0,-1,"S");
		protected static Location SE=new Location(1,-1,"SE");
		
		private final String name;
		private final int x;
		private final int y;
		
		public Location(int x,int y,String name) {
			this.x = x;
			this.y = y;
			this.name = name;
		}
		
		@Override
		public String toString() { return name; }
		
		public static Location valueOf(int x,int y) {
			for ( Location l : valid() ) {
				if ( l.x == x && l.y == y ) {
					return l;
				}
			}
			return new Location(x,y,"x="+x+",y="+y);
		}
		
		public static Location[] valid() 
		{
			return new Location[]{CENTER,NW,N,NE,W,E,SW,S,SE};
		}
		
		@Override
		public boolean equals(Object obj) 
		{
			if ( obj instanceof Location) {
				return ((Location) obj).x == x && ((Location) obj).y == y;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return 31+(31+x*31)*31+y*31;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected static class MockTileManager extends TileManager 
	{
		private List<Location> loaded = new ArrayList<>();
		private List<Location> unloaded = new ArrayList<>();
		
		private final MyTile[] tiles = new MyTile[] {
				tile(Location.NW),tile(Location.N),tile(Location.NE),					
				tile(Location.W),tile(Location.CENTER),tile(Location.E),
				tile(Location.SW),tile(Location.S),tile(Location.SE)
		};
		
		public void assertLoaded(Location... locations) 
		{
			if ( loaded.size() != locations.length ) {
				fail("Loaded tiles size mismatch: actual = "+loaded+" , expected: "+Arrays.asList(locations));
			}
			for ( Location l : locations ) 
			{
				boolean found = false;
				for (Iterator<Location> it = loaded.iterator(); it.hasNext();) 
				{
					Location locLoaded = it.next();
					if ( locLoaded.equals( l ) ) 
					{
						it.remove();
						found = true;
						break;
					}
				}
				assertTrue("Location "+l+" not loaded?",found);
			}
			if ( ! loaded.isEmpty() ) {
				fail("Unexpected tiles loaded: "+loaded);
			}
		}
		
		public void assertUnloaded(Location... locations) 
		{
			if ( unloaded.size() != locations.length ) {
				fail("Unloaded tiles size mismatch: actual = "+unloaded+" , expected: "+Arrays.asList(locations));
			}
			for ( Location l : locations ) 
			{
				boolean found = false;
				for (Iterator<Location> it = unloaded.iterator(); it.hasNext();) 
				{
					Location locLoaded = it.next();
					if ( locLoaded.equals( l ) ) 
					{
						it.remove();
						found = true;
						break;
					}
				}
				assertTrue("Location "+l+" not unloaded?",found);
			}
			if ( ! unloaded.isEmpty() ) {
				fail("Unexpected tiles unloaded: "+unloaded);
			}
		}		
		
		private MyTile tile(final Location loc) 
		{
			return new MyTile( loc );
		}		

		@Override
		public MyTile loadTile(int tileX, int tileY) 
		{
			MyTile result;
			if ( tileX < -1 || tileX > 1 || tileY < -1 || tileY > 1 ) {
				result = new MyTile(Location.valueOf(tileX, tileY));
			} else {
				result = tiles[(-tileY+1)*3 + (tileX+1) ];
			}
			loaded.add( result.loc );
			System.out.println( "LOADED: ("+tileX+","+tileY+") => "+result);
			return result;
		}
		
		@Override
		public void unloadTile(Tile tile) {
			Location loc = ((MyTile) tile).loc;
			System.out.println("Unload: "+loc);
			unloaded.add( loc );
		}
		
		public void unloadTiles(Tile tile1, Tile tile2, Tile tile3) {
			unloadTile(tile1);
			unloadTile(tile2);
			unloadTile(tile3);
		}
		
		public void unloadTiles(Tile tile1, Tile tile2, Tile tile3, Tile tile4, Tile tile5) {
			unloadTile(tile1);
			unloadTile(tile2);
			unloadTile(tile3);
			unloadTile(tile4);
			unloadTile(tile5);			
		}		
	}

	private static final class MyTile extends Tile {
		
		public final Location loc;

		public MyTile(Location loc) {
			super(loc.x,loc.y,null);
			this.loc = loc;
		}
		@Override
		public String toString() {
			return "Tile '"+loc+"'";
		}
	}
	
	public void testInitialLoad() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.assertLoaded( Location.valid() );
		assertEquals(0,tileManager.unloaded.size());
	}
	
	public void testMoveCameraRight() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( 1 , 0 );
		
		// verify#
		chunkManager.printChunk();
		
		tileManager.assertUnloaded( Location.NW , Location.W , Location.SW );
		tileManager.assertLoaded( Location.valueOf( 2,-1) , Location.valueOf( 2,0) ,Location.valueOf( 2,1) );
	}	
	
	public void testMoveCameraRightAndUp() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( 1 , 1 );
		
		// verify
		tileManager.assertUnloaded( Location.NW , Location.W , Location.SW , Location.S , Location.SE );
		tileManager.assertLoaded( Location.valueOf( 0,2) , Location.valueOf( 1,2) ,Location.valueOf( 2,2),Location.valueOf( 2,1),Location.valueOf( 2,0) );
		
		chunkManager.printChunk();		
	}	
	
	public void testMoveCameraRightAndDown() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( 1 , -1 );
		
		// verify
		chunkManager.printChunk();
		tileManager.assertUnloaded( Location.NW , Location.W , Location.SW , Location.N , Location.NE );
		tileManager.assertLoaded( Location.valueOf( 0,-2) , Location.valueOf( 1,-2) ,Location.valueOf( 2,-2),Location.valueOf( 2,-1),Location.valueOf( 2,0) );
	}	
	
	public void testMoveCameraLeft() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( -1 , 0 );
		
		// verify
		tileManager.assertUnloaded( Location.NE , Location.E , Location.SE );
		tileManager.assertLoaded( Location.valueOf( -2,-1) , Location.valueOf( -2,0) ,Location.valueOf( -2,1) );
		
		chunkManager.printChunk();		
	}	
	
	public void testMoveCameraLeftAndUp() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( -1 , 1 );
		
		// verify
		tileManager.assertUnloaded( Location.SW , Location.S , Location.SE , Location.E,Location.NE);
		tileManager.assertLoaded( Location.valueOf( -2,2) , Location.valueOf( -2,1) ,Location.valueOf( -2,0),Location.valueOf( -1,2),Location.valueOf( 0,2 ) );
		
		chunkManager.printChunk();
	}	
	
	public void testMoveCameraLeftAndDown() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( -1 , -1 );
		
		// verify
		tileManager.assertUnloaded( Location.NW , Location.N , Location.NE , Location.E,Location.SE);
		tileManager.assertLoaded( Location.valueOf( -2,0) , Location.valueOf( -2,-1) ,Location.valueOf( -2,-2),Location.valueOf( -1,-2),Location.valueOf( 0,-2 ) );
		
		chunkManager.printChunk();		
	}	
	
	public void testMoveCameraUp() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( 0 , 1 );
		
		// verify
		chunkManager.printChunk();			
		tileManager.assertUnloaded( Location.SW , Location.S , Location.SE );
		tileManager.assertLoaded( Location.valueOf( -1,2) , Location.valueOf( 0,2) ,Location.valueOf( 1,2) );
	}	
	
	public void testMoveCameraDown() 
	{
		ChunkManager chunkManager= new ChunkManager();
		MockTileManager tileManager = new MockTileManager();
		chunkManager.setTileManager( tileManager );		
		
		chunkManager.getCurrentChunk();
		
		tileManager.loaded.clear();
		tileManager.unloaded.clear();
		
		// test
		chunkManager.moveCameraToTile( 0 , -1 );
		
		// verify
		chunkManager.printChunk();	
		
		tileManager.assertUnloaded( Location.NW , Location.N , Location.NE );
		tileManager.assertLoaded( Location.valueOf( -1,-2) , Location.valueOf( 0,-2) ,Location.valueOf( 1,-2) );
	}	
}