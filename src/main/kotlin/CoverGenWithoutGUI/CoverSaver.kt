package CoverGenWithoutGUI

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
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
        position = IntVector2(0, 0)
        hideWindowDecorations = true
        windowAlwaysOnTop = false
    }
    program {

        var showTitle = false
        var showColorRamp = true

        class Director {

            val gui = GUI(baseColor = ColorRGBa.fromHex("#7a1414").shade(0.3))
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

            val ecosystem = Ecosystem(gui, drawer.bounds)
            val fxs = object  {
                val perturb = Perturb()
                val poisson = PoissonFill()
                val fluidDistort = FluidDistort()
                val lenses = Lenses()
                val tapeNoise = TapeNoise()
            }
            val cbs = object  {
                val perturbed = colorBuffer(width, height)
                val poissoned = colorBuffer(width, height)
                val tiled = colorBuffer(width, height)
            }

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

            fun draw() {

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
        val director = Director()

        //extend(ScreenRecorder())
        extend(TimeOperators()) {
            track(director.ecosystem.lfo)
        }
        extend(director.ecosystem.orb)
        val g = extend(director.gui)
        val s = extend(Screenshots())
/*
        val jsons = File("data/new/").walk().filter { it.isFile && it.extension == "json" }
        launch {
            for (json in jsons) {
                g.loadParameters(json)
                s.apply {
                    name = json.nameWithoutExtension
                    trigger()
                }

                for (z in 0 until 10) {
                    yield()
                }
            }
        }
*/
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