package animatedCover


import components.animatedCover.AnimatedCover
import components.skipPoints
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.LinearGradientOKLab
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle


val fontList = listOf(
    Pair("data/fonts/Roboto-Bold.ttf", 46.0), // title
    Pair("data/fonts/RobotoCondensed-Regular.ttf", 26.0), // Authors
    Pair("data/fonts/Roboto-Bold.ttf", 26.0), // Faculty
    Pair("data/fonts/RobotoCondensed-Regular.ttf", 18.0), // Dept
    Pair("data/fonts/Roboto-Medium.ttf", 18.0), // Date
)

open class Section(val rect: Rectangle, val direction: Int = 0): Animatable() {

    var currentIndex = 0
    var animatedRect = rect
    var k = 0.0
    var font = loadFont(fontList[direction].first, fontList[direction].second)


    fun fold(index: Int) {
        animate(::k, 1.0, 0).completed.listen {
            currentIndex = index
            animate(::k, 0.0, 900, Easing.CubicInOut)
        }
    }

    fun unfold(index: Int) {
        animate(::k, 0.0, 0).completed.listen {
            currentIndex = index
            val delay = (index * 300).toLong()
            animate(::k, 1.0, 1000, Easing.CubicInOut, predelayInMs = 400 + delay)
        }
    }

    fun dynamicRect(parent: Rectangle): Rectangle {
        return when(direction) {
            1 -> rect.scaledBy(1.0, k, 1.0, 1.0).widthScaledTo(parent.width).movedTo(Vector2(parent.x, parent.y + parent.height - animatedRect.height))
            2 -> rect.scaledBy(k, 1.0, 0.0, 1.0).heightScaledTo(parent.height).movedTo(Vector2(parent.x, parent.y + parent.height - animatedRect.height))
            3 -> rect.scaledBy(1.0, k, 0.0, 0.0).widthScaledTo(parent.width).movedTo(parent.corner)
            4 -> rect.scaledBy(k, 1.0, 1.0, 1.0).heightScaledTo(parent.height).movedTo(Vector2(parent.x + parent.width - animatedRect.width, parent.y))
            else -> rect
        }
    }

    open fun draw(drawer: Drawer, parentRect: Rectangle, childRect: Rectangle?, text: String) {

        updateAnimation()
        animatedRect = dynamicRect(parentRect)

        drawer.run {
            isolated {
                shadeStyle = LinearGradientOKLab(ColorRGBa.GRAY.toOKLABa().opacify(0.5), ColorRGBa.BLACK.toOKLABa().opacify(0.5), rotation = 90.0 * direction)
                fill = ColorRGBa.WHITE
                drawStyle.blendMode = BlendMode.MULTIPLY
                stroke = null
                rectangle(animatedRect)
            }
        }

        val offset = 20.0
        val childWidth = childRect?.width ?: 0.0
        val childHeight = childRect?.height ?: 0.0
        val container = when (direction) {
            1 -> Rectangle(rect.x + childWidth + 5.0, rect.y  + offset, rect.width - childWidth - 20.0, rect.height)
            2 -> Rectangle(rect.x, rect.y + childHeight  + offset, rect.width, rect.height - childHeight)
            3 -> Rectangle(rect.x + 5.0, rect.y + offset, rect.width - childWidth, rect.height)
            4 -> Rectangle(rect.x + 5.0, rect.y + 5.0, rect.width - childWidth, rect.height)
            else -> rect.offsetEdges(-10.0)
        }.offsetEdges(-5.0)


        drawer.drawStyle.clip = animatedRect

        drawer.fill = ColorRGBa.WHITE.opacify(k)
        drawer.fontMap = font

        drawer.writer {
            box = container
            text(direction.toString() + text.trimIndent())
        }


        drawer.drawStyle.clip = null
    }

}

class SectionWithQr(rect:Rectangle, direction: Int, var proxy: ColorBufferProxy): Section(rect, direction) {

    override fun draw(drawer: Drawer, parentRect: Rectangle, childRect: Rectangle?, text: String) {
        proxy?.touch()
        proxy?.priority = 0

        updateAnimation()
        animatedRect = dynamicRect(parentRect)

        drawer.run {
            // mipmaps?
            isolated {
                shadeStyle = LinearGradientOKLab(ColorRGBa.GRAY.toOKLABa().opacify(0.5), ColorRGBa.BLACK.toOKLABa().opacify(0.5), rotation = 90.0 * direction)

                fill = ColorRGBa.WHITE
                drawStyle.blendMode = BlendMode.MULTIPLY
                stroke = null
                rectangle(animatedRect)
            }
        }

        drawer.drawStyle.clip = animatedRect


        var qrSize = if(animatedRect.width > animatedRect.height) animatedRect.height else animatedRect.width
        if(proxy!!.state == ColorBufferProxy.State.LOADED) {
            proxy!!.colorBuffer?.let {
                drawer.pushStyle()
                drawer.drawStyle.colorMatrix = invert
                it.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.NEAREST)
                drawer.imageFit(it, animatedRect.corner.x, animatedRect.corner.y,  qrSize, qrSize)
                drawer.drawStyle.colorMatrix = Matrix55.IDENTITY
                drawer.popStyle()

                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = loadFont(fontList[2].first, 12.5)
                val rect = if(direction == 1 || direction == 3) Rectangle(animatedRect.width + animatedRect.x - qrSize + 8.0, animatedRect.y + 10.0, animatedRect.width - qrSize, animatedRect.height)
                            else Rectangle(animatedRect.x + 8.0, animatedRect.y + qrSize + 10.0, animatedRect.width, animatedRect.height - qrSize)
                drawer.writer {
                    box = rect
                    gaplessNewLine()
                    text("SCAN TO READ")
                }
            }
        }


        drawer.drawStyle.clip = null
    }
}

class Coverlay(val drawer: Drawer, val proxy: ColorBufferProxy, val data: List<String>, val index: Int) {

    var subdivisionsLeft = data.size - 1
    var allSections = mutableListOf<Section>()
    var currentDirection = 0

    private val initialK = 0.575 // Space for title
    private var initialFrame = Rectangle.EMPTY

    fun subdivide(frame: Section) {
        if(currentDirection == 0) {
            initialFrame = frame.rect
            currentDirection = 1
        }

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
                println(subdivisionsLeft)

                val newSect = if(subdivisionsLeft != 1) Section(it, currentDirection) else {
                    val proxy = colorBufferLoader.loadFromUrl("file:offline-data/qrs/${data[data.size - 1].toInt() + skipPoints}.png")
                    SectionWithQr(it, currentDirection, proxy)
                }

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
        for((i, section) in allSections.withIndex()) {
            section.unfold(i)
        }
    }

    fun fold() {
        for((i, section) in allSections.withIndex()) {
            section.fold(i)
        }
    }


    var font = loadFont(fontList[0].first, fontList[0].second)

    fun draw(opacity: Double = 0.0) {

        drawer.drawStyle.clip = initialFrame

        drawer.stroke = null
        if(proxy!!.state == ColorBufferProxy.State.LOADED) {
            drawer.opacify(opacity)
            drawer.imageFit(proxy.colorBuffer!!, initialFrame)
        } else {
            drawer.fill = ColorRGBa.TRANSPARENT
            drawer.rectangle(initialFrame)
        }

        drawer.opacify(1.0)

        drawer.fill = ColorRGBa.WHITE
        drawer.fontMap = font
        drawer.writer {
            box = initialFrame.offsetEdges(-20.0).scaledBy(0.64, 1.0, 0.0, 0.0)
            gaplessNewLine()
            val text = data[0]
            text(text.take((text.length * opacity).toInt()))
        }


        allSections.forEachIndexed { i, section ->
            val parent = if(i == 0) initialFrame else allSections[i - 1].animatedRect
            val child = if(i == allSections.size - 1) null else allSections[i + 1].animatedRect
            section.draw(drawer, parent, child, data[i + 1])
        }


        drawer.drawStyle.clip = null

    }
}

fun Drawer.opacify(o: Double) {
    shadeStyle = shadeStyle {
        fragmentTransform = """
                x_fill *= p_opacity;
            """.trimIndent()
        parameter("opacity", o)
    }

}
