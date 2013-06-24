package de.codesourcery.games.libgdxtest.core.world;

import java.util.LinkedHashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Texture;

public class DefaultTileManager extends TileManager
{
    private static final int CACHE_SIZE = 20;
    
    private final LinkedHashMap<TileKey,Tile> cache = new LinkedHashMap<TileKey,Tile>() 
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TileKey,Tile> eldest) {
            if ( size() > CACHE_SIZE ) {
                dispose(eldest.getValue());
                return true;
            }
            return false;
        }
    };
    
    protected static final class TileKey {
        
        public final int x;
        public final int y;
        
        public TileKey(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public int hashCode()
        {
            int result = 31 + x;
            return 31 * result + y;
        }
        @Override
        public boolean equals(Object obj)
        {
            if ( obj != null && obj.getClass() == TileKey.class ) 
            {
                final TileKey other = (TileKey) obj;
                return x == other.x && y == other.y;
            }
            return true;
        }
    }    
    
    private final TileFactory tileFactory;
    
    public DefaultTileManager(TileFactory tileFactory) {
        this.tileFactory = tileFactory;
    }
    
    protected void dispose(Tile tile) 
    {
        System.out.println("dispose(): Tile "+tile);
        tile.dispose();
    }
    
    @Override
    public Tile loadTile(int tileX, int tileY)
    {
//        System.out.println("loadTile(): "+tileX+" / "+tileY);
        
        TileKey key = new TileKey(tileX,tileY );
        Tile result = cache.get( key );
        if ( result == null ) 
        {
            result = createTile(tileX,tileY);
            cache.put(key,result);
        }
        result.activate();
        return result;
    }

    @Override
    public void unloadTiles(Tile tile1, Tile tile2, Tile tile3)
    {
        tile1.passivate();
        tile2.passivate();
        tile3.passivate();
    }

    @Override
    public void unloadTiles(Tile tile1, Tile tile2, Tile tile3, Tile tile4, Tile tile5)
    {
        tile1.passivate();
        tile2.passivate();
        tile3.passivate();
        tile4.passivate();
        tile5.passivate();
    }

    @Override
    public void unloadTile(Tile tile)
    {
        tile.passivate();        
    }
    
    private Tile createTile(int tileX, int tileY)
    {
        return tileFactory.createTile(tileX, tileY);
    }    
}