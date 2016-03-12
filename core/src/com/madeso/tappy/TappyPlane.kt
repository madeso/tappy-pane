package com.madeso.tappy

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.badlogic.gdx.utils.viewport.Viewport

val HEIGHT: Float = 480f;
val WIDTH : Float = (9f/16f) * HEIGHT;

class TappyPlane : ApplicationAdapter() {
    internal lateinit var batch: SpriteBatch
    internal lateinit var camera : OrthographicCamera
    internal lateinit var viewport : Viewport;

    internal lateinit var img: Texture

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera();
        viewport = StretchViewport(WIDTH, HEIGHT, camera);
        img = Texture("background.png")
        camera.translate(WIDTH/2,HEIGHT/2)
    }

    override fun render() {
        camera.update();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.begin()
        batch.projectionMatrix = camera.combined;
        batch.draw(img, 0f, 0f)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }
}
