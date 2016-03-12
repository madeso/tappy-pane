package com.madeso.tappy.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.madeso.tappy.HEIGHT
import com.madeso.tappy.TappyPlane
import com.madeso.tappy.WIDTH

object DesktopLauncher {
    @JvmStatic fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.width = WIDTH.toInt() * 2
        config.height = HEIGHT.toInt() * 2
        LwjglApplication(TappyPlane(), config)
    }
}
