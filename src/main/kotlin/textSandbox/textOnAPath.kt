package textSandbox

import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
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

            drawer.textOnPath(title, contour, offset = seconds)
        }
    }
}

interface DynamicText {
    fun marquee(speed: Double)
}

fun Drawer.textOnPath(text: String, nlpath: ShapeContour, fontSize: Double = 65.0, offset: Double = 0.0) {

    val path = nlpath.sampleLinear()
    val font = loadFont("data/fonts/default.otf", fontSize)
    fontMap = font

    var currentPos = 0.0

    text.forEachIndexed { i, char ->
        val charWidth = font.characterWidth(char)

        val slotSize = charWidth / path.length
        val whitespace = fontSize / path.length * 0.35
        currentPos += if(char.toString() == " ") whitespace else slotSize

        pushTransforms()
            model = path.pose(currentPos) * buildTransform { translate(charWidth / 2.0,font.height / 2.0); rotate(180.0) }
            text(char.toString(), Vector2.UNIT_X * offset * 100.0)
        popTransforms()
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