package dev.peekakoog.app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM

/** Kodee by JetBrains s.r.o. is licensed under CC BY 4.0. */
enum class KodeeMood(file: String) {
    WELCOMING("kodee-welcoming.png"),
    NAUGHTY("kodee-naughty.png"),
    SURPRISED("kodee-surprised.png"),
    EXCITED("kodee-excited.png");

    val image: ImageBitmap by lazy {
        val bytes = KodeeMood::class.java.getResourceAsStream("/kodee/$file")!!.use { it.readBytes() }
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
}

/** The game's logo: Kodee covering its eyes. Adapted from the official Kodee artwork (see the SVG for details). */
object KodeeLogo {
    val image: ImageBitmap by lazy {
        val bytes = KodeeLogo::class.java.getResourceAsStream("/kodee/kodee-peekaboo.svg")!!.use { it.readBytes() }
        val svg = SVGDOM(Data.makeFromBytes(bytes))
        val size = 512
        svg.setContainerSize(size.toFloat(), size.toFloat())
        val surface = Surface.makeRasterN32Premul(size, size)
        svg.render(surface.canvas)
        surface.makeImageSnapshot().toComposeImageBitmap()
    }
}
