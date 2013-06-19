package de.codesourcery.games.libgdxtest.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class Main extends Game
{
	@Override
	public void create () 
	{
	    Gdx.graphics.setVSync(true);
	    setScreen( new GameScreen() );
	}

	@Override
	public void render () 
	{
	    getScreen().render( Gdx.graphics.getDeltaTime() );
	}

	@Override
	public void dispose () {
	}
}
