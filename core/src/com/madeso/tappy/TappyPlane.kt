package com.madeso.tappy

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
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
val GRAVITY = 10f
val GROUND_LEVEL = 10f

class Plane(atlas: TextureAtlas) : Actor() {
    var texture = atlas.findRegion("planeGreen1")
    var accel = Vector2(0f,-GRAVITY)
    var vel = Vector2(0f, 0f)
    var state = State.ALIVE

    enum class State { ALIVE, DEAD }

    init {
        width = texture.regionWidth.toFloat()
        height = texture.regionHeight.toFloat()
        setOrigin(Align.center)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        batch?.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    override fun act(delta: Float) {
        super.act(delta)
        vel.add(accel.x * delta, accel.y * delta)
        x += vel.x * delta
        y += vel.y * delta

        val isBelowGround = getY(Align.bottom) <= GROUND_LEVEL
        val isAboveGame = getY(Align.top) > HEIGHT

        if ( isBelowGround ) {
            setPosition(getX(Align.bottom), GROUND_LEVEL, Align.bottom)
            state = State.DEAD
        }

        if ( isAboveGame ) {
            setPosition(getX(Align.top), HEIGHT, Align.top)
            state = State.DEAD
        }
    }
}

class GameScreen(var batch : SpriteBatch, atlas: TextureAtlas) : ScreenAdapter() {
    internal var camera = OrthographicCamera()
    internal var viewport = StretchViewport(WIDTH, HEIGHT, camera);
    internal var stage = Stage(viewport, batch)

    init {
        //camera.translate(WIDTH/2,HEIGHT/2)
        var plane = Plane(atlas)
        plane.setPosition(WIDTH/2, HEIGHT/2)

        stage.addActor(Image(Texture("background.png")))
        stage.addActor(Image(atlas.findRegion("groundDirt")))
        stage.addActor(plane)
    }

    override fun render(delta: Float) {
        batch.projectionMatrix = camera.combined;
        stage.act(delta)
        camera.update();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }
}

class TappyPlane : Game() {
    internal lateinit var batch: SpriteBatch
    internal lateinit var atlas : TextureAtlas

    override fun create() {
        batch = SpriteBatch()
        atlas = TextureAtlas("pack.atlas")
        setScreen(GameScreen(batch, atlas))
    }
}
