package com.madeso.tappy

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.badlogic.gdx.utils.viewport.Viewport

val HEIGHT: Float = 480f;
val WIDTH : Float = (9f/16f) * HEIGHT;

class Plane(atlas: TextureAtlas) : Actor() {
    var texture = atlas.findRegion("planeGreen1")

    init {
        width = texture.regionWidth.toFloat()
        height = texture.regionHeight.toFloat()
        setOrigin(Align.center)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        batch?.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }
}

class TappyPlane : ApplicationAdapter() {
    internal lateinit var batch: SpriteBatch
    internal lateinit var camera : OrthographicCamera
    internal lateinit var viewport : Viewport;

    internal lateinit var atlas : TextureAtlas
    internal lateinit var stage : Stage

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera();
        viewport = StretchViewport(WIDTH, HEIGHT, camera);
        stage = Stage(viewport, batch)
        atlas = TextureAtlas("pack.atlas")

        //camera.translate(WIDTH/2,HEIGHT/2)
        var plane = Plane(atlas)
        plane.setPosition(WIDTH/2, HEIGHT/2)
        stage.addActor(Image(Texture("background.png")))
        stage.addActor(plane)
    }

    override fun render() {
        batch.projectionMatrix = camera.combined;
        stage.act()
        camera.update();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }
}
