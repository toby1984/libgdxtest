package de.codesourcery.games.libgdxtest.core.world;

import org.apache.commons.lang.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;


public class ChunkManager 
{
    // the current chunk (3x3 tiles)
    private final Tile[] currentChunk = new Tile[3*3];

    private TileManager tileManager;

    public final OrthographicCamera camera = new OrthographicCamera();

    // the tile the camera currently is in

    // the camera is always located in the CENTER tile of the 3x3 chunk

    // tiles are layed out in a cartesian coordinate system
    // with the y-axis pointing up and the x-axis pointing right
    // world coordinates (0,0) are right in the center of tile (0,0)

    public int cameraTileX = 0;
    public int cameraTileY = 0;

    private boolean reloadWholeChunk= true;

    public ChunkManager() 
    {
    }

    public Tile[] getCurrentChunk() 
    {
        if ( reloadWholeChunk ) 
        {
        	currentChunk[ 0 ] = tileManager.loadTile( cameraTileX - 1 , cameraTileY+1 );
        	currentChunk[ 1 ] = tileManager.loadTile( cameraTileX     , cameraTileY+1 );
        	currentChunk[ 2 ] = tileManager.loadTile( cameraTileX + 1 , cameraTileY+1 );
        	
        	currentChunk[ 3 ] = tileManager.loadTile( cameraTileX - 1 , cameraTileY   );
        	currentChunk[ 4 ] = tileManager.loadTile( cameraTileX     , cameraTileY   );
        	currentChunk[ 5 ] = tileManager.loadTile( cameraTileX + 1 , cameraTileY   );
        	
        	currentChunk[ 6 ] = tileManager.loadTile( cameraTileX - 1 , cameraTileY-1 );
        	currentChunk[ 7 ] = tileManager.loadTile( cameraTileX     , cameraTileY-1 );
        	currentChunk[ 8 ] = tileManager.loadTile( cameraTileX + 1 , cameraTileY-1 );
        	
            System.out.println("Center: "+cameraTileX+" / "+cameraTileY);
            printChunk(currentChunk);
            reloadWholeChunk=false;
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

    /**
     * 
     * @param deltaX
     * @param deltaY
     * @return <code>true</code> if camera moved to a new tile, otherwise false
     */
    public boolean moveCameraRelative(float deltaX,float deltaY) 
    {
        camera.position.add(deltaX,deltaY,0);
        camera.update(true);

        // world (0,0) is at center of tile at (0,0), adjust coordinates by half tile size
        float trueX = camera.position.x+Tile.HALF_TILE_WIDTH;
        float trueY = (camera.position.y+Tile.HALF_TILE_HEIGHT);

        // tile 'world' coordinates 
        int newCameraTileX = (int) Math.floor(trueX / Tile.WIDTH);
        int newCameraTileY = (int) Math.floor(trueY / Tile.HEIGHT);

        if ( newCameraTileX == cameraTileX && newCameraTileY == cameraTileY ) 
        {
            // camera still on same tile, nothing to do
            return false;
        }
        // camera moved to different tile, load adjacent tiles
        System.out.println("Camera: "+camera.position+" => tile: "+newCameraTileX+" / "+newCameraTileY);
        moveCameraToTile( newCameraTileX , newCameraTileY );
        return true;
    }

    public void renderCurrentChunk(SpriteBatch renderer) 
    {
    	renderer.begin();
    	renderer.setProjectionMatrix( camera.combined );
    	
        final Tile[] tiles = getCurrentChunk();

        final float xOffset = -0.5f*Tile.WIDTH + ( cameraTileX*Tile.WIDTH )-Tile.WIDTH;
        final float yOffset = -0.5f*Tile.HEIGHT + ( cameraTileY*Tile.HEIGHT )+Tile.HEIGHT;

        float xScreen = xOffset;
        float yScreen = yOffset;
        
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[0].getBackgroundTexture() );
        xScreen += Tile.WIDTH;
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[1].getBackgroundTexture() );
        xScreen += Tile.WIDTH;
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[2].getBackgroundTexture() );

        xScreen = xOffset;
        yScreen -= Tile.HEIGHT;
        
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[3].getBackgroundTexture() );
        xScreen += Tile.WIDTH;
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[4].getBackgroundTexture() );
        xScreen += Tile.WIDTH;
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[5].getBackgroundTexture() );
        
        xScreen = xOffset;
        yScreen -= Tile.HEIGHT;
        
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[6].getBackgroundTexture() );
        xScreen += Tile.WIDTH;
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[7].getBackgroundTexture() );
        xScreen += Tile.WIDTH;
        renderQuad( xScreen, yScreen , Tile.WIDTH,Tile.HEIGHT , tiles[8].getBackgroundTexture() );
        
        renderer.end();
    }
    
    private void renderQuad(float xStart,float yStart,float cellWidth,float cellHeight,Texture texture) 
    {
        Mesh quad = new Mesh(true, 2*3 , 2*3, 
                new VertexAttribute(Usage.Position, 3, "a_position"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoords"));
        
        final float[] vertices = new float[ 2 * 3 * 5 ];
        final short[] indices = new short[ 2 * 3 ];

        int vertexPtr = 0;
        
        float x0 = xStart;
        float y0 = yStart;
        
        float x1 = x0 + cellWidth;
        float y1 = y0 + cellHeight;
        
        // triangle no. #1
        vertices[vertexPtr++] = x0;
        vertices[vertexPtr++] = y0;
        vertices[vertexPtr++] = 0;
        vertices[vertexPtr++] = 0; // texture U
        vertices[vertexPtr++] = 1; // texture V
        
        vertices[vertexPtr++] = x1;
        vertices[vertexPtr++] = y0;
        vertices[vertexPtr++] = 0;
        vertices[vertexPtr++] = 1; // texture U
        vertices[vertexPtr++] = 1; // texture V        
        
        vertices[vertexPtr++] = x0;
        vertices[vertexPtr++] = y1;
        vertices[vertexPtr++] = 0;   
        vertices[vertexPtr++] = 0; // texture U
        vertices[vertexPtr++] = 0; // texture V           
        
        // triangle no. #2
        vertices[vertexPtr++] = x0;
        vertices[vertexPtr++] = y1;
        vertices[vertexPtr++] = 0;
        vertices[vertexPtr++] = 0; // texture U
        vertices[vertexPtr++] = 0; // texture V          
        
        vertices[vertexPtr++] = x1;
        vertices[vertexPtr++] = y0;
        vertices[vertexPtr++] = 0;    
        vertices[vertexPtr++] = 1; // texture U
        vertices[vertexPtr++] = 1; // texture V          
        
        vertices[vertexPtr++] = x1;
        vertices[vertexPtr++] = y1;
        vertices[vertexPtr++] = 0;  
        vertices[vertexPtr++] = 1; // texture U
        vertices[vertexPtr++] = 0; // texture V          
        
        indices[0] = 0;
        indices[1] = 1;
        indices[2] = 2;
        indices[3] = 3;
        indices[4] = 4;
        indices[5] = 5;    
        
        quad.setVertices( vertices );
        quad.setIndices( indices );
        
        Gdx.graphics.getGL10().glEnable(GL10.GL_TEXTURE_2D);
        texture.bind();
        quad.render(GL10.GL_TRIANGLES);
        quad.dispose();
    }
    
    protected void moveCameraToTile(int newCameraTileX,int newCameraTileY) 
    {
        final int dx = newCameraTileX - cameraTileX;
        final int dy = newCameraTileY - cameraTileY;

        if ( dx < -1 || dx > 1 || dy < -1 || dy > 1 ) 
        {
            throw new IllegalArgumentException("Invalid chunk movement: dx="+dx+",dy="+dy);
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
                    	tileManager.unloadTiles( currentChunk[6],currentChunk[7],currentChunk[8]);
                    	
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