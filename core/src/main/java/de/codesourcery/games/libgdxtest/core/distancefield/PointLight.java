package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;

import com.badlogic.gdx.math.Vector3;

public class PointLight {

	public final Vector3 position=new Vector3();
	public final int color;
	
	public PointLight(Vector3 position,Color color) {
		this(position,color.getRGB());
	}
	
	public PointLight(Vector3 position,int color) {
		this.position.set(position);
		this.color = color;
	}
}
