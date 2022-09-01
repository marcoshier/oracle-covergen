import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.ffmpeg.ScreenRecorder
import java.io.File
import org.openrndr.math.mod
import kotlin.math.abs

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
    }
    program {

        val folders = File("screenshots/").walk().filter { it.isDirectory }.toMutableList().drop(1)
        var i = 0

        fun loadImages(): List<ColorBuffer> {
            return folders[i].walk().filter { it.isFile }.toMutableList().sortedBy { it.nameWithoutExtension.toInt() }.map {
                loadImage(it)
            }
        }

        var images = loadImages()

        keyboard.keyUp.listen {
            if(it.key == KEY_ARROW_RIGHT && i < folders.size) {
                i++
                images = loadImages()
            }
            if(it.key == KEY_ARROW_LEFT && i > 0) {
                i++
                images = loadImages()
            }
        }

        extend(ScreenRecorder())

        extend {
            val t =  (2.0 * abs(mod(seconds * 0.12, 1.0) - 0.5) * images.size).coerceAtMost(29.0)
            drawer.image(images[t.toInt()])

            drawer.fill = ColorRGBa.BLACK
            drawer.fontMap = loadFont("data/fonts/default.otf", 26.0)
            drawer.text("$i", 10.0, 36.0)

        }
    }
}