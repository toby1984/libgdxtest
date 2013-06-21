package de.codesourcery.games.libgdxtest.core.world;

public interface INoiseGenerator
{
    public byte[] createNoise2D(int width,int height,long seed);
}
