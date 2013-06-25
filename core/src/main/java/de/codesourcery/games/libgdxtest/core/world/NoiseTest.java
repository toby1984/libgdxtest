package de.codesourcery.games.libgdxtest.core.world;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.codesourcery.games.libgdxtest.core.world.NavMeshGenerator.NavMesh;
import de.codesourcery.games.libgdxtest.core.world.PathFinder.Path;
import de.codesourcery.games.libgdxtest.core.world.PathFinder.PathNode;

public class NoiseTest 
{
    private static final int MAP_SIZE = 256; 

    private static final boolean ANTI_ALIAS = false;   

    private float persistance = 32f;
    private int octaves = 3;
    private float tileSize=2f;
    private float groundLevel=0.0f;
    private float walkableGroundLevel=0.0f;

    private final int[] blackWhiteGradient = TextureUtils.createBlackWhiteGradient();
    private final int[] landscapeGradient = TextureUtils.createLandscapeGradient();
    private int[] colorGradient = landscapeGradient;

    private final int heightMapSize;

    private float[][] noiseMaps = new float[4][];

    // ===========================

    private long seed = 0xdeadbeef; // System.currentTimeMillis();

    private BufferedImage grass;
    private BufferedImage stones;       

    private boolean renderNavHeightmap = false;
    private boolean doBlending = false;

    private int tileX = 0;
    private int tileY = 0;    

    private final JFrame frame = new JFrame("test");
    private JPanel panel;

    private SimplexNoise simplexNoise;

    private final ThreadPoolExecutor threadPool;

    // path finding test
    private final int navGridTileSize = 4;
    private List<Point> points = new ArrayList<>();
    private Path path;
    private NavMesh navMesh;

    private final MouseAdapter mouseAdapter = new MouseAdapter() 
    {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) 
        {
            points.add( new Point(e.getX(),e.getY() ) );
            if ( points.size() > 2 ) {
                points.remove(0);
            }
            path = null;
            navMesh = null;
            frame.repaint();
        }
        
        @Override
        public void mouseMoved(java.awt.event.MouseEvent e) {
            panel.setToolTipText( "Height: "+getHeight( e.getX() , e.getY() ) );
        }
        
        private float getHeight(int scrX,int scrY) {
            
            float scaleX = panel.getWidth() / (MAP_SIZE*2.0f);
            float scaleY = panel.getHeight() / (MAP_SIZE*2.0f);
            
            int x = (int) (scrX / scaleX);
            int y = (int) (scrY / scaleY);
            return readHeightValue( x , y );
        }
    };
    
    private final KeyAdapter keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e)
        {
            switch( e.getKeyChar() ) 
            {
                case 'g':
                    if ( colorGradient == blackWhiteGradient ) {
                        colorGradient = landscapeGradient;
                    } else {
                        colorGradient = blackWhiteGradient;
                    }
                    frame.repaint();
                    break;
                case 'b':
                    doBlending = !doBlending;
                    frame.repaint();
                    break;
                case ' ':
                    seed = System.currentTimeMillis();
                    path = null;
                    navMesh = null;
                    frame.repaint();
                    break;
                case 'n':
                    renderNavHeightmap = ! renderNavHeightmap;
                    frame.repaint();
                    break;
                case 'a':
                    tileX--;
                    path = null;
                    navMesh = null;                    
                    frame.repaint();
                    break;
                case 'd':
                    tileX++;
                    path = null;
                    navMesh = null;                    
                    frame.repaint();
                    break;  
                case 'w':
                    tileY--;
                    path = null;
                    navMesh = null;                    
                    frame.repaint();
                    break;
                case 'p':
                    if ( points.size() == 2 ) 
                    {
                        findPath(points.get(0),points.get(1) );
                        frame.repaint();
                    } else {
                        System.out.println("Select 2 points first");
                    }
                    break;
                case 's':
                    path = null;
                    navMesh = null;                      
                    tileY++;
                    frame.repaint();
                    break;                                  
            } 
        }
    };

    private float[] joinNoiseMaps() 
    {
        final int combinedSize=heightMapSize*2;
        float[] combined = new float[ combinedSize*combinedSize ];
        int ptr = 0;
        for ( int y = 0 ; y < combinedSize ; y++ ) 
        {
            for ( int x = 0 ; x < combinedSize ; x++ ) 
            {
                combined[ptr++] = readHeightValue(x,y);
            }
        }
        return combined;
    }
    
    private float readHeightValue(int x,int y) 
    {
        int mapIndex;
        if ( x < heightMapSize && y < heightMapSize )  
        {
            mapIndex=0;
        } else if ( x >= heightMapSize && y < heightMapSize )  {
            mapIndex=1;
        } else if ( x < heightMapSize && y >= heightMapSize )  {
            mapIndex=2;
        } else {
            mapIndex=3;
        }
        return noiseMaps[mapIndex][(x%heightMapSize)+(y%heightMapSize)*heightMapSize];
    }
    
    private void findPath(Point src,Point dst) 
    {
        // join height maps
        final int combinedSize=heightMapSize*2;
        float[] combined = joinNoiseMaps();
        
        float scaleX = 2*MAP_SIZE / (float) navGridTileSize;
        
        float cellWidth = panel.getWidth() / scaleX;
        float cellHeight = panel.getHeight() / scaleX;        

        int srcX = (int) Math.floor(src.x / cellWidth);
        int srcY = (int) Math.floor(src.y / cellHeight);
        int dstX = (int) Math.floor(dst.x / cellWidth);
        int dstY = (int) Math.floor(dst.y / cellHeight);        

        System.out.println("Generting nav mesh ...");
        navMesh = new NavMeshGenerator().generateGrid( combined , combinedSize , navGridTileSize );
        PathFinder finder = new PathFinder( navMesh );
        System.out.print("Finding path "+srcX+" / "+srcY+" -> "+dstX+" / "+dstY);
        path = finder.findPath( srcX , srcY , dstX , dstY , walkableGroundLevel );
        System.out.println( path == null ? "No path" : "Got path" );
    }

    public NoiseTest(int heightMapSize) 
    {
        this.heightMapSize = heightMapSize;

        final int poolSize = Runtime.getRuntime().availableProcessors()+1;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) 
            {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        };

        threadPool = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.MINUTES, workQueue, threadFactory, new CallerRunsPolicy() );
    }

    public static void main(String[] args)
    {
        new NoiseTest(MAP_SIZE).test();
    }

    protected void test() 
    {
        stones = TextureUtils.createTexture( new File("/home/tgierke/workspace/libgdxtest/assets/stones.png") , 1024 , 1024 );              
        grass = TextureUtils.createTexture( new File("/home/tgierke/workspace/libgdxtest/assets/grass.png") , 1024 , 1024 );

        panel = new JPanel() 
        {
            @Override
            protected void paintComponent(Graphics graphics)
            {
                Graphics2D g = (Graphics2D) graphics;
                super.paintComponent(g);

                if ( ANTI_ALIAS ) {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                }

                // 2x2 images
                float xOffset= tileX*tileSize*0.1f;
                float yOffset= tileY*tileSize*0.1f;
                
                int halfWidth = getWidth() / 2;
                int halfHeight = getHeight() / 2;

                BufferedImage[] image1={null};
                BufferedImage[] image2={null};
                BufferedImage[] image3={null};
                BufferedImage[] image4={null};      
                CountDownLatch latch = new CountDownLatch(4);
                if ( doBlending ) 
                {
                    createBlendedImage( latch, xOffset , yOffset , image1 ,0);
                    createBlendedImage( latch, xOffset+tileSize , yOffset , image2 ,1 );
                    createBlendedImage( latch, xOffset , yOffset+tileSize  , image3 ,2 );
                    createBlendedImage( latch, xOffset+tileSize , yOffset+tileSize , image4 , 3 );

                } 
                else 
                {
                    getImage( latch , xOffset , yOffset , image1 ,0 );
                    getImage( latch , xOffset+tileSize , yOffset , image2 , 1 );
                    getImage( latch , xOffset , yOffset+tileSize , image3 , 2 );
                    getImage( latch , xOffset+tileSize , yOffset+tileSize , image4 , 3 );
                }
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }                
                g.drawImage( image1[0] ,0,0, halfWidth , halfHeight , null );
                g.drawImage( image2[0] ,halfWidth,0, halfWidth , halfHeight , null ); 
                g.drawImage( image3[0] ,0,halfHeight, halfWidth , halfHeight , null ); 
                g.drawImage( image4[0] ,halfWidth,halfHeight, halfWidth , halfHeight , null ); 

                if ( navMesh != null ) 
                {
                    renderNavMesh(g);
                }
                
                g.setColor( Color.RED );
                g.drawString( "X = "+xOffset +" / Y = "+yOffset+" / tile size: "+tileSize , 15,15 );

                
                if ( points.size() > 0 ) {
                    drawCross( points.get(0) , g );
                }
                if ( points.size() > 1 ) {
                    drawCross( points.get(1) , g );
                }        

                if ( path != null ) 
                {
                    PathNode previous = null;
                    for ( PathNode n : path.path ) 
                    {
                        if ( previous != null ) {
                            drawLine(previous,n,g);
                        }
                        previous = n; 
                    }
                }
            }

            private void renderNavMesh(Graphics2D g)
            {
                float cellWidth = getWidth() / (float) navMesh.width;
                float cellHeight = getHeight() / (float) navMesh.width;
                for ( int y = 0 ; y < navMesh.width ; y++ ) 
                {
                    for ( int x = 0 ; x < navMesh.width ; x++ ) 
                    {
                        float x1 = (int) (x*cellWidth);
                        float y1 = (int) (y*cellHeight);
                        
                        if ( renderNavHeightmap ) 
                        {
                            final float height = navMesh.height[ x+y*navMesh.width ];
                            final int index = (int) (height*255.0f);
//                            System.out.println("Rendering "+x+" / "+y+" => "+index);
                            g.setColor( new Color( blackWhiteGradient[ index & 0xff ] | 0xff000000 ) );
                            g.fillRect( round(x1) , round(y1) , round(cellWidth) , round(cellHeight) );
                        }
                        
                      g.setColor( Color.WHITE );
                      g.drawRect( round(x1) , round(y1) , round(cellWidth) , round(cellHeight) );
                    }
                }
            }
            
            private int round(float x) {
                return Math.round(x);
            }

            private void drawLine(PathNode n1,PathNode n2,Graphics2D g) 
            {
                float scaleX = 2*MAP_SIZE / (float) navGridTileSize;
                
                float cellWidth = getWidth() / scaleX;
                float cellHeight = getHeight() / scaleX;
                
                float p1x = (cellWidth/2)  + n1.x * cellWidth;
                float p1y = (cellHeight/2) + n1.y * cellHeight;
                float p2x = (cellWidth/2)  + n2.x * cellWidth;
                float p2y = (cellHeight/2) + n2.y * cellHeight;

                g.setColor(Color.GREEN);
                g.setStroke( new BasicStroke(1.0f) );                
                g.drawLine( round(p1x),round(p1y),round(p2x),round(p2y));
                
                g.fillRect( round(p1x) - 2 , round(p1y) - 2 , 4 ,4 );
                g.fillRect( round(p2x) - 2 , round(p2y) - 2 , 4 ,4 );                
            }

            private void drawCross(Point p,Graphics2D g) 
            {
                g.setColor(Color.MAGENTA);
                g.setStroke( new BasicStroke(3.0f) );

                final int width = 7;
                final int height = 7;

                g.drawLine( p.x , p.y - height , p.x , p.y + height );
                g.drawLine( p.x-width , p.y  , p.x+width , p.y );
            }

            private void createBlendedImage(final CountDownLatch latch,final float x,final float y,final BufferedImage[] image,final int mapIndex) 
            {
                final Runnable r = new Runnable() {

                    @Override
                    public void run()
                    {
                        try 
                        {
                            final float[] noise = createNoise2D( x, y , seed );
                            noiseMaps[mapIndex]=noise;
                            image[0]=TextureUtils.blendTextures( stones , grass , noise , heightMapSize , 0.0f );
                        } 
                        finally 
                        {
                            latch.countDown();
                        }
                    }
                };
                threadPool.submit(r);
            }           

            private void getImage(final CountDownLatch latch,final float x,final float y,final BufferedImage[] image,final int mapIndex) 
            {
                final Runnable r = new Runnable() {

                    @Override
                    public void run() 
                    {
                        try 
                        {
                            long time = -System.currentTimeMillis();
                            float[] noise2d = createNoise2D(x,y,seed);
                            noiseMaps[mapIndex] = noise2d;
                            time += System.currentTimeMillis();
//                            System.out.println("Noise generation: "+time+" ms");

                            time = -System.currentTimeMillis();
                            image[0]= TextureUtils.heightMapToImage(noise2d, heightMapSize , colorGradient , groundLevel );
                            time += System.currentTimeMillis();
//                            System.out.println("Image generation: "+time+" ms");
                        } 
                        finally 
                        {
                            latch.countDown();
                        }
                    }
                };
                threadPool.submit( r );
            }
        };

        panel.addKeyListener(keyListener);
        panel.addMouseListener(mouseAdapter);
        panel.addMouseMotionListener(mouseAdapter);
        
        panel.setFocusable(true);
        panel.setPreferredSize(new Dimension(heightMapSize , heightMapSize ));

        JPanel controlPanel = createControlPanel();

        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( panel , BorderLayout.CENTER );
        frame.getContentPane().add( controlPanel , BorderLayout.NORTH );
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.addKeyListener( keyListener );
        frame.setVisible( true );
        panel.requestFocus();       
    }

    private JPanel createControlPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        // octave control
        final JTextField octave = new JTextField( Integer.toString( octaves ) );
        octave.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                NoiseTest.this.octaves = Integer.parseInt( octave.getText() );
                frame.repaint();
            }
        });
        octave.setColumns(5);
        addTextField(panel,0,"Octave",octave);

        // persistance control
        final JTextField persistance = new JTextField( Float.toString( NoiseTest.this.persistance ) );
        persistance.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                NoiseTest.this.persistance = Float.parseFloat( persistance.getText() );
                frame.repaint();
            }
        });
        persistance.setColumns(5);
        addTextField(panel,1,"persistance",persistance);     

        // tile size (zoom)
        final JTextField tileSize = new JTextField( Float.toString( NoiseTest.this.tileSize ) );
        tileSize.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                NoiseTest.this.tileSize = Float.parseFloat( tileSize.getText() );
                frame.repaint();
            }
        });
        tileSize.setColumns(5);
        addTextField(panel,2,"Zoom",tileSize);         

        // ground level
        final JTextField groundLevel = new JTextField( Float.toString( NoiseTest.this.groundLevel ) );
        groundLevel.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                NoiseTest.this.groundLevel = Float.parseFloat( groundLevel.getText() );
                frame.repaint();
            }
        });
        groundLevel.setColumns(5);
        addTextField(panel,3,"Ground level",groundLevel);    
        
        // walkable ground level
        final JTextField wGroundLevel = new JTextField( Float.toString( NoiseTest.this.walkableGroundLevel ) );
        wGroundLevel.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                NoiseTest.this.walkableGroundLevel = Float.parseFloat( wGroundLevel.getText() );
                frame.repaint();
            }
        });
        wGroundLevel.setColumns(5);
        addTextField(panel,4,"Walkable ground level",wGroundLevel);         

        return panel;
    }

    private void addTextField(JPanel panel,int x , String label, JTextField textfield) 
    {
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.gridx=x*2;
        cnstrs.gridy=0;        
        cnstrs.weightx=0.0f;
        cnstrs.weighty=0.0f;
        cnstrs.fill=GridBagConstraints.NONE;
        panel.add( new JLabel( label )  , cnstrs );

        cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.NONE;
        cnstrs.gridx=x*2+1;
        cnstrs.gridy=0;
        cnstrs.weightx=1.0f;
        cnstrs.weighty=0.0f;
        cnstrs.fill=GridBagConstraints.NONE;
        panel.add( textfield , cnstrs );        
    }

    private float[] createNoise2D(float x,float y,long seed) 
    {
        if ( simplexNoise == null || this.seed != seed ) {
            simplexNoise = new SimplexNoise(seed);
        }
        return simplexNoise.createHeightMap( x ,y , heightMapSize , tileSize , octaves , persistance );
    }
}