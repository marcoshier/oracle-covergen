import components.skipPoints
import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blend.Screen
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.distort.Lenses
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.launch
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import java.io.File

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
        windowAlwaysOnTop = false
    }
    program {


        val gui = GUI(baseColor = ColorRGBa.fromHex("#7a1414").shade(0.3))

        val fxSliders = object {

            @DoubleParameter("FX Amount", 0.0, 1.0)
            var fxAmount = 0.0

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

            @DoubleParameter("Cells Scale", 0.0, 1.0)
            var cellsScale = 0.0

            // FLUID DISTORT

            @DoubleParameter("Fluid Distort", 0.9, 1.0, precision = 3)
            var fd = 0.0

        }.addTo(gui, "FX")
        val colorSliders = object {
            @IntParameter("Main Hue", 0, 7)
            var centerHue = 0

            @DoubleParameter("Contrast Reversal", 0.0, 1.0)
            var contrastReversal = 0.0

        }.addTo(gui, "Color Settings")

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
            val current = colors[colorSliders.centerHue]

            val saturation = 1.1

            val lights = (1..3).map { current.shiftHue(-6.0 * it).shade(1.0 + 0.15 * it).saturate(saturation  -0.4).toRGBa() }
            val baseColors = (0..2).map { current.shade(1.1 - 0.3 * (it + 1)).shiftHue(-8.0 * it).saturate(saturation).toRGBa() }
            val darks = (1..3).map { current.shade(1.0 - 0.425 * it).saturate(saturation - 0.4).toRGBa() }

            val lightContrasted = (lights.asReversed() zip darks.asReversed()).map {
                it.first.mix(it.second, colorSliders.contrastReversal)
            }

            val darkContrasted = (lights.asReversed() zip darks.asReversed()).map {
                it.second.mix(it.first, colorSliders.contrastReversal)
            }


            return listOf(lightContrasted, baseColors, darkContrasted)

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



        val jsons = File("offline-data/resolved/json").walk().filter { it.isFile && it.extension == "json" }.drop(skipPoints)

        g.loadParameters(jsons.elementAt(20932))

        extend(ScreenRecorder())

        extend {


            g.visible = mouse.position.x < 200.0

            val palette = generatePalette()
            drawer.clear(palette[2][0])

            val maxSpeed = 0.8
            ecosystem.draw(drawer, seconds * maxSpeed, palette)

            // effects
            var active = cbs.perturbed
            val smoothFxAmount = smoothstep(0.55, 1.0, fxSliders.fxAmount)
            fxs.perturb.run {
                radius = fxSliders.radius * smoothFxAmount
                offset = Vector2.ONE + seconds * fxSliders.velocity * smoothFxAmount
                scale = fxSliders.scale * smoothFxAmount
                decay = fxSliders.decay * smoothFxAmount
                gain = fxSliders.gain * smoothFxAmount
            }
            fxs.perturb.apply(ecosystem.dryResolved, cbs.perturbed)
            if(fxSliders.poisson > 0) {
                fxs.poisson.apply(cbs.perturbed, cbs.poissoned)
                active = cbs.poissoned
            }
            if(fxSliders.cellsX > 1 || fxSliders.cellsY > 1) {

                fxs.lenses.run {
                    rows = (fxSliders.cellsY * smoothFxAmount).toInt() + 1
                    columns = (fxSliders.cellsX  * smoothFxAmount).toInt() + 1
                    distort = 0.6 * smoothFxAmount
                    scale = smoothstep(0.0, 0.33, fxSliders.cellsScale)
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
        }
    }
}
