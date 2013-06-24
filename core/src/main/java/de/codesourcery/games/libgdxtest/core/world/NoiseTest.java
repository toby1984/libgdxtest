package de.codesourcery.games.libgdxtest.core.world;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
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

public class NoiseTest 
{
    private static final int MAP_SIZE = 512;
    
    private static final boolean ANTI_ALIAS = false;   
    
    private float persistance = 0.3f;
    private int octaves = 3;
    private float tileSize=2f;
    private float groundLevel=0.0f;
    
    private final int[] blackWhiteGradient = TextureUtils.createBlackWhiteGradient();
    private final int[] landscapeGradient = TextureUtils.createLandscapeGradient();
    private int[] colorGradient = landscapeGradient;

    private final int heightMapSize;
    
    // ===========================
    
    private long seed = 0xdeadbeef; // System.currentTimeMillis();
    
    private BufferedImage grass;
    private BufferedImage stones;       
    
    private boolean doBlending = false;
    
    private int tileX = 0;
    private int tileY = 0;    

    private final JFrame frame = new JFrame("test");
    
    private SimplexNoise simplexNoise;
    
    private final ThreadPoolExecutor threadPool;
    
    private final KeyAdapter keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e)
        {
            switch( e.getKeyChar() ) {
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
                    frame.repaint();
                    break;
                case 'a':
                    tileX--;
                    frame.repaint();
                    break;
                case 'd':
                    tileX++;
                    frame.repaint();
                    break;  
                case 'w':
                    tileY--;
                    frame.repaint();
                    break;
                case 's':
                    tileY++;
                    frame.repaint();
                    break;                                  
            } 
        }
    };

    public NoiseTest(int heightMapSize) {
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
        
        final JPanel panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                
                if ( ANTI_ALIAS ) {
                    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
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
                   createBlendedImage( latch, xOffset , yOffset , image1 );
                   createBlendedImage( latch, xOffset+tileSize , yOffset , image2  );
                   createBlendedImage( latch, xOffset , yOffset+tileSize  , image3 );
                   createBlendedImage( latch, xOffset+tileSize , yOffset+tileSize , image4  );
                    
                } else {
                    getImage( latch , xOffset , yOffset , image1  );
                    getImage( latch , xOffset+tileSize , yOffset , image2 );
                    getImage( latch , xOffset , yOffset+tileSize , image3 );
                    getImage( latch , xOffset+tileSize , yOffset+tileSize , image4 );
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

                g.setColor( Color.RED );
                g.drawString( "X = "+xOffset +" / Y = "+yOffset+" / tile size: "+tileSize , 15,15 );
            }
            
            private void createBlendedImage(final CountDownLatch latch,final float x,final float y,final BufferedImage[] image) 
            {
                final Runnable r = new Runnable() {

                    @Override
                    public void run()
                    {
                        try {
                            final float[] noise = createNoise2D( x, y , seed );
                            image[0]=TextureUtils.blendTextures( stones , grass , noise , heightMapSize , 0.0f );
                        } finally {
                            latch.countDown();
                        }
                    }
                };
                threadPool.submit(r);
            }           
            
            private void getImage(final CountDownLatch latch,final float x,final float y,final BufferedImage[] image) 
            {
                final Runnable r = new Runnable() {

                    @Override
                    public void run() 
                    {
                        try {
                        long time = -System.currentTimeMillis();
                        float[] noise2d = createNoise2D(x,y,seed);
                        time += System.currentTimeMillis();
                        System.out.println("Noise generation: "+time+" ms");
                        
                        time = -System.currentTimeMillis();
                        image[0]= TextureUtils.heightMapToImage(noise2d, heightMapSize , colorGradient , groundLevel );
                        time += System.currentTimeMillis();
                        System.out.println("Image generation: "+time+" ms");
                        } finally {
                            latch.countDown();
                        }
                    }
                };
                threadPool.submit( r );
            }
        };

        panel.addKeyListener(keyListener);
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