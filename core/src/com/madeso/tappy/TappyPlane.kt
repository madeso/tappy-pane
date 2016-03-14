package com.madeso.tappy

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
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
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.StretchViewport

val HEIGHT: Float = 480f;
val WIDTH : Float = (9f/16f) * HEIGHT;
val GRAVITY = 700f
val JUMP_VEL = 350f
val GROUND_LEVEL = 10f
val SPEED = 120f
val STARTING_GAP = WIDTH*1
val TOTAL_ROCKS = 3
val ROCK_GAP = WIDTH * 1.25f
val HALF_GAP_SIZE = 100f
// the minimum amount of pixels to display when drawing a rock
val EXTRA_ROCK_PIXELS = 15f
val MIN_DISTANCE = HALF_GAP_SIZE + EXTRA_ROCK_PIXELS

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
        // height = texture.regionHeight.toFloat()
        // the height needs to be a little larger than the
        // actual pixels to accommodate for game logic
        height = 239f + 30f
        setOrigin(Align.topLeft)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        batch?.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }
}

// fun GetRandomOpening() = MIN_DISTANCE+EXTRA_ROCK_PIXELS
fun GetRandomOpening() = MathUtils.random(MIN_DISTANCE, HEIGHT - MIN_DISTANCE)

class RockPair(atlas:TextureAtlas) : Group() {
    var top = Rock(atlas, true)
    var bottom = Rock(atlas, false)

    init {
        addActor(top)
        addActor(bottom)

        setupSpacing()
    }

    override fun act(delta: Float) {
        x -= SPEED * delta
        if( x < 0f) {
            x += ROCK_GAP * TOTAL_ROCKS
            setupSpacing()
        }
        super.act(delta)
    }

    private fun setupSpacing() {
        val gapSize = HALF_GAP_SIZE
        val y = GetRandomOpening()
        bottom.setPosition(0f, y-gapSize, Align.topRight)
        top.setPosition(0f, y + gapSize, Align.bottomRight)
    }
}

class TilingImage(count:Int, val speed:Float, createImage:()->Image) : Group() {
    var images = Array<Image>(count, {i -> createImage() })
    val totalwidth : Float

    init {
        var d = 0f
        for(img in images) {
            img.setAlign(Align.bottomLeft)
            img.setScaling(Scaling.stretch)
            img.x = d
            img.y = 0f
            d += img.width
            addActor(img)
        }
        totalwidth = d
    }

    override fun act(delta: Float) {
        for(img in images) {
            img.x += delta * speed
            if (img.x <= -img.width) img.x += totalwidth
        }
    }
}

class GameScreen(var batch : SpriteBatch, atlas: TextureAtlas) : ScreenAdapter() {
    internal var camera = OrthographicCamera()
    internal var viewport = StretchViewport(WIDTH, HEIGHT, camera);
    internal var stage = Stage(viewport, batch)
    internal var plane = Plane(atlas)

    init {
        plane.setPosition(WIDTH * 0.25f, HEIGHT/2, Align.center)

        stage.addActor(
                TilingImage(2, -SPEED/4) {
                    Image(Texture("background.png"))
                }
        )
        stage.addActor(
                TilingImage(2, -SPEED) {
                    Image(atlas.findRegion("groundDirt"))
                }
        )

        stage.addActor(plane)
        for(x in 1..TOTAL_ROCKS) {
            var rocks = RockPair(atlas)
            rocks.setPosition(STARTING_GAP + x*ROCK_GAP, 0f)
            stage.addActor(rocks)
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
