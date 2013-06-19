package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.math.collision.BoundingBox;

public class Utils
{
    public static boolean intersect(BoundingBox a,BoundingBox b) 
    {

        if (a.max.x < b.min.x || 
            a.max.y < b.min.y || 
            a.min.x > b.max.x || 
            a.min.y > b.max.y) 
            {
                return false;
            }
        return true;        
    }
    
    public static float squaredDistance(Entity e1,Entity e2) 
    {
        float dx = e2.position.x - e1.position.x;
        float dy = e2.position.y - e1.position.y;
        return dx*dx+dy*dy;
    }    
    
    public static float width(BoundingBox box) {
        return box.max.x - box.min.x;
    }
    
    public static float height(BoundingBox box) {
        return box.max.y - box.min.y;
    }    
}
