package de.codesourcery.games.libgdxtest.core.maze;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.games.libgdxtest.core.maze.MazeTest.Maze.Room;

public class MazeTest
{
    protected static enum Direction {
        NORTH,SOUTH,WEST,EAST;
    }
    
    public static final class Maze 
    {
        public static final class Room 
        {
            public final int x;
            public final int y;
            
            // connected neighbors
            public Room north;
            public Room east;
            public Room south;
            public Room west;
            
            public boolean visited=false;            
            
            public Room(int x, int y)
            {
                this.x = x;
                this.y = y;
            }
            
            public void reset() {
                visited = false;
                north=east=south=west=null;
            }
            
            @Override
            public String toString()
            {
                return "("+x+","+y+")";
            }
            
            private void addPassageTo(Direction d,Room room) 
            {
                switch(d) {
                    case NORTH:
                        assert( north == null );
                        north=room;
                        room.south = this;
                        break;
                    case SOUTH:
                        assert( south == null );
                        south=room;
                        room.north = this;
                        break;
                    case EAST:
                        assert( east == null );
                        east=room;
                        room.west = this;
                        break;
                    case WEST:
                        assert( west == null );
                        west=room;
                        room.east = this;
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled direction: "+d);
                }
            }
            
            
            public void addPassageTo(Room room) 
            {
                if ( room.y == y ) { // can only be the western or eastern neighbor cell
                    if ( room.x == x-1 ) {
                        addPassageTo(Direction.WEST , room );
                    } else {
                        addPassageTo(Direction.EAST , room );
                    }
                } else if ( room.x == x ) { // can only be the northern or southern neighbor cell
                    if ( room.y == y - 1 ) 
                    {
                        addPassageTo(Direction.NORTH , room );
                    } else {
                        addPassageTo(Direction.SOUTH , room );
                    }
                } 
                else 
                {
                    throw new RuntimeException("Unreachable code reached");
                }
            }
        }
        
        public final int size;
        public final Room[] rooms;
        
        private final Point[] directions = {new Point(0,-1),new Point(1,0),new Point(0,1),new Point(-1,0)};
        
        public Maze(int size)
        {
            this.size = size;
            this.rooms = new Room[ size*size ];
            int ptr = 0;
            for ( int y = 0 ; y < size ; y++ ) {
                for ( int x = 0 ; x < size ; x++ ) {
                    rooms[ptr++] = new Room(x,y);
                }
            }              
        }
        
        public void reset() 
        {
            for ( int i = 0 ; i < size*size ; i++ ) {
                rooms[i].reset();
            }            
        }
        
        private void shuffleDirections(Random rnd) 
        {
            /* Fisher-Yates shuffle.
             * To shuffle an array a of n elements (indices 0..n-1):
             * 
             * for i from n − 1 downto 1 do
             *   j ← random integer with 0 ≤ j ≤ i
             *   exchange a[j] and a[i]             
             */
            for ( int i = 3 ; i >= 1 ; i--) 
            {
                int j = rnd.nextInt( i+1 );
                Point tmp = directions[i];
                directions[i] = directions[j];
                directions[j] = tmp;
            }
        }
        
        public Room getUnvisitedNeighbor(Room c,Random rnd) 
        {
            final int x = c.x;
            final int y = c.y;
            
            shuffleDirections(rnd);
            
            for (int i = 0; i < directions.length; i++) 
            {
                final Point p = directions[i];
                Room n = getCell( x+p.x   , y+p.y ); 
                if ( n != null && ! n.visited ) {
                    return n;
                }                 
            }
            return null;
        }
        
        public Room getCell(int x,int y) 
        {
            if ( x >= 0 && x < size && y>=0 && y < size ) {
                return rooms[ x + y * size ];
            }
            return null;
        }        
    }
    
    public static void main(String[] args)
    {
        final Maze maze = new Maze(640);
        MazeTest.generateMaze(maze,System.currentTimeMillis());
        
        final JPanel panel = new JPanel() {
            
            @Override
            protected void paintComponent(Graphics graphics)
            {
                final int w = getWidth()-20;                
                final int h = getHeight()-20;
                
                final int cellWidth = w / maze.size;
                final int cellHeight = h / maze.size;                
                
                // clear screen
                final Graphics2D g = (Graphics2D) graphics;
                
                g.setColor(Color.WHITE );
                g.fillRect( 0 , 0 , w ,h );
                
                // draw maze
                g.setColor(Color.BLACK );
                for ( int y = 0 ; y < maze.size ; y++ ) 
                {
                    for ( int x = 0 ; x < maze.size ; x++ ) 
                    {
                        final Room cell = maze.getCell( x,y );
                        
                        final int xmin = 10+(x * cellWidth);
                        final int xmax = xmin + cellWidth;
                        
                        final int ymin = 10+(y * cellHeight);
                        final int ymax = ymin + cellHeight;
                        
                        if ( cell.north == null ) { // draw north wall
                            g.drawLine( xmin , ymin , xmax , ymin );
                        } 
                        if ( cell.south == null ) { // draw south wall
                            g.drawLine( xmin , ymax , xmax , ymax );
                        }
                        if ( cell.west == null ) { // draw west wall
                            g.drawLine( xmin , ymin , xmin, ymax);
                        } 
                        if ( cell.east == null ) { // draw east wall
                            g.drawLine( xmax , ymin , xmax, ymax );
                        }                        
                    }
                }
            }
        };
        
        panel.setFocusable(true);
        panel.addKeyListener( new KeyAdapter() 
        {
            public void keyTyped(java.awt.event.KeyEvent e) {
                generateMaze(maze,System.currentTimeMillis());
                panel.repaint();
            }
        } );
        final JFrame frame = new JFrame("test");
        frame.setPreferredSize(new Dimension(400,200  ) );
        frame.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.gridx=GridBagConstraints.REMAINDER;
        cnstrs.gridy=GridBagConstraints.REMAINDER;
        cnstrs.weightx=1.0;
        cnstrs.weighty=1.0;
        frame.getContentPane().add(panel,cnstrs);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    public static void generateMaze(Maze result,long seed) 
    {
        result.reset();
        
        final Random rnd = new Random(seed);
        final List<Room> rooms = new ArrayList<>();
        
        final Room start = result.getCell(rnd.nextInt(result.size), rnd.nextInt(result.size) );
        rooms.add( start );
        
        do
        {
            final Room currentRoom = rooms.get( rnd.nextInt(rooms.size() ) );
            
            final Room neighbour = result.getUnvisitedNeighbor(currentRoom,rnd);
            if ( neighbour == null ) 
            {
                rooms.remove( currentRoom );
            } 
            else 
            {
                currentRoom.visited = true;
                neighbour.visited = true;
                currentRoom.addPassageTo( neighbour );
                rooms.add( neighbour );
            }
        } while ( ! rooms.isEmpty() );
    }
}
