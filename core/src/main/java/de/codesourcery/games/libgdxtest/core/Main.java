package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class Main extends Game
{
	@Override
	public void create () 
	{
	    setScreen( new GameScreen() );
	}

	@Override
	public void render () 
	{
	    float delta = Gdx.graphics.getDeltaTime();
	    float deltaInMillis = delta * 1000;
	    if ( deltaInMillis > 50 ) {
	        System.out.println("Slow: "+deltaInMillis+" ms");
	    }
	    getScreen().render( 1/60.0f );
	}

	@Override
	public void dispose () {
	}
}
