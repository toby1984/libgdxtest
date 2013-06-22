package de.codesourcery.games.libgdxtest.core.world;

import java.util.Random;

public class FractalNoise
{
    public static float[] generateFractalNoise(int mapSize,float[] baseNoise, int octaveCount,float persistance)
    {
        float[][] smoothNoise = new float[octaveCount][]; //an array of 2D arrays containing

        //generate smooth noise
        for (int i = 0; i < octaveCount; i++)
        {
            smoothNoise[i] = generateSmoothNoise(mapSize,baseNoise, i);
        }

        float[] fractalNoise = new float[mapSize*mapSize];
        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;

        //blend noise together
        for (int octave = octaveCount - 1; octave >= 0; octave--)
        {
            amplitude *= persistance;
            totalAmplitude += amplitude;

            for (int i = 0; i < mapSize; i++)
            {
                for (int j = 0; j < mapSize; j++)
                {
                    fractalNoise[i+j*mapSize] += smoothNoise[octave][i+j*mapSize] * amplitude;
                }
            }
        }

        //normalisation
        for (int i = 0; i < mapSize*mapSize; i++)
        {
        	fractalNoise[i] /= totalAmplitude;
        }

        return fractalNoise;
    }    

    public static float[][] generateWhiteNoise(int width, int height,long seed)
    {
        final Random rnd = new Random(seed);
        float[][] noise = createEmptyArray(width,height);

        for (int i = 0; i < width ; i++)
        {
            for (int j = 0; j < height ; j++)
            {
                noise[i][j] = rnd.nextFloat();
            }
        }
        return noise;
    }  

    public static float[][] createEmptyArray(int width,int height) {
        float[][] noise = new float[width][];

        for(int i = 0 ; i < width ; i++ ) {
            noise[i] = new float[height];
        }
        return noise;
    }
    
    public static float[][][] createEmptyArray(int width,int height,int depth) {
        float[][][] noise = new float[width][][];

        for(int i = 0 ; i < width ; i++ ) {
            noise[i] = new float[height][];
            for ( int j = 0 ; j < height ; j++ ) {
                noise[i][j] = new float[ depth ];
            }
        }
        return noise;
    }    

    private static float[] generateSmoothNoise(int mapSize,float[] baseNoise, int octave)
    {
        float[] smoothNoise = new float[mapSize*mapSize];

        int samplePeriod = 1 << octave; // calculates 2 ^ k
        float sampleFrequency = 1.0f / samplePeriod;

        for (int i = 0; i < mapSize; i++)
        {
            //calculate the horizontal sampling indices
            int sample_i0 = (i / samplePeriod) * samplePeriod;
            int sample_i1 = (sample_i0 + samplePeriod) % mapSize; //wrap around
            float horizontal_blend = (i - sample_i0) * sampleFrequency;

            for (int j = 0; j < mapSize; j++)
            {
                //calculate the vertical sampling indices
                int sample_j0 = (j / samplePeriod) * samplePeriod;
                int sample_j1 = (sample_j0 + samplePeriod) % mapSize; //wrap around
                float vertical_blend = (j - sample_j0) * sampleFrequency;

                //blend the top two corners
                float sample1 = baseNoise[sample_i0 + sample_j0*mapSize];
				float sample2 = baseNoise[sample_i1 + sample_j0*mapSize];
				float sample3 = baseNoise[sample_i0 + sample_j1*mapSize];
				float sample4 = baseNoise[sample_i1 + sample_j1*mapSize];
                
//				float top = interpolate(sample1,sample2, horizontal_blend);
//                //blend the bottom two corners

//				float bottom = interpolate(sample3,sample4, horizontal_blend);
//                float sum = interpolate(top, bottom, vertical_blend); //final blend
                
				smoothNoise[i+j*mapSize] = sample1;
            }
        }

        return smoothNoise;
    }    

    private static float interpolate( double y1,double y2, double mu)
         {
            double mu2 = (1-Math.cos(mu*Math.PI))/2;
            return (float) (y1*(1-mu2)+y2*mu2);
         }    
}
