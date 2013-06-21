package de.codesourcery.games.libgdxtest.core.world;

public interface INoiseGenerator
{
    public float[] createNoise2D(int mapSize,long seed);
}
