package documentation.resources

import documentation.resources.coverlayResources.Ecosystem
import documentation.resources.coverlayResources.Sliders
import org.openrndr.Application
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.distort.Lenses
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.proxyprogram.proxyApplication
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.smoothstep
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle

fun coverlayProxy(parent: Application? = null): Program = proxyApplication(parent) {
    program {



        this.width = 563
        this.height = 1000
        val frame = Rectangle(Vector2.ZERO, 563.0, 1000.0)

        val gui = GUI()
        val lfo = LFO()
        val orb = Orbital().apply {
            eye = Vector3(0.0, 0.0, 0.01)
            dampingFactor = 0.0
            near = 0.5
            far = 5000.0
            userInteraction = false
        }

        val sliders = Sliders(gui)
        sliders.addToGui()


        var passSliderValues: (json: String) -> Unit by userProperties
        passSliderValues = { sliders.update(it)}


        class Effects {
            val perturb = Perturb()
            val poisson = PoissonFill()
            val fluidDistort = FluidDistort()
            val lenses = Lenses()
        }
        val fxs = Effects()

        class ColorBuffers {
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

        val g = extend(gui)
        extend(TimeOperators()) {
            track(lfo)
        }
        extend(orb)

        extend {
            g.visible = false

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
}