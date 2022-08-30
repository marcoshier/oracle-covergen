package textSandbox

import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.writer
import kotlin.math.floor

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 /2
    }
    program {

        val title = "A tool for circular proposition design"

        val contour = Circle(drawer.bounds.center, 200.0).contour

        extend {
            drawer.textOnPath(title, contour)
        }
    }
}
/*
class Marquee(val text: String, fontSize: Double = 100.0) {

    val font = loadFont("data/fonts/default.otf", fontSize)
    var finalText = text
    var textWidth = finalText.sumOf { font.characterWidth(it) }

    fun draw(t: Double, drawer: Drawer) {

        drawer.fontMap = font
        val movement = t * drawer.width * 0.3

        for(char in text) {
            drawer.text(char.toString(), movement, font.height)
        }


        // add
        if(t > textWidth) {
            finalText += text
            textWidth += textWidth
        }

        // trim
        if(t > textWidth * 2) {
            finalText.drop(text.length)
        }

        drawer.text(finalText, movement, font.height)
    }
}
*/