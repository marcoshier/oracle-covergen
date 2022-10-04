package documentation.resources

import animatedCover.Section
import animatedCover.SectionWithQr
import animatedCover.fontList
import animatedCover.opacify
import documentation.resources.coverlayResources.Ecosystem
import documentation.resources.coverlayResources.Sliders
import org.openrndr.Application
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.distort.Lenses
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.proxyprogram.proxyApplication
import org.openrndr.extra.shadestyles.LinearGradientOKLab
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.smoothstep
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle

class CoverlayBackground(val drawer: Drawer, var frame: Rectangle) {

    val gui = GUI()
    val lfo = LFO()
    val orb = Orbital().apply {
        eye = Vector3(0.0, 0.0, 0.01)
        dampingFactor = 0.0
        near = 0.5
        far = 5000.0
        userInteraction = false
    }

    val sliders = Sliders(gui).apply { addToGui() }

    class Effects {
        val perturb = Perturb()
        val poisson = PoissonFill()
        val fluidDistort = FluidDistort()
        val lenses = Lenses()
    }
    val fxs = Effects()

    inner class ColorBuffers {
        val perturbed = colorBuffer(frame.width.toInt(), frame.height.toInt())
        val poissoned = colorBuffer(frame.width.toInt(), frame.height.toInt())
        val tiled = colorBuffer(frame.width.toInt(), frame.height.toInt())
    }
    val cbs = ColorBuffers()

    fun generatePalette(): List<List<ColorRGBa>> {
        val colors = listOf(
            "#F2602B",
            "#C197FB",
            "#59DD9F",
            "#EDF63B",
            "#3566EC",
            "#6E5544",
            "#C2EFF2",
            "#EFCA64"
        ).map { ColorRGBa.fromHex(it).toHSVa() }
        val current = colors[sliders.colorSliders.centerHue]

        val saturation = 1.1

        val lights = (1..3).map { current.shiftHue(-6.0 * it).shade(1.0 + 0.15 * it).saturate(saturation  -0.4).toRGBa() }
        val baseColors = (0..2).map { current.shade(1.1 - 0.3 * (it + 1)).shiftHue(-8.0 * it).saturate(saturation).toRGBa() }
        val darks = (1..3).map { current.shade(1.0 - 0.425 * it).saturate(saturation - 0.4).toRGBa() }

        val lightContrasted = (lights.asReversed() zip darks.asReversed()).map {
            it.first.mix(it.second, sliders.colorSliders.contrastReversal)
        }

        val darkContrasted = (lights.asReversed() zip darks.asReversed()).map {
            it.second.mix(it.first, sliders.colorSliders.contrastReversal)
        }


        return listOf(lightContrasted, baseColors, darkContrasted)

    }

    val ecosystem = Ecosystem(drawer, sliders, frame, lfo, orb)

    fun draw(seconds: Double) {

        var palette = generatePalette()
        drawer.clear(palette[2][0])

        val maxSpeed = 0.8
        ecosystem.draw(seconds * maxSpeed, palette)

        // effects
        var active = cbs.perturbed
        val smoothFxAmount = smoothstep(0.55, 1.0, sliders.fxSliders.fxAmount)
        fxs.perturb.run {
            radius = sliders.fxSliders.radius * smoothFxAmount
            offset = Vector2.ONE + seconds * sliders.fxSliders.velocity * smoothFxAmount
            scale = sliders.fxSliders.scale * smoothFxAmount
            decay = sliders.fxSliders.decay * smoothFxAmount
            gain = sliders.fxSliders.gain * smoothFxAmount
        }
        fxs.perturb.apply(ecosystem.dryResolved, cbs.perturbed)
        if(sliders.fxSliders.poisson > 0) {
            fxs.poisson.apply(cbs.perturbed, cbs.poissoned)
            active = cbs.poissoned
        }
        if(sliders.fxSliders.cellsX > 1 || sliders.fxSliders.cellsY > 1) {

            fxs.lenses.run {
                rows = (sliders.fxSliders.cellsY * smoothFxAmount).toInt() + 1
                columns = (sliders.fxSliders.cellsX  * smoothFxAmount).toInt() + 1
                distort = 0.6 * smoothFxAmount
                scale = smoothstep(0.0, 0.33, sliders.fxSliders.cellsScale)
            }
            fxs.lenses.apply(active, cbs.tiled)

            if(sliders.fxSliders.fd > 0.9) {
                fxs.fluidDistort.blend = 1.0 - sliders.fxSliders.fd
                fxs.fluidDistort.apply(cbs.tiled, active)
            } else {
                active = cbs.tiled
            }
        }


        drawer.image(active)
        drawer.defaults()
    }
}

class CoverlayTextBoxes(val drawer: Drawer) {

    var data = listOf("a","b","c","d","e", "f")
    var subdivisionsLeft = data.size - 1
    var allSections = mutableListOf<animatedCover.Section>()
    var currentDirection = 0

    private val initialK = 0.575 // Space for title
    private var initialFrame = Rectangle.EMPTY


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


    fun subdivide(frame: animatedCover.Section) {
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

                val newSect = if(subdivisionsLeft != 1) animatedCover.Section(it, currentDirection) else {
                    val proxy = colorBufferLoader.loadFromUrl("file:offline-data/qrs/${data[data.size - 1].toInt() + components.skipPoints}.png")
                    animatedCover.SectionWithQr(it, currentDirection, proxy)
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

fun coverlayProxy(parent: Application? = null): Program = proxyApplication(parent) {
    program {


        this.width = 563
        this.height = 1000
        val frame = Rectangle(Vector2.ZERO, 563.0, 1000.0)

        val background = CoverlayBackground(drawer, frame)
        val textOverlay = CoverlayTextBoxes(drawer)

        var coverData: (json: String, data: List<String>?) -> Unit by userProperties
        coverData = { json, data ->
            println("changed 1")
            background.sliders.update(json)
            if(data != null) {
                textOverlay.data = data
                textOverlay.subdivide(Section(frame))
                textOverlay.unfold()
            }
        }


        val g = extend(background.gui)
        extend(TimeOperators()) {
            track(background.lfo)
        }
        extend(background.orb)
        extend {
            g.visible = false

            background.draw(seconds)
            textOverlay.draw(1.0)

        }
    }
}