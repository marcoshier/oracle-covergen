package documentation.resources

import animatedCover.fontList
import animatedCover.opacify
import documentation.resources.coverlayResources.Ecosystem
import documentation.resources.coverlayResources.Sliders

import org.openrndr.Application
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.distort.Lenses
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.proxyprogram.proxyApplication
import org.openrndr.extra.shadestyles.LinearGradient
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.*
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.math.min

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

open class Section(val drawer: Drawer, val text: String?, val rect: Rectangle, val direction: Int = 0): Animatable() {

    var currentIndex = 0
    var animatedRect = rect
    var k = 0.0
    open var font = loadFont(fontList[direction].first, fontList[direction].second)


    fun fold(index: Int) {
        animate(::k, 1.0, 0).completed.listen {
            currentIndex = index
            animate(::k, 0.0, 900, Easing.CubicInOut)
        }
    }

    fun unfold(index: Int) {
        animate(::k, 0.0, 0).completed.listen {
            currentIndex = index
            animate(::k, 1.0, 1000, Easing.CubicInOut, predelayInMs = index * 225L)
        }
    }

    fun animatedRectangle(p: Rectangle): Rectangle {
        return when(direction) {
            1 -> Rectangle(p.x, p.y + p.height - (rect.height * k), p.width, rect.height * k)
            2 -> Rectangle(p.x, p.y, rect.width * k, p.height)
            3 -> Rectangle(p.x, p.y, p.width, rect.height * k)
            4 -> Rectangle(p.x + p.width - (rect.width * k), p.y, rect.width * k, p.height)
            else -> rect
        }
    }

    open fun draw(parent: Rectangle) {
        updateAnimation()

        animatedRect = animatedRectangle(parent)

        drawer.fill = ColorRGBa.WHITE
        drawer.run {
            isolated {
                shadeStyle = LinearGradient(ColorRGBa.GRAY.opacify(0.5), ColorRGBa.BLACK.opacify(0.5), rotation = 90.0 * direction)
                stroke = null
                drawStyle.blendMode = BlendMode.MULTIPLY
                rectangle(animatedRect)
            }
        }


        if(text != null) {
            val container = when (direction) {
                1 -> rect.sub(0.425, 1.0 - k, 1.0, 1.0)
                2 -> rect.sub(k - 1.0, 0.425, 1.0, 1.0)
                3 -> rect.sub(0.0, 1.0 - k, 0.575, 1.0)
                4 ->  rect.sub(0.0, 1.0 - k, 1.0, 1.0)
                else -> rect
            }.offsetEdges(-8.0, -12.0)

            drawer.run {
                drawStyle.clip = animatedRect
                fontMap = font
                writer {
                    box = container
                    gaplessNewLine()
                    text(text.trimIndent())
                }
                drawStyle.clip = null
            }
        }
    }

}

class SectionWithQr(drawer: Drawer, rect:Rectangle, direction: Int, var proxy: ColorBufferProxy): Section(drawer, null, rect, direction) {

    override var font = loadFont(fontList[3].first, 10.5)

    override fun draw(parent: Rectangle)  {
        updateAnimation()
        proxy?.apply {
            touch()
            priority = 0
        }

        animatedRect = animatedRectangle(parent)

        drawer.run {
            isolated {
                shadeStyle = LinearGradient(ColorRGBa.GRAY.opacify(0.5), ColorRGBa.BLACK.opacify(0.5), rotation = 90.0 * direction)
                stroke = null
                drawStyle.blendMode = BlendMode.MULTIPLY
                rectangle(animatedRect)
            }
        }

        if(proxy!!.state == ColorBufferProxy.State.LOADED) {

            drawer.run {
                drawStyle.clip = animatedRect

                proxy!!.colorBuffer?.let {

                    it.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.NEAREST)
                    val qrSize = min(animatedRect.width, animatedRect.height)
                    isolated {
                        drawer.shadeStyle = shadeStyle { fragmentTransform = """
                            vec4 c = vec4(1.0);
                            if(x_fill.r == 1.0) { c = vec4(0.0); };
                            if(x_fill.r == 0.0) { c = vec4(1.0); };
                            x_fill = c;
                        """.trimIndent()}
                        drawer.imageFit(it, animatedRect.corner.x, animatedRect.corner.y,  qrSize, qrSize)
                        drawer.shadeStyle = null
                    }

                    fill = ColorRGBa.WHITE
                    fontMap = font

                    isolated {
                        writer {
                            if(direction % 2 == 0) {
                                box = Rectangle(animatedRect.x, animatedRect.y + qrSize, animatedRect.width, animatedRect.height - qrSize).offsetEdges(-9.0)
                                text("SCAN ME")
                            } else {
                                box = Rectangle(animatedRect.x + qrSize, animatedRect.y, animatedRect.width - qrSize, animatedRect.height)
                                cursor.y += 6.0
                                for(c in "SCAN ME") {
                                    newLine()
                                    cursor.x = box.x + ((box.width - font.characterWidth(c)) / 2.0)
                                    cursor.y -= 2.0
                                    text("$c")
                                }
                            }

                        }
                    }
                }


                drawStyle.clip = null
            }

        }


    }
}

class CoverlayTextBoxes(val drawer: Drawer, var initialFrame: Rectangle = drawer.bounds, private val k: Double = 0.575): Animatable() {

    var index = 0
    var data = listOf<String>()
        set(value) {
            val clean = value.filter { it != "" && it != " " }.dropLast(1)
            index = value[value.size - 1].toInt() + skipPoints
            field = clean
            subdivisionsLeft = clean.size - 1

            subdivide(initialFrame)
        }

    var allSections = mutableListOf<Section>()
    var subdivisionsLeft = 0
    var currentDirection = 0

    var fade = 0.0

    private fun subdivide(parent: Rectangle) {
        if(currentDirection > 4 || currentDirection == 0) {
            currentDirection = 1
        }

        val i = (data.size - subdivisionsLeft + 1)

        val newRect = when(currentDirection) {
            1 -> Rectangle(parent.x, parent.y + parent.height * k, parent.width, parent.height * (1.0 - k))
            2 -> Rectangle(parent.x, parent.y, parent.width * (1.0 - k), parent.height)
            3 -> Rectangle(parent.x, parent.y, parent.width, parent.height * (1.0 - k))
            4 -> Rectangle(parent.x + parent.width * k, parent.y, parent.width * (1.0 - k), parent.height)
            else -> parent
        }
        val newSect = if(subdivisionsLeft != 1) Section(drawer, data[i], newRect, currentDirection) else {
            val proxy = colorBufferLoader.loadFromUrl("file:offline-data/qrs/$index.png")
            SectionWithQr(drawer, newRect, currentDirection, proxy)
        }

        allSections.add(newSect)
        subdivisionsLeft--

        if(subdivisionsLeft > 0) {
            currentDirection++
            subdivide(newRect)
        } else {
            unfold()
        }
    }

    private fun unfold() {
        ::fade.cancel()
        ::fade.animate(1.0, 2500, Easing.SineOut)

        for((i, section) in allSections.withIndex()) {
            section.unfold(i)
        }
    }

    fun fold() {
        ::fade.cancel()
        ::fade.animate(0.0, 2000, Easing.SineOut)

        for((i, section) in allSections.withIndex()) {
            section.fold(i)
        }
    }


    var titleFont = loadFont(fontList[0].first, fontList[0].second)

    fun draw() {
        updateAnimation()

        allSections.forEachIndexed { i, section ->
            val parent = allSections[(i - 1).coerceAtLeast(0)].animatedRect
            val child = if(i == allSections.size - 1) null else allSections[i + 1].animatedRect

            section.draw(parent)
        }

        drawer.fontMap = titleFont
        drawer.writer {
            box = initialFrame.offsetEdges(-20.0).widthScaledBy(0.675)
            gaplessNewLine()
            val text = data[0]
            text(text.take((text.length * fade).toInt()))
        }

    }
}

class Coverlay(val drawer: Drawer, val frame: Rectangle): Animatable() {

    var json: String? = null
        set(value) {
            field = value
            if(value != null) {
                background.sliders.update(value)
            }
        }

    var data: List<String>? = null
        set(value) {
            field = value
            if(value != null) {
                textOverlay = CoverlayTextBoxes(drawer, frame).apply {
                    data = value
                }
            }
            fadeIn()
        }


    var background = CoverlayBackground(drawer, frame)
    var textOverlay: CoverlayTextBoxes? = null


    var fade = 0.0

    init {
        if(json == null) {
            background.sliders.update(File("data/template.json").readText())
        }
    }

    fun fadeIn() {
        ::fade.animate(1.0, 800, Easing.SineOut)
    }

    fun fadeOut(): PropertyAnimationKey<Double>  {
        textOverlay?.fold()
        return ::fade.animate(0.0, 1100, Easing.SineIn)
    }

    fun draw(seconds: Double) {
        updateAnimation()

        background.draw(seconds)
        drawer.stroke = null
        drawer.fill  = ColorRGBa.BLACK.opacify(1.0 - fade)
        drawer.rectangle(frame)

        textOverlay?.draw()
    }

}

fun coverlayProxy(parent: Application? = null): Program = proxyApplication(parent) {
    program {

        this.width = 540
        this.height = 960
        val frame = Rectangle(Vector2.ZERO, 540.0, 960.0)

        var coverlay = Coverlay(drawer, frame)

        var coverData: (json: String, articleData: List<String>?) -> Unit by userProperties
        coverData = { json, data ->
            coverlay.fadeOut().completed.listen {
                coverlay.json = json
                coverlay.data = data
            }
        }


        val g = extend(coverlay.background.gui)
        extend(TimeOperators()) {
            track(coverlay.background.lfo)
        }
        extend(coverlay.background.orb)


        extend {
            g.visible = false

            coverlay.draw(seconds)

        }
    }
}

fun Rectangle.widthScaledBy(factor: Double): Rectangle {
    return Rectangle(x, y, width * factor, height)
}