package de.codesourcery.games.libgdxtest.core.distancefield;

import java.awt.Color;

import com.badlogic.gdx.math.Vector3;

public class PointLight {

	public final Vector3 position=new Vector3();
	public final Vector3 color=new Vector3();
	
	public PointLight(Vector3 position,Color color) {
		this(position,color.getRGB());
	}
	
	public PointLight(Vector3 position,int color) {
		this.position.set(position);
		this.color.set( (color>>16) & 0xff , (color>>8) & 0xff , color & 0xff );
	}
}
