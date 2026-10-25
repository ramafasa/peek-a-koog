package dev.peekakoog.app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

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
