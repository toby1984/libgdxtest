package de.codesourcery.games.libgdxtest.core.world;

public class NavMeshGenerator
{
    public static final class NavMesh {
        
        public final int width;
        public final float[] height;
        
        public NavMesh(int width) 
        {
            this.width = width;
            this.height = new float[width*width];
        }
        
        public float get(int x,int y) {
            return height[x+y*width];
        }
        
        public void set(int x,int y,float h) {
            height[x+y*width] = h;
        }        
    }
    
    public NavMesh generateGrid(float[] heightmap,int heightmapSize,int tileSize) 
    {
        final int meshSize = heightmapSize / tileSize;
        final NavMesh result = new NavMesh( meshSize );
        
        for ( int x = 0 ; x < meshSize ; x++ ) 
        {
            for ( int y = 0 ; y < meshSize ; y++) 
            {
                float avgHeight = calcCellAverageHeight( x * tileSize  , y * tileSize , tileSize , heightmap , heightmapSize );
                result.set( x,y, avgHeight );
            }
        }
        return result;
    }
    
    private float calcCellAverageHeight(float cellX,float cellY , int tileSize,float[] heightmap,int heightmapSize) 
    {
        float sum=0.0f;
        int cellCount=0;
        
        for ( float x = cellX ; x < (cellX+tileSize) ; x+=1.0f ) 
        {
            for ( float y = cellY ; y < (cellY+tileSize) ; y+=1.0f ) 
            {
                int ix = (int) Math.floor(x);
                int iy = (int) Math.floor(y);
                
                if ( ix < heightmapSize && iy < heightmapSize ) 
                {
                    sum += heightmap[ix+iy*heightmapSize];
                    cellCount++;
                }
            }
        }
        return cellCount != 0 ? sum/cellCount : 1f;
    }
}