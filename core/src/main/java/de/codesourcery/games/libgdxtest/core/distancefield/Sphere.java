package de.codesourcery.games.libgdxtest.core.distancefield;

import com.badlogic.gdx.math.Vector3;

public final class Sphere extends SceneObject {

	private final float radius;
	public Sphere(Vector3 center,final float radius) {
		super(center);
		if ( radius < 0 ) {
			throw new IllegalArgumentException("radius must not be >= 0");
		}
		this.radius=radius;
	}
	
	@Override
	protected float _distance(float px, float py, float pz) {
		return (float) Math.sqrt( px*px +py*py + pz*pz ) - radius;
	}
}