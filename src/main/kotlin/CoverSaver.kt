import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.fettepalette.generateColorRamp
import org.openrndr.extra.fx.distort.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import java.io.File

fun main() = application {
    configure {
        width = 1080
        height = 1920
        position = IntVector2(2450, -1980)
        hideWindowDecorations = true
        windowAlwaysOnTop = false
    }
    program {

        var coverSaver = true
        var showTitle = false
        var showColorRamp = true


        val gui = GUI(baseColor = ColorRGBa.fromHex("#7a1414").shade(0.3))
        val colors = listOf(
            "#F2602B",
            "#C197FB",
            "#59DD9F",
            "#EDF63B",
            "#3566EC",
            "#6E5544",
            "#C2EFF2",
            "#EFCA64",
            "#141414",
            "#FDFDFD"
        ).map { ColorRGBa.fromHex(it).toHSVa() }

        val colorSliders = object {
            @IntParameter("Main Hue", 0, 9)
            var centerHue = 0

            @IntParameter("Contrast Reversal", 0, 1)
            var contrastReversal = 0

        }.addTo(gui, "Color Settings")
        val fxSliders = object {

            @IntParameter("Dry / Wet", 0, 1)
            var active = 0

            // PERTURB

            @DoubleParameter("Perturb Radius", 0.0, 10.0)
            var radius = 1.0


            @DoubleParameter("Perturb Scale", 0.0, 10.0)
            var scale = 1.0

            @DoubleParameter("Perturb Velocity", 0.0, 0.3)
            var velocity = 0.15

            @DoubleParameter("Perturb Decay", 0.0, 10.0)
            var decay = 1.0

            @DoubleParameter("Perturb Gain", 0.0, 0.224, precision = 3)
            var gain = 1.0


            // POISSON

            @IntParameter("Poisson Active", 0, 1)
            var poisson = 0


            // CELLS

            @IntParameter("Cells Amount X", 1, 50)
            var cellsX = 1

            @IntParameter("Cells Amount Y", 1, 50)
            var cellsY = 1

            @DoubleParameter("Cells Distort", 0.0, 1.0)
            var cellsDistort = 0.5

            @DoubleParameter("Cells Scale", 0.0, 0.5)
            var cellsScale = 0.0

            // FLUID DISTORT

            @DoubleParameter("Fluid Distort", 0.9, 1.0, precision = 3)
            var fd = 0.0

        }.addTo(gui, "FX")

        fun generatePalette(): List<List<ColorRGBa>> {
            return generateColorRamp(
                total = 3,
                centerHue = colors[colorSliders.centerHue].h,
                offsetTint = 0.2,
                offsetShade = 0.18,
                hueCycle = 0.0,
                tintShadeHueShift = 0.05,
                useOK = true
            ).run {
                if(colorSliders.contrastReversal == 1) {
                    listOf(lightColors.asReversed(), baseColors, darkColors)
                } else {
                    listOf(darkColors, baseColors, lightColors.asReversed())
                }
            }
        }

        val ecosystem = Ecosystem(gui, drawer.bounds)
        val fxs = object  {
            val perturb = Perturb()
            val poisson = PoissonFill()
            val fluidDistort = FluidDistort()
            val lenses = Lenses()
        }
        val cbs = object  {
            val perturbed = colorBuffer(width, height)
            val poissoned = colorBuffer(width, height)
            val tiled = colorBuffer(width, height)
        }

        //extend(ScreenRecorder())
        extend(TimeOperators()) {
            track(ecosystem.lfo)
        }
        extend(ecosystem.orb)
        val g = extend(gui)
        val s = extend(Screenshots())

        if(coverSaver) {
            showTitle = false
            showColorRamp = false
            val jsons = File("data/xyNew/").walk().filter { it.isFile && it.extension == "json" }

            launch {
                for (json in jsons) {
                    g.loadParameters(json)
                    s.apply {
                        name = "screenshots/${json.nameWithoutExtension}.png"
                        trigger()
                    }

                    for (z in 0 until 10) {
                        yield()
                    }
                }

                println("Finished")
            }
        }

        extend {

            val palette = generatePalette()
            drawer.clear(palette[2][0])

            ecosystem.draw(drawer, seconds, palette)
            if(fxSliders.active > 0) {

                var active = cbs.perturbed

                fxs.perturb.run {
                    radius = fxSliders.radius
                    offset = Vector2.ONE + seconds * fxSliders.velocity
                    scale = fxSliders.scale
                    decay = fxSliders.decay
                    gain = fxSliders.gain
                }
                fxs.perturb.apply(ecosystem.dryResolved, active)
                if(fxSliders.poisson > 0) {
                    fxs.poisson.apply(cbs.perturbed, cbs.poissoned)
                    active = cbs.poissoned
                }
                if(fxSliders.cellsX > 1 || fxSliders.cellsY > 1) {

                    fxs.lenses.run {
                        rows = fxSliders.cellsY
                        columns = fxSliders.cellsX
                        distort = fxSliders.cellsDistort
                        scale = fxSliders.cellsScale
                    }
                    fxs.lenses.apply(active, cbs.tiled)

                    if(fxSliders.fd > 0.9) {
                        fxs.fluidDistort.blend = 1.0 - fxSliders.fd
                        fxs.fluidDistort.apply(cbs.tiled, active)
                    } else {
                        active = cbs.tiled
                    }
                }

                drawer.image(active)
            } else {
                drawer.image(ecosystem.dryResolved)
            }


            if(showColorRamp) {
                showColorRamp(palette)
            }
            if(showTitle) {
                showTitle(palette)
            }

        }
    }
}