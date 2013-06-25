package de.codesourcery.games.libgdxtest.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import de.codesourcery.games.libgdxtest.core.world.NavMeshGenerator.NavMesh;

public class PathFinder
{
    private final PriorityQueue<PathNode> openList = new PriorityQueue<>();
    private final byte[] visited;

    private final int meshWidth;
    private final NavMesh mesh;

    private float walkableGroundLevel;
    
    private int dstX;
    private int dstY;

    public static final class PathNode implements Comparable<PathNode> 
    {
        public final int x;
        public final int y;

        public PathNode predecessor;

        public float f;
        public float g;

        public PathNode(int x, int y)
        {
            this(x,y,0,0);
        }

        public PathNode(int x, int y,float f,float g)
        {
            this.x = x;
            this.y = y;
            this.f = f;
            this.g = g;
        }

        @Override
        public String toString()
        {
            return "PathNode[ "+x+" | "+y+" | f = "+f+" ]";
        }

        @Override
        public int hashCode()
        {
            final int result = 31 + x;
            return 31 * result + y;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || obj.getClass() != PathNode.class) {
                return false;
            }
            final PathNode other = (PathNode) obj;
            return x == other.x && y == other.y;
        }

        @Override
        public int compareTo(PathNode o)
        {
            if ( this.f < o.f ) {
                return -1;
            } 
            if ( this.f > o.f ) {
                return 1;
            }
            return 0;
        }
    }

    public static final class Path 
    {
        public final List<PathNode> path = new ArrayList<>();
    }

    public PathFinder(NavMesh mesh) {
        this.mesh=mesh;
        this.meshWidth = mesh.width;
        this.visited=new byte[meshWidth*meshWidth];
    }

    private void markAsVisited(int x,int y) 
    {
        try {
            visited[x+y*meshWidth]=1;
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("x="+x+",y="+y+",index="+x+y*meshWidth);
        }
    }    
    
    private boolean wasVisited(int x,int y) {
        return visited[x+y*meshWidth] != 0;
    }

    public Path findPath(int startX,int startY, int dstX,int dstY,float walkableGroundLevel) 
    {
        if ( startX < 0 || startX >= meshWidth || startY < 0 || startY >= meshWidth ) {
            throw new IllegalArgumentException("Invalid start position: "+startX+" / "+startY);
        }
        if ( dstX < 0 || dstX >= meshWidth || dstY < 0 || dstY >= meshWidth ) {
            throw new IllegalArgumentException("Invalid dest position: "+dstX+" / "+dstY);
        }        
        
        openList.clear();
        for ( int i = 0 ; i < meshWidth*meshWidth;i++) {
            visited[i]=0;
        }
        
        this.walkableGroundLevel = walkableGroundLevel;
        this.dstX = dstX;
        this.dstY = dstY;

        /*
declare openlist as PriorityQueue with Nodes // Prioritätenwarteschlange
declare closedlist as Set with Nodes          
         */
        // Initialisierung der Open List, die Closed List ist noch leer (die Priorität bzw. der f Wert des Startknotens ist unerheblich)
        // openlist.enqueue(startknoten, 0)
        openList.add( new PathNode( startX,startY ) );
        /*
    // diese Schleife wird durchlaufen bis entweder
    // - die optimale Lösung gefunden wurde oder
    // - feststeht, dass keine Lösung existiert
         */
        do 
        {
            /* repeat
        // Knoten mit dem geringsten f Wert aus der Open List entfernen
        currentNode := openlist.removeMin()
             */
            PathNode current = openList.poll();
            /*
        // Wurde das Ziel gefunden? 
        if currentNode == zielknoten then
            return PathFound
             */
            if ( current.x == dstX && current.y == dstY ) 
            {
                final Path result = new Path();
                do 
                {
                    result.path.add( current );
                    current = current.predecessor;
                } while ( current != null );
                Collections.reverse( result.path );
                return result;
            }
            /*
        // Der aktuelle Knoten soll durch nachfolgende Funktionen
        // nicht weiter untersucht werden damit keine Zyklen entstehen
        closedlist.add(currentNode)
             */
            markAsVisited( current.x , current.y );
            /*
        // Wenn das Ziel noch nicht gefunden wurde: Nachfolgeknoten
        // des aktuellen Knotens auf die Open List setzen
             */
            expandNode(current);
            // until openlist.isEmpty()
        } while ( ! openList.isEmpty() ); /*
    // die Open List ist leer, es existiert kein Pfad zum Ziel
    return NoPathFound
         */
        return null;
    }

    private void expandNode(PathNode current)
    {
        /* überprüft alle Nachfolgeknoten und fügt sie der Open List hinzu, wenn entweder
//- der Nachfolgeknoten zum ersten Mal gefunden wird oder
//- ein besserer Weg zu diesem Knoten gefunden wird
function expandNode(currentNode)
   foreach successor of currentNode */
        for ( int y = current.y - 1 ; y <= current.y + 1 ; y++ ) 
        {
            if ( y >= 0 && y < meshWidth ) 
            {
                for ( int x = current.x - 1 ; x <= current.x + 1 ; x++ ) 
                {
                    if ( x >= 0 && x < meshWidth && ! (current.x == x && current.y == y) ) 
                    {
                        processNode(current,x,y);
                    }
                }
            }
        }
    }

    private void processNode(PathNode current,int succX,int succY) 
    {
        /*
        // wenn der Nachfolgeknoten bereits auf der Closed List ist - tue nichts
        if closedlist.contains(successor) then
            continue
         */
        if ( wasVisited( succX, succY ) ){
            return;
        }
        
        final float succHeight = mesh.get( succX ,  succY );
        // do not try paths that are too steep
        float heightDelta = Math.abs( succHeight - mesh.get( current.x , current.y ) );
        
        // do not walk across mountains
        if ( succHeight > walkableGroundLevel ) {
            System.out.println("Not walkable: "+succX+","+succY+" with height "+succHeight);
            return;
        }        

        // g Wert für den neuen Weg berechnen: g Wert des Vorgängers plus
        // die Kosten der gerade benutzten Kante
        // tentative_g = g(currentNode) + c(currentNode, successor)
        float tentative_g = current.g + 1 + 10*heightDelta; // >>> traversing any edge incurs the same (constant) cost

        // wenn der Nachfolgeknoten bereits auf der Open List ist,
        // aber der neue Weg nicht besser ist als der alte - tue nichts
        PathNode successor = null;
        for ( PathNode node : openList ) // TODO: Replace linear search with something faster... 
        {
            if ( node.x == succX && node.y == succY ) 
            {
                successor = node;
                // if openlist.contains(successor) and tentative_g >= g(successor) then
                //    continue        
                if ( tentative_g >= successor.g ) {
                    return;
                }                  
                break;
            }
        }

        // f Wert des Knotens in der Open List aktualisieren
        // bzw. Knoten mit f Wert in die Open List einfügen
        // f := tentative_g + h(successor)
        float f = tentative_g +h(succX,succY);

        if ( successor != null ) 
        {
            // if openlist.contains(successor) then
            //     openlist.decreaseKey(successor, f)

            // Vorgängerzeiger setzen und g Wert merken
            // successor.predecessor := currentNode            
            successor.predecessor = current;
            // g(successor) = tentative_g
            successor.g = tentative_g;
            // f Wert des Knotens in der Open List aktualisieren
            // bzw. Knoten mit f Wert in die Open List einfügen
            // f := tentative_g + h(successor)            
            successor.f = f;
            openList.remove( successor );
            openList.add( successor );
        } else {
            //     openlist.enqueue(successor, f)
            successor = new PathNode(succX,succY , f , tentative_g );
            successor.predecessor = current;
            openList.add( successor );
        }
    }

    private float h(int x,int y) 
    {
        int dx = dstX - x;
        int dy = dstY - y;
        return dx*dx+dy*dy; // squared distance
    }    
}