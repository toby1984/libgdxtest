package de.codesourcery.games.libgdxtest.core.maze;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.badlogic.gdx.math.Vector2;

public class Voronoi2 {

	protected static final class BreakPoint 
	{
		public final Point2D.Float point;

		public BreakPoint(Point2D.Float point) 
		{
			this.point = point;
		}
	}
	
	protected static final class Parabola 
	{
		private final Point2D.Float focus;
		/*
		 * (x-h)^2 = 4p(y-k)
		 */
		public final Point2D.Float v = new Point2D.Float();
		public float h;
		public float k;
		public float p;
		
		public float a; 
		public float b;
		public float c;		
		
		public Parabola(Point2D.Float focus) 
		{
			this.focus = focus;
		}
		
		public boolean intersectsVerticalLine(float xCoordinate) 
		{
			float y = evaluateAt(xCoordinate);
			return ! Float.isInfinite(y) && ! Float.isNaN( y ) && y > 0;
		}
		
		public float a() {
			return a;
		}
		
		public float b() {
			return b;
		}
		
		public float c() {
			return c;
		}
		
		public float[] intersect(Parabola pb) 
		{
			float a = a();
			float b = b();
			float c = c();
			
			float d = pb.a();
			float e = pb.b();
			float f = pb.c();
			
			float disc = -4*a*c+4*a*f+b*b-2*e*b+4*c*d-4*d*f+e*e;
			if ( disc < 0 ) {
				return null;
			}
			
			/*
			 * (-sqrt(-4*a*c+4*a*f+b*b-2*e*b+4*c*d-4*d*f+e*e)-b+e)/(2 (a-d))
			 */
			/*
			 * a-d!=0 and x = (-Math.sqrt(-4*a*c+4*a*f+b*b-2*e*b+4*c*d-4*d*f+e*e)-b+e)/(2*(a-d)) 
			 */
			if ( a == d && (e-b)!= 0) {
				return new float[]{ (c-f)/(e-b) };
			}
			double x1 = (-Math.sqrt(disc)-b+e)/(2*(a-d));
			double x2 = (Math.sqrt(disc)-b+e)/(2*(a-d));
			return new float[]{(float) x1,(float) x2};
		}
		
		public float evaluateAt(float x) {
			return a()*x*x+b()*x+c();
		}
		
		public void calculateCoefficients(float directrixY) 
		{
			v.x = focus.x;
			v.y = (focus.y + directrixY)/2.0f;
			
			// vx = h
			// vy = k
			
			// dy = vy-p => vy - dy = p  
			h = v.x;
			k = v.y;
			p = v.y - directrixY;
			
			a = 1/(4*p);
			b = (-2*h)/(4*p);
			c = ((h*h)/(4*p))+k;
		}

		@Override
		public String toString() {
			return "Parabola [focus=" + focus + ", h=" + h + ", k=" + k+ ", p=" + p + "]";
		}
		
		public void render2(Graphics g,int width,float scaleX,float scaleY) {
			
			float inc = 0.1f;
			
			float a = a();
			float b = b();
			float c = c();
			
			for ( float x = 0 ; x < width ; x+= inc) 
			{
				float y = a*x*x+b*x+c;
				if ( y > 0 ) {
					g.drawLine( floor( x*scaleX ) , floor(y*scaleY) , floor( x*scaleX ) , floor(y*scaleY) );
				}
			} 
		}
		
		public void render1(Graphics g,float scaleX,float scaleY) 
		{
			final float inc = 1f;
			
			float y1=0,y2=0;
			int step = 0;
			do {
			  	/* (x-h)(x-h) = 4p(y-k)
			  	 * 
			  	 * (x-h)(x-h) 
			  	 * ---------- = y-k
			  	 *     4p
			  	 *     
			  	 * (x-h)(x-h) 
			  	 * ---------- + k = y
			  	 *     4p   
			  	 */
				float x1 = v.x+step*inc;
				float x2 = x1 + inc;
				
				y1 = ( ((x1-h)*(x1-h))/(4*p))+k;
				y2 = ( ((x2-h)*(x2-h))/(4*p))+k;
				
				g.drawLine( floor(x1*scaleX) , floor(y1*scaleY) , floor(x2*scaleX) , floor(y2*scaleY) );
				
				x1 = v.x-step*inc;
				x2 = x1 - inc;				
				
				g.drawLine( floor(x1*scaleX) , floor(y1*scaleY) , floor(x2*scaleX) , floor(y2*scaleY) );				
				step++;
			} while ( y1 >= 0 && y2 >= 0);
		}
	}	
	
	protected static enum NodeType { LEAF_NODE , INTERNAL_NODE };
	
	public interface ISiteVisitor {
		
		public void visitSite(SiteNode n);
	}
	
	protected static final class Line {
		
		private final Point2D.Float p1;
		private final Point2D.Float p2;
		
		private final float a;
		private final float b;
		
		public Line(Point2D.Float p1,Point2D.Float p2) 
		{
			this.p1 = p1;
			this.p2 = p2;

			if ( ! isVertical() && ! isHorizontal() ) {
				float dx1 = p2.x - p1.x;
				float dy1 = p2.y - p1.y ;
				a = dy1/dx1;
				b = p1.y - a*p1.x;
			} else {
				a=b=0;
			}
		}
		
		public Point2D.Float getIntersectionPoint(Line other) {
			
			if ( isVertical() && other.isVertical() ) {
				return null; // either lines are parallel or intersect fully
			} 
			if ( isVertical() && other.isHorizontal() ) {
				return new Point2D.Float( p1.x , p2.y );
			} 
			if ( isHorizontal() && other.isVertical() ) {
				return new Point2D.Float( other.p1.x , this.p1.y );
			} 
			if ( isHorizontal() && other.isHorizontal() ) {
				return null; // either lines are parallel or intersect fully
			}
			// regular case
			float a1 = a;
			float b1 = b;

			float a2 = other.a;
			float b2 = other.b;
			
			float x = ( b2 - b1) / ( a1 - a2);
			float y = a1*x+b1;
			return new Point2D.Float(x,y);
		}
		
		public boolean isVertical() {
			return p1.x == p2.x;
		}
		
		public boolean isHorizontal() {
			return p1.y == p2.y;
		}		
	}	
	
	protected static abstract class TreeNode {

		private TreeNode parent;
		private TreeNode leftChild;
		private TreeNode rightChild;
		
		public TreeNode() {
		}
		
		public void visitSites(ISiteVisitor v) {
			
			if ( getType() == NodeType.INTERNAL_NODE ) {
				if ( leftChild != null ) {
					leftChild.visitSites( v );
				} 
				if ( rightChild != null ){
					rightChild.visitSites(v);
				}
			} else {
				v.visitSite( (SiteNode) this);
			}
		}
		
		public final void replaceWith(TreeNode newNode) 
		{
			if ( getParent() == null ) {
				throw new UnsupportedOperationException("Cannot replace root node");
			}
			if ( getParent().getLeftChild() == this ) {
				getParent().setLeftChild( newNode );
			} else {
				getParent().setRightChild( newNode );
			}
		}
		
		public void replaceChild(TreeNode oldChild,TreeNode newChild) {
			if ( leftChild == oldChild ) {
				leftChild = newChild;
			} else if ( rightChild == oldChild ) {
				rightChild = newChild;
			} else {
				throw new IllegalArgumentException("Node "+oldChild+" is no child of "+this);
			}
			newChild.setParent( this );
		}
		
		public abstract NodeType getType();
		
		public final TreeNode getParent() { return parent; }

		public final void setParent(TreeNode parent) { this.parent = parent; }

		public final TreeNode getLeftChild() { return leftChild; }

		public final void setLeftChild(TreeNode leftChild) { 
			this.leftChild = leftChild;
			leftChild.setParent( this );
		}

		public final TreeNode getRightChild() { return rightChild; }

		public final void setRightChild(TreeNode rightChild) 
		{ 
			this.rightChild = rightChild;
			rightChild.setParent(this);
		}
		
		public final SiteNode findArc(Site s) 
		{
			if ( getType() == NodeType.INTERNAL_NODE ) 
			{
					InternalNode node = (InternalNode) this;
					Float breakPoint1 = node.getBreakPointX();
					if ( s.point.x < breakPoint1 ) 
					{
						// visit left subtree ( newX < x )
						return node.getLeftChild().findArc( s );
					} 
					// visit right subtree ( newX < x )
					return node.getRightChild().findArc( s );
			} else {
				return (SiteNode) this;
			}
		}
	}
	
	protected static final class InternalNode extends TreeNode 
	{
		public SiteNode leftSite;
		public SiteNode rightSite;
		
		private InternalNode(SiteNode leftSite, SiteNode rightSite) {
			this.leftSite = leftSite;
			this.rightSite = rightSite;
		}
		
		public Float getBreakPointX() {
			return leftSite.getBreakPoint( rightSite );
		}

		@Override
		public NodeType getType() { return NodeType.INTERNAL_NODE; }
	}
	
	protected static final class SiteNode extends TreeNode 
	{
		public Site site;
		public CircleEvent circleEvent;
		
		private SiteNode(Site site) {
			this.site=site;
		}
		
		public Float getBreakPoint(SiteNode rightNeighbor) {
			
			float[] intersect = site.parabola.intersect( rightNeighbor.site.parabola );
			if ( intersect == null ) {
				return null;
			}
			if ( intersect.length == 1 ) {
				return intersect[0];
			}
			return Math.max( intersect[0] , intersect[1] );
		}		
		
		public SiteNode getLeftNeighbor() {
			
			if ( getParent() == null ) {
				return null;
			}
			InternalNode parent = (InternalNode ) getParent();
			if ( parent.getRightChild() == this ) 
			{
				TreeNode node = parent.getLeftChild();
				if ( node.getType() == NodeType.LEAF_NODE) {
					return (SiteNode) node;
				}
				node = node.getRightChild();
				while( node.getType() != NodeType.LEAF_NODE ) {
					node = node.getRightChild();
				}
				return (SiteNode) node;
			}
			
			TreeNode node = parent.getParent();
			if ( node == null ) {
				return null;
			}
			node = node.getRightChild();
			while( node.getType() != NodeType.LEAF_NODE ) {
				node = node.getRightChild();
			}
			return (SiteNode) node;			
		}
		
		public SiteNode getRightNeighbor() {
			
			if ( getParent() == null ) {
				return null;
			}
			InternalNode parent = (InternalNode ) getParent();
			if ( parent.getLeftChild() == this ) 
			{
				TreeNode node = parent.getRightChild();
				if ( node.getType() == NodeType.LEAF_NODE) {
					return (SiteNode) node;
				}
				node = node.getLeftChild();
				while( node.getType() != NodeType.LEAF_NODE ) {
					node = node.getLeftChild();
				}
				return (SiteNode) node;
			}
			
			TreeNode node = parent.getParent();
			if ( node == null ) {
				return null;
			}
			node = node.getRightChild();
			while( node.getType() != NodeType.LEAF_NODE ) {
				node = node.getLeftChild();
			}
			return (SiteNode) node;			
		}		
		
		@Override
		public NodeType getType() { return NodeType.LEAF_NODE; }
	}
	
	protected static enum EventType { SITE , CIRCLE };
	
	protected static abstract class Event {
		
		public abstract EventType getType();
		
		public abstract float getYCoordinate();
	}
	
	protected static final class CircleEvent extends Event {

		public final Point2D.Float circleCenter;
		public final float radius;
		public final float radiusSquared;
		public final SiteNode siteNode;
		private final float circleBottomY;
		
		public CircleEvent(java.awt.geom.Point2D.Float circleCenter,float radius,SiteNode site) 
		{
			this.siteNode = site;
			this.circleCenter = circleCenter;
			this.radius = radius;
			this.radiusSquared=radius*radius;
			this.circleBottomY = circleCenter.y+radius;
		}
		
		public boolean contains(Site site) {
			
			float dx = site.point.x - circleCenter.x;
			float dy = site.point.y - circleCenter.y;
			return (dx*dx+dy*dy) < radiusSquared;
		}

		@Override
		public EventType getType() {
			return EventType.CIRCLE;
		}

		@Override
		public float getYCoordinate() {
			return circleBottomY;
		}
	}
	
	protected static final class SiteEvent extends Event 
	{
		public final Site site;
		
		public SiteEvent(Site s) {
			this.site = s;
		}
		
		@Override
		public EventType getType() {
			return EventType.SITE;
		}

		@Override
		public float getYCoordinate() 
		{
			return site.point.y;
		}
	}
	
	protected static final class Site 
	{
		public final Point2D.Float point;
		public final Parabola parabola;
		
		public final List<HalfEdge> edges = new ArrayList<>();

		private Site(Point2D.Float point) {
			this.point = point;
			this.parabola = new Parabola(point);
			this.parabola.calculateCoefficients(point.y);
		}
		
		public void update(float directrixY) {
			parabola.calculateCoefficients(directrixY);
		}
	}
	
	protected static final class HalfEdge 
	{
		public Point2D.Float start;
		public Point2D.Float end;
	}	
	
	private TreeNode beachFront;
	
	private final PriorityQueue<Event> events = new PriorityQueue<Event>(100,new Comparator<Event>() {

		@Override
		public int compare(Event o1, Event o2) {
			return Float.compare(o1.getYCoordinate() , o2.getYCoordinate() );
		}
	});

	private List<SiteNode> sites = new ArrayList<>();
	
	public List<SiteNode> generate(List<Point2D.Float> points) 
	{
		for ( Point2D.Float p : points ) 
		{
			final Site s = new Site(p);
			events.add( new SiteEvent( s ) );
		}
		
		generate();
		
		return sites;
	}
	
	private void generate() {
		
		while ( ! events.isEmpty() ) 
		{
			final Event event = events.poll();
			switch( event.getType() ) 
			{
				case SITE:
					handleSiteEvent((SiteEvent) event);
					break;
				case CIRCLE:
					handleCircleEvent((CircleEvent) event);
					break;
			}
		}
		// TODO: Close unfinished cells
	}
	
	private void handleSiteEvent(SiteEvent event) 
	{
		// update parabolas for breakpoint calculations
		for ( SiteNode n : sites ) {
			n.site.update( event.site.point.y );
		}
		
		final SiteNode newSite = new SiteNode( event.site );
		
		// store site by ascending X coordinates
		boolean inserted = false;
		for ( int i = 1 ; i < sites.size() ; i++ ) 
		{
			final SiteNode previous = sites.get(i-1);
			final SiteNode current = sites.get(i);
			if ( event.site.point.x >= previous.site.point.x && event.site.point.x <= current.site.point.x ) {
				sites.add( i  , newSite );
			}
		}
		
		if ( ! inserted ) {
			sites.add( newSite );
		}
		
		if ( beachFront == null ) 
		{
			// first site, create new node
			beachFront = new SiteNode( event.site );
			return;
		}
		
		if ( beachFront.getType() == NodeType.LEAF_NODE ) { // currently only one site in the tree

			SiteNode existingSite = (SiteNode) beachFront;
			if ( newSite.site.point.x < existingSite.site.point.x ) { // newSite.x < existingSite.x ==> left child node
				
				InternalNode newNode = new InternalNode(newSite,existingSite);
				newNode.setLeftChild( newSite );
				newNode.setRightChild( existingSite );
				beachFront = newNode;
			} else { // newSite.x >= existingSite.x ==> right child node
				InternalNode newNode = new InternalNode(existingSite,newSite);
				newNode.setLeftChild( existingSite );
				newNode.setRightChild( newSite );
				beachFront = newNode;				
			}
			return;
		}
		
		// 1. Determine arc on beach line 
		final SiteNode existing = beachFront.findArc(event.site);
		
		// remember old parent BEFORE overwriting it by adding the existing node to another parent
		final TreeNode parent = existing.getParent();

		// replace 
		final InternalNode newNode1;
		final InternalNode newNode2;
		if ( newSite.site.point.x < existing.site.point.x ) 
		{
			newNode2 = new InternalNode( newSite , existing );
			newNode2.setLeftChild( newSite );
			newNode2.setRightChild( existing );
		} 
		else 
		{
			// insert new site to the right of the existing one
			newNode2 = new InternalNode( existing,newSite );
			newNode2.setLeftChild( existing );
			newNode2.setRightChild( newSite );
		}

		newNode1 = new InternalNode( existing , newSite );
		newNode1.setLeftChild( existing );
		newNode1.setRightChild( newNode2 );
		
		parent.replaceChild( existing , newNode1 );
		
		// check circle events
		checkCircleEvent( newSite );
		checkCircleEvent( newSite.getLeftNeighbor() );
		checkCircleEvent( newSite.getRightNeighbor() );
	}	
	
	private void checkCircleEvent(SiteNode middle) 
	{
		if ( middle == null ) {
			return;
		}
		
		discardCircleEvent(middle);
		
		final SiteNode leftNeighbor = middle.getLeftNeighbor();
		final SiteNode rightNeighbor = middle.getRightNeighbor();
		
		final Line line1 = bisect(leftNeighbor.site.point , middle.site.point );
		final Line line2 = bisect(middle.site.point , rightNeighbor.site.point );
		final Line line3 = bisect(leftNeighbor.site.point , rightNeighbor.site.point );
		
		// calculate intersection points of the three bisectors
		final Point2D.Float i1 = line1.getIntersectionPoint( line2 );
		if ( i1 == null ) 
		{
			return;
		}
		
		final Point2D.Float i2 = line2.getIntersectionPoint( line3 );
		if ( i2 == null ) {
			return;
		}		
		
		final Point2D.Float i3 = line1.getIntersectionPoint( line3 );
		if ( i3 == null ) {
			return;
		}
		
		if ( equals( i1 , i2 ) && equals( i2,i3 ) ) { // => Circle event
			float dx = middle.site.point.x - i1.x;
			float dy = middle.site.point.y - i1.y;
			
			// circle event occurs when sweep line passes through the BOTTOM of an EMPTY circle
			float radius = (float) Math.sqrt( dx*dx + dy*dy );
			final CircleEvent event = new CircleEvent( i1 , radius , middle );
			
			// assert that no other sites are contained in this circle
			if ( checkValid( event ) ) 
			{
				middle.circleEvent = event;
				events.add( event );
				return;
			}
		}
	}
	
	private void discardCircleEvent(SiteNode node) 
	{
		if ( node.circleEvent != null ) {
			events.remove( node.circleEvent );
			node.circleEvent = null;
		}
	}
	
	private boolean checkValid(CircleEvent event) 
	{
		float x1 = event.circleCenter.x - event.radius;
		float x2 = event.circleCenter.x + event.radius;
		for ( SiteNode n : sites ) 
		{
			float siteX = n.site.point.x;
			if ( n.site.point.x > x1 && siteX < x2 ) 
			{
				float siteY = n.site.point.y;
				// calculate squared distance to circle center
				float dx = siteX - event.circleCenter.x;
				float dy = siteY - event.circleCenter.y;
				
				float distanceSquared = dx*dx+dy*dy;
				if ( distanceSquared < event.radiusSquared ) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static boolean equals(Point2D.Float p1,Point2D.Float p2) 
	{
		return equals( p1.x , p2.x , 0.00001 ) && equals( p1.y , p2.y , 0.00001 );
	}
	
	private static boolean equals(float f1,float f2,double epsilon) {
		return Math.abs(f1-f2) <= epsilon;
	}
	
	private Line bisect(Point2D.Float p1,Point2D.Float p2) 
	{
		// calculate point right in the middle of the two points
		final Vector2 v = new Vector2(p2.x - p1.x,p2.y - p1.y).scl(0.5f);
		
		// construct orthogonal vector
		final Vector2 v2 = new Vector2( v.y , -v.x );
		
		final Point2D.Float l1 = new Point2D.Float( p1.x + v.x , p1.y + v.y );
		final Point2D.Float l2 = new Point2D.Float( l1.x + v2.x , l1.y + v2.y );
		
		return new Line(l1,l2);
	}
	
	private void handleCircleEvent(CircleEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	protected static final int floor(float f) {
		return (int) f;
	}	
}