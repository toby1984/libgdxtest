package de.codesourcery.games.libgdxtest.java;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import de.codesourcery.games.libgdxtest.core.Main;

public class MainDesktop 
{
	public static void main (String[] args) 
	{
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.useGL20 = false;
		new LwjglApplication(new Main(), config);
	}
}
