import com.google.gson.Gson
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.fettepalette.generateColorRamp
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import java.io.File
import java.io.FileReader

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
        //position = IntVector2(1300, -1280)
        hideWindowDecorations = true
        windowAlwaysOnTop = true
    }
    program {

        var showTitle = false
        var showColorRamp = false

        var parametersPath = "data/new/parameters/3A0bbb8c8b-2126-45ab-8da7-20b8c0f496a4.json"

        class Director {

            val gui = GUI(baseColor = ColorRGBa.GRAY.shade(0.3))
            val colors = listOf(
                "#FF4D00",
                "#91EE9A",
                "#000DFF",
                "#76645A",
                "#9990FF",
                "#A3DEFF",
                "#F5C5CB",
                "#FDFF9B",
                "#11132E",
                "#FDFDFD"
            ).map { ColorRGBa.fromHex(it).toHSVa() }

            private val colorSliders = object {
                @IntParameter("Main Hue", 0, 9)
                var centerHue = 0

                @DoubleParameter("Tint Shade Hue Shift", 0.0, 1.0)
                var tintShadeHueShift = 0.0

                @IntParameter("Contrast Reversal", 0, 1)
                var contrastReversal = 0

            }.addTo(gui, "Color Settings")
            private val fxSliders = object {

                @IntParameter("Dry / Wet", 0, 1)
                var active = 0

                // PERTURB

                @DoubleParameter("Perturb Radius", 0.0, 10.0)
                var radius = 1.0


                @DoubleParameter("Perturb Scale", 0.0, 10.0)
                var scale = 1.0

                @DoubleParameter("Perturb Velocity", 0.0, 0.4)
                var velocity = 1.0

                @DoubleParameter("Perturb Decay", 0.0, 10.0)
                var decay = 1.0

                @DoubleParameter("Perturb Gain", 0.0, 0.224, precision = 3)
                var gain = 1.0


                // POISSON

                @IntParameter("Poisson Active", 0, 1)
                var poisson = 0


                // CELLS

                @IntParameter("Cells Amount X", 1, 20)
                var cellsX = 1

                @IntParameter("Cells Amount Y", 1, 20)
                var cellsY = 1


                // FLUID DISTORT

                @DoubleParameter("Fluid Distort", 0.0, 1.0, precision = 3)
                var fd = 0.0

            }.addTo(gui, "FX")


            val ecosystem = Ecosystem(gui, drawer.bounds)


            val cellsRt = renderTarget(width, height, multisample = BufferMultisample.SampleCount(8)) {
                colorBuffer()
                depthBuffer()
            } // effects
            val cellsRtResolved = colorBuffer(width, height)

            val fxed = colorBuffer(width, height)
            val fluidDistorted = colorBuffer(width, height)

            val perturb = Perturb()
            val poisson = PoissonFill()
            val fd = FluidDistort()

            fun draw() {

                val palette = generateColorRamp(
                    total = 3,
                    centerHue = colors[colorSliders.centerHue].h,
                    offsetTint = 0.2,
                    offsetShade = 0.24,
                    hueCycle = 0.4,
                    tintShadeHueShift = colorSliders.tintShadeHueShift,
                    useOK = true
                ).run {
                    if(colorSliders.contrastReversal == 1) {
                        listOf(lightColors.asReversed(), baseColors, darkColors)
                    } else {
                        listOf(darkColors, baseColors, lightColors.asReversed())
                    }
                }


                drawer.clear(palette[2][0])
                ecosystem.draw(drawer, seconds, palette)


                // revert to orthographic projection and apply post-fx
                drawer.defaults()
                if(fxSliders.active == 1) {

                    if(fxSliders.radius > 0.0 || fxSliders.scale > 0.0 || fxSliders.velocity > 0.0 || fxSliders.decay > 0.0 || fxSliders.gain > 0.0) {

                        perturb.apply {
                            radius = fxSliders.radius
                            offset = Vector2.ONE + seconds * fxSliders.velocity
                            scale = fxSliders.scale
                            decay = fxSliders.decay
                            gain = fxSliders.gain
                        }
                        perturb.apply(ecosystem.dryResolved, fxed)

                    }
                    if(fxSliders.poisson == 1) {
                        poisson.apply(fxed, fxed)
                    }
                    if(fxSliders.cellsX > 1 || fxSliders.cellsY > 1) {
                        drawer.isolatedWithTarget(cellsRt) {
                            drawer.clear(ColorRGBa.TRANSPARENT)
                            val areas = (0..fxSliders.cellsY).flatMap { y ->
                                (0..fxSliders.cellsX).map { x ->
                                    val t = seconds * 0.05
                                    val n = (simplex(2383, x * 0.05 + t, y * 0.05 + t, seconds * 0.05) * 0.5 + 0.51) * (fxSliders.cellsX * fxSliders.cellsY / 400.0)

                                    val source = Rectangle(n * width, n * height, width / fxSliders.cellsX * 1.0, height / fxSliders.cellsY * 1.0)
                                    val target = Rectangle((x - 1) * (width / fxSliders.cellsX - 1.0) , (y - 1) * (height / fxSliders.cellsY * 1.0), width / fxSliders.cellsX * 1.0, height / fxSliders.cellsY * 1.0)
                                    source to target
                                }
                            }
                            drawer.image(fxed, areas)
                        }
                        cellsRt.colorBuffer(0).copyTo(cellsRtResolved)

                        if(fxSliders.fd > 0.0) {
                            fd.blend = 1.0 - fxSliders.fd
                            fd.apply(cellsRtResolved, fluidDistorted)
                            drawer.image(fluidDistorted)
                        } else {
                            drawer.image(cellsRtResolved)
                        }
                    } else {
                        drawer.image(fxed)
                    }

                } else {
                    drawer.image(ecosystem.dryResolved)
                }


                // TEST STUFF
                if(showColorRamp) {
                    showColorRamp(palette)
                }
                if(showTitle) {
                    showTitle(palette)
                }
            }
        }
        val director = Director()

        //extend(ScreenRecorder())
        extend(TimeOperators()) {
            track(director.ecosystem.lfo)
        }
        extend(director.ecosystem.orb)
        val g = extend(director.gui)
        //g.loadParameters(File(parametersPath))
        extend(Screenshots()) {
/*            afterScreenshot.listen {
                val id = uuids[currentIndex]
                println("$currentIndex,  $id")

                g.saveParameters(File("data/parameters/$id.json"))
                currentIndex++
            }*/
        }

        extend {
            g.visible = mouse.position.x < 200.0
            director.draw()
        }
    }
}

fun Program.showTitle(palette: List<List<ColorRGBa>>) {
    val sampleTitle = "A tool for collaborative circular proposition design"

    drawer.isolated {
        drawer.defaults()

        drawer.fill = null
        drawer.stroke = palette[0][0]
        drawer.strokeWeight = 2.0

        val titleRect = Rectangle(Vector2.ZERO, width * 1.0, height / 2.0)

        drawer.rectangle(titleRect)
        drawer.rectangle(drawer.bounds.center, width / 2.0, height / 2.0)
        drawer.rectangle(0.0, height / 4.0 * 3, width / 2.0, height / 4.0)

        drawer.fill = palette[0][0]
        drawer.fontMap = loadFont("data/fonts/default.otf", 65.0)
        writer {
            box = titleRect.offsetEdges(-20.0)
            newLine()
            text(sampleTitle)
        }
    }
}

fun Program.showColorRamp(palette: List<List<ColorRGBa>>) {

    drawer.translate(width - (60.0 * palette[0].size), height - 200.0)
    drawer.isolated {
        for ((index, i) in palette[0].withIndex()) {
            drawer.stroke = null
            drawer.fill = i.toRGBa()
            drawer.rectangle(20.0, 20.0, 50.0, 50.0)
            drawer.translate(50.0, 0.0)
        }
    }
    drawer.isolated {
        for ((index, i) in palette[1].withIndex()) {
            drawer.stroke = null
            drawer.fill = i.toRGBa()
            drawer.rectangle(20.0, 70.0, 50.0, 50.0)
            drawer.translate(50.0, 0.0)
        }
    }
    drawer.isolated {
        for ((index, i) in palette[2].withIndex()) {
            drawer.stroke = null
            drawer.fill = i.toRGBa()
            drawer.rectangle(20.0, 120.0, 50.0, 50.0)
            drawer.translate(50.0, 0.0)
        }
    }
}