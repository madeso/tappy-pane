package com.madeso.tappy

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.badlogic.gdx.utils.viewport.Viewport

val HEIGHT: Float = 480f;
val WIDTH : Float = (9f/16f) * HEIGHT;
val GRAVITY = 700f
val JUMP_VEL = 400f
val GROUND_LEVEL = 10f
val SPEED = 50f
val STARTING_GAP = WIDTH*1
val TOTAL_ROCKS = 3
val ROCK_GAP = WIDTH * 1.25f

class AnimationDrawable(var anim:Animation) {
    var stateTime = 0f

    var regionWidth = anim.keyFrames[0].regionWidth.toFloat()
    var regionHeight = anim.keyFrames[0].regionHeight.toFloat()

    public fun act(delta:Float) {
        stateTime += delta;
    }

    public fun reset()
    {
        stateTime = 0f
    }

    fun image(): TextureRegion {
        return anim.getKeyFrame(stateTime)
    }
}


class Plane(atlas: TextureAtlas) : Actor() {
    var texture = AnimationDrawable(
            Animation(0.05f,
                    atlas.findRegion("planeGreen1"),
                    atlas.findRegion("planeGreen2"),
                    atlas.findRegion("planeGreen3")
            )
    )
    var accel = Vector2(0f,-GRAVITY)
    var vel = Vector2(0f, 0f)
    var state = State.ALIVE

    enum class State { ALIVE, DEAD }

    init {
        width = texture.regionWidth
        height = texture.regionHeight
        setOrigin(Align.center)
        texture.anim.playMode = Animation.PlayMode.LOOP_PINGPONG
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        batch?.draw(texture.image(), x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    override fun act(delta: Float) {
        super.act(delta)
        when(state) {
            State.ALIVE -> actAlive(delta)
            State.DEAD -> {
                vel = Vector2.Zero
                accel = Vector2.Zero
            }
        }
    }

    private fun actAlive(delta: Float) {
        texture.act(delta)
        vel.add(accel.x * delta, accel.y * delta)
        x += vel.x * delta
        y += vel.y * delta

        val newrotation = MathUtils.clamp(vel.y / JUMP_VEL, -1f, 1f) * 45f
        rotation = MathUtils.lerp(rotation, newrotation, 0.1f)

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

    fun jump() {
        vel.y = JUMP_VEL;
    }
}

class Rock(atlas: TextureAtlas, down: Boolean) : Actor() {
    var texture = atlas.findRegion("rock" + (if(down) "Down" else ""))

    init {
        width = texture.regionWidth.toFloat()
        height = texture.regionHeight.toFloat()
        setOrigin(Align.topLeft)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        batch?.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }
}

fun GetRandomOpening() = MathUtils.random(HEIGHT * .15f, HEIGHT * .85f)

class RockPair(atlas:TextureAtlas) : Group() {
    var top = Rock(atlas, true)
    var bottom = Rock(atlas, false)

    init {
        addActor(top)
        addActor(bottom)

        setup()
    }

    override fun act(delta: Float) {
        x -= SPEED * delta
        if( x < 0f) {
            x += ROCK_GAP * TOTAL_ROCKS
            setup()
        }
        super.act(delta)
    }

    private fun setup() {
        val gapSize = 200f
        val y = GetRandomOpening()
        bottom.setPosition(0f, y-gapSize/2f, Align.topRight)
        top.setPosition(0f, y + gapSize/2f, Align.bottomRight)
    }
}

class GameScreen(var batch : SpriteBatch, atlas: TextureAtlas) : ScreenAdapter() {
    internal var camera = OrthographicCamera()
    internal var viewport = StretchViewport(WIDTH, HEIGHT, camera);
    internal var stage = Stage(viewport, batch)
    internal var plane = Plane(atlas)

    init {
        //camera.translate(WIDTH/2,HEIGHT/2)
        plane.setPosition(WIDTH * 0.25f, HEIGHT/2, Align.center)

        stage.addActor(Image(Texture("background.png")))
        stage.addActor(Image(atlas.findRegion("groundDirt")))
        stage.addActor(plane)
        for(x in 1..TOTAL_ROCKS) {
            var rock = RockPair(atlas)
            rock.setPosition(STARTING_GAP + x*ROCK_GAP, 0f)
            stage.addActor(rock)
        }

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                plane.jump()
                return true
            }
        }
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
