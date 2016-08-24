package com.madeso.tappy

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
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
val EXTRA_ROCK_PIXELS = 25f
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

enum class State { ALIVE, DYING, DEAD }

class Plane(val game:GameScreen, atlas: TextureAtlas) : Actor() {
    var texture = AnimationDrawable(
            Animation(0.05f,
                    atlas.findRegion("planeGreen1"),
                    atlas.findRegion("planeGreen2"),
                    atlas.findRegion("planeGreen3")
            )
    )
    var accel = Vector2(0f,-GRAVITY)
    var vel = Vector2(0f, 0f)
    var hitbox = Rectangle(0f, 0f, texture.regionWidth, texture.regionHeight)

    init {
        width = texture.regionWidth
        height = texture.regionHeight
        setOrigin(Align.center)
        texture.anim.playMode = Animation.PlayMode.LOOP_PINGPONG
        addAction(rotateDownAction())
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        batch?.draw(texture.image(), x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    override fun drawDebug(shapes: ShapeRenderer?) {
        super.drawDebug(shapes)
        shapes?.rect(x, y, texture.regionWidth, texture.regionHeight)
        shapes?.line(0f,0f, x, y)
    }

    override fun act(delta: Float) {
        super.act(delta)
        when(game.state) {
            State.ALIVE -> actAlive(delta)
            State.DYING -> actDying(delta)
            State.DEAD -> {
                vel = Vector2.Zero
                accel = Vector2.Zero
            }
        }
    }

    private fun actDying(delta: Float) {
        applyGravity(delta)
        belowGroundCheck()
    }

    private fun actAlive(delta: Float) {
        texture.act(delta)
        applyGravity(delta)

        belowGroundCheck()

        val isAboveGame = getY(Align.top) > HEIGHT
        if ( isAboveGame ) {
            setPosition(getX(Align.top), HEIGHT, Align.top)
            game.state = State.DYING
        }
    }

    private fun belowGroundCheck() {
        val isBelowGround = getY(Align.bottom) <= GROUND_LEVEL
        if ( isBelowGround ) {
            setPosition(getX(Align.bottom), GROUND_LEVEL, Align.bottom)
            game.state = State.DEAD
            clearActions()
        }
    }

    private fun applyGravity(delta: Float) {
        vel.add(accel.x * delta, accel.y * delta)
        x += vel.x * delta
        y += vel.y * delta

        hitbox.x = x
        hitbox.y = y
    }

    fun jump() {
        if( game.state == State.ALIVE ) {
            vel.y = JUMP_VEL;

            clearActions()
            addAction(Actions.sequence(Actions.rotateTo(35f, .15f),
                    Actions.delay(.5f),
                    rotateDownAction()))
        }
    }

    private fun rotateDownAction() = Actions.rotateTo(-80f, .3f)
}

class Rock(atlas: TextureAtlas, down: Boolean) : Actor() {
    var texture = atlas.findRegion("rock" + (if(down) "Down" else ""))
    var hitbox = Rectangle(0f, 0f, texture.regionWidth.toFloat(), texture.regionHeight.toFloat())

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

    fun updateHitbox(ax: Float) {
        hitbox.x = ax
        hitbox.y = y
    }
}

// fun GetRandomOpening() = MIN_DISTANCE+EXTRA_ROCK_PIXELS
fun GetRandomOpening() = MathUtils.random(MIN_DISTANCE, HEIGHT - MIN_DISTANCE)

class RockPair(val game:GameScreen, atlas:TextureAtlas) : Group() {
    var top = Rock(atlas, true)
    var bottom = Rock(atlas, false)
    var player = 0f
    var hasPassed = false

    init {
        addActor(top)
        addActor(bottom)

        setupSpacing()
    }

    override fun act(delta: Float) {
        if( game.state == State.ALIVE) {
            x -= SPEED * delta
            if ( x < 0f) {
                x += ROCK_GAP * TOTAL_ROCKS
                hasPassed = false
                setupSpacing()
            }

            if( hasPassed == false && getX(Align.center) < player) {
                hasPassed = true
                game.addScore()
            }

            top.updateHitbox(x)
            bottom.updateHitbox(x)
        }
        super.act(delta)
    }

    fun overlaps(o:Rectangle) = top.hitbox.overlaps(o) || bottom.hitbox.overlaps(o)

    private fun setupSpacing() {
        val gapSize = HALF_GAP_SIZE
        val y = GetRandomOpening()
        bottom.setPosition(0f, y-gapSize, Align.topRight)

        top.setPosition(0f, y + gapSize, Align.bottomRight)
    }
}

class TilingImage(val animate:()->Boolean, count:Int, val speed:Float, createImage:()->Image) : Group() {
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
        if( animate() ) {
            for (img in images) {
                img.x += delta * speed
                if (img.x <= -img.width) img.x += totalwidth
            }
        }
    }
}

class Assets {
    var font = BitmapFont(Gdx.files.internal("future_thin.fnt"), false)
    var background = Texture("background.png")
    var atlas = TextureAtlas("pack.atlas")
}

class BasicScreen(var batch : SpriteBatch) {
    var camera = OrthographicCamera()
    var viewport = StretchViewport(WIDTH, HEIGHT, camera);
    var stage = Stage(viewport, batch)

    var uicamera = OrthographicCamera()
    var uiviewport = StretchViewport(WIDTH, HEIGHT, uicamera);
    var uistage = Stage(uiviewport, batch)

    fun act(delta: Float) {
        stage.act(delta)
        uistage.act(delta)
    }

    fun render() {
        camera.update();
        batch.projectionMatrix = camera.combined;
        stage.draw()
    }

    fun uirender() {
        uicamera.update()
        batch.projectionMatrix = uicamera.combined;
        uistage.draw()
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        uiviewport.update(width, height)
    }

    fun addBackground(assets: Assets, animate: () -> Boolean) {
        d.stage.addActor(
                TilingImage(animate, 2, -SPEED/4) {
                    Image(assets.background)
                }
        )

        d.stage.addActor(
                TilingImage(animate, 2, -SPEED/2f) {
                    Image(assets.atlas.findRegion("groundDirt"))
                }
        )
    }
}

fun ClearScreen() {
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
}

class GameScreen(assets:Assets, var batch : SpriteBatch) : ScreenAdapter() {
    internal var d = BasicScreen(batch)
    internal var scoreLabel = Label("0", Label.LabelStyle(assets.font, Color.WHITE))
    internal var score = 0

    internal var plane = Plane(this, assets.atlas)
    var state = State.ALIVE
    var rocks = Array<RockPair>(TOTAL_ROCKS) {
        x ->
            var rocks = RockPair(this, assets.atlas)
            rocks.setPosition(STARTING_GAP + x*ROCK_GAP, 0f)
            rocks
    }

    init {
        plane.setPosition(WIDTH * 0.25f, HEIGHT/2, Align.center)

        d.addBackground(assets) {
            state == State.ALIVE
        }

        d.stage.addActor(plane)
        for(rock in rocks) {
            d.stage.addActor(rock)
            rock.player = plane.getX(Align.center)
        }

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                plane.jump()
                return true
            }
        }

        scoreLabel.setPosition(WIDTH/2f, HEIGHT * .9f, Align.center)
        d.uistage.addActor(scoreLabel)
    }

    override fun render(delta: Float) {
        d.act(delta)

        if( state == State.ALIVE ) {
            for (rock in rocks) {
                if ( rock.overlaps(plane.hitbox)) {
                    state = State.DYING
                }
            }
        }

        ClearScreen()
        d.render()
        d.uirender()
    }

    override fun resize(width: Int, height: Int) {
        d.resize(width, height)
    }

    fun addScore() {
        score += 1
        scoreLabel.setText(score.toString())
    }
}

class MainMenuScreen(assets:Assets, var batch: SpriteBatch) : ScreenAdapter() {

    var d = BasicScreen(batch)

    init {
        d.addBackground(assets) {
            true
        }
    }

    override fun render(delta: Float) {
        d.act(delta)

        ClearScreen()
        d.render()
        d.uirender()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }
}

class TappyPlane : Game() {
    internal lateinit var batch: SpriteBatch
    internal lateinit var assets : Assets

    override fun create() {
        assets = Assets()
        batch = SpriteBatch()
        setScreen(GameScreen(assets, batch))
    }
}
