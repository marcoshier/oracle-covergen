package textSandbox

import com.google.gson.Gson
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.shadestyles.LinearGradientOKLab
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File

val fontList = listOf(
    loadFont("data/fonts/IBMPlexSans-SemiBold.otf", 46.0), // title
    loadFont("data/fonts/IBMPlexSans-Text.otf", 26.0), // Authors
    loadFont("data/fonts/IBMPlexSans-Medium.otf", 26.0), // Faculty
    loadFont("data/fonts/IBMPlexSans-Text.otf", 18.0), // Dept
    loadFont("data/fonts/IBMPlexSans-Text.otf", 18.0), // Date
)

class Section(val rect: Rectangle, val direction: Int = 0): Animatable() {

    var currentIndex = 0
    var animatedRect = rect
    var k = 0.0
    var font = fontList[direction]

    fun unfold(index: Int) {
        animate(::k, 0.0, 0).completed.listen {
            currentIndex = index
            val delay = (index * 200).toLong()
            animate(::k, 1.0, 1650, Easing.CubicInOut, predelayInMs = 400 + delay)
        }
    }

    private fun dynamicRect(parent: Rectangle): Rectangle {
        return when(direction) {
            1 -> rect.scaledBy(1.0, k, 1.0, 1.0).widthScaledTo(parent.width).movedTo(Vector2(parent.x, parent.y + parent.height - animatedRect.height))
            2 -> rect.scaledBy(k, 1.0, 0.0, 1.0).heightScaledTo(parent.height).movedTo(Vector2(parent.x, parent.y + parent.height - animatedRect.height))
            3 -> rect.scaledBy(1.0, k, 0.0, 0.0).widthScaledTo(parent.width).movedTo(parent.corner)
            4 -> rect.scaledBy(k, 1.0, 1.0, 1.0).heightScaledTo(parent.height).movedTo(Vector2(parent.x + parent.width - animatedRect.width, parent.y))
            else -> rect
        }
    }

    fun draw(drawer: Drawer, parentRect: Rectangle, childRect: Rectangle?, text: String) {

        updateAnimation()
        animatedRect = dynamicRect(parentRect)

        drawer.isolated {
            drawer.shadeStyle = LinearGradientOKLab(ColorRGBa.GRAY.toOKLABa().opacify(0.5), ColorRGBa.BLACK.toOKLABa().opacify(0.5), rotation = 90.0 * direction)
            drawer.fill = ColorRGBa.WHITE
            drawer.drawStyle.blendMode = BlendMode.MULTIPLY
            drawer.stroke = null
            drawer.rectangle(animatedRect)
        }

        drawer.fill = ColorRGBa.WHITE.opacify(k)
        drawer.fontMap = font

        // not working :(
        //drawer.drawStyle.clip = animatedRect
        val offset = 20.0
        drawer.writer {
            val childWidth = childRect?.width ?: 0.0
            val childHeight = childRect?.height ?: 0.0
            box = when (direction) {
                1 -> Rectangle(rect.x + childWidth + 5.0, rect.y  + offset, rect.width - childWidth - 20.0, rect.height)
                2 -> Rectangle(rect.x, rect.y + childHeight  + offset, rect.width, rect.height - childHeight)
                3 -> Rectangle(rect.x + 5.0, rect.y + offset, rect.width - childWidth, rect.height)
                4 -> Rectangle(rect.x + 5.0, rect.y  + offset, rect.width - childWidth, rect.height)
                else -> box.offsetEdges(-10.0)
            }.offsetEdges(-5.0)
            text(text.trimIndent())
        }

        //drawer.drawStyle.clip = null

    }

}

class Coverlay(val initialFrame: Rectangle, val data: List<String>) {
    private val initialK = 0.575 // Space for title

    var subdivisionsLeft = 4
    var allSections = mutableListOf<Section>()
    var currentDirection = 1
    var backgroundImage: ColorBuffer? = null

    fun subdivide(frame: Section) {

        if(currentDirection > 4) {
            currentDirection = 1
        }

        if(data[currentDirection] != "") {

            when(currentDirection) {
                1 -> frame.rect.sub(0.0, 0.0, 1.0, 1.0 - initialK).movedBy(Vector2(0.0, initialK * frame.rect.height))
                2 -> frame.rect.sub(0.0, 0.0, 1.0 - initialK, 1.0).movedBy(Vector2(0.0, 0.0))
                3 -> frame.rect.sub(0.0, 0.0, 1.0, 1.0 - initialK).movedBy(Vector2(0.0, 0.0))
                4 -> frame.rect.sub(0.0, 0.0, initialK, 1.0).movedBy(Vector2((1.0 - initialK) * frame.rect.width, 0.0))
                else -> frame.rect
            }.also {
                val newSect = Section(it, currentDirection)
                allSections.add(newSect)

                subdivisionsLeft--

                if(subdivisionsLeft > 0) {
                    currentDirection++
                    subdivide(newSect)
                }
            }
        }

    }

    fun unfold() {
        allSections.forEachIndexed {  i, section ->
            section.unfold(i)
        }
    }

    fun draw(drawer: Drawer) {

        if(backgroundImage != null) {
            drawer.image(backgroundImage!!)
        } else {
            drawer.clear(ColorRGBa.PINK)
        }

        drawer.fill = ColorRGBa.WHITE
        drawer.fontMap = fontList[0]
        drawer.writer {
            box = initialFrame.offsetEdges(-20.0).scaledBy(0.64, 1.0, 0.0, 0.0)
            gaplessNewLine()
            text(data[0])
        }

        allSections.forEachIndexed { i, section ->
            val parent = if(i == 0) initialFrame else allSections[i - 1].animatedRect
            val child = if(i == allSections.size - 1) null else allSections[i + 1].animatedRect
            section.draw(drawer, parent, child, data[i + 1])
        }

    }
}

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
        //position = IntVector2(2450, - 2120)
    }
    program {

        class Entry(var ogdata: Map<String, String> = emptyMap())

        val currentIndex = 1
        val data = Gson().fromJson(File("data/randomPicked2.json").readText(), Array<Entry>::class.java).filter { it.ogdata["Title"] != "" }.map {
            listOf(it.ogdata["Title"], it.ogdata["Author"], it.ogdata["Faculty"], it.ogdata["Department"], it.ogdata["Date"]) as List<String>
        }
        val initialFrame = Section(drawer.bounds.offsetEdges(-40.0))

        val testImage = loadImage("data/generated/142082.png")
        val overlay = Coverlay(testImage.bounds, data[currentIndex].drop(1)).apply {
            subdivide(initialFrame) // recursive
            unfold()
        }

        extend {

            drawer.fill = ColorRGBa.WHITE.shade(0.5)
            drawer.stroke = ColorRGBa.WHITE
            drawer.rectangle(initialFrame.rect)

            overlay.draw(drawer)
        }
    }
}