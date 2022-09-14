package components.animatedCover

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.math.*
import org.openrndr.shape.*
import kotlin.math.ceil

class Structure(val gui: GUI) {

    private val structureSliders = object {

        @DoubleParameter("Height", 5.0, 1200.0)
        var height = 640.0

        @IntParameter("Height Segments", 10, 250)
        var heightSegments = 100


        @DoubleParameter("Width", 5.0, 400.0)
        var width = 540.0

        @IntParameter("Width / Rotation Segments", 10, 100)
        var rotationSegments = 100


        @DoubleParameter("Noise", 0.0, 1.0)
        var noise = 0.5

        @DoubleParameter("Noise Scale", 0.0, 0.1)
        var noiseScale = 0.1

        @DoubleParameter("Noise Frequency", 0.0, 0.5)
        var noiseFrequency = 0.005


    }.addTo(gui, "Structure")
    private val vertebraeSliders = object {
        @DoubleParameter("Visibility", 0.05, 1.0)
        var visibility = 0.5

        @DoubleParameter("Contour noise", 0.1, 1.0)
        var cNoise = 0.05

        @DoubleParameter("Contour noise frequency", 0.0, 0.02)
        var cNoiseFreq = 0.01

        @DoubleParameter("Contour offset", 0.0, 0.1)
        var cOffset = 0.01


        @DoubleParameter("Wave amount", 0.0, 1.0)
        var waveAmount = 1.0

        @IntParameter("Waveform", 0, 3)
        var waveform = 0

        @DoubleParameter("Wave frequency", 0.1, 50.0)
        var waveFrequency = 1.0

        @DoubleParameter("Wave phase", 0.01, 1.0)
        var wavePhase = 0.05


    }.addTo(gui, "Vertebrae")
    private val cellSliders = object {

        @DoubleParameter("Cells amount", 0.0, 1.0)
        var corners = 0.0

        @IntParameter("Cell type", 0, 3)
        var cellType = 0

    }.addTo(gui, "Cells")

    var width = structureSliders.width
    var height = structureSliders.height

    fun draw(drawer: Drawer, t: Double, lfo: LFO, complexity: Double, dimensions: Double = 2.0, palette: List<List<ColorRGBa>>) {

            height = structureSliders.height
            width = structureSliders.height

            // ADAPTIVE VALUES
            val heightSegments = ceil(structureSliders.heightSegments.times(complexity)).toInt().coerceAtLeast(5)
            val rotationSegments = ceil(structureSliders.rotationSegments.times(complexity)).toInt().coerceAtLeast(5)

            drawer.translate(0.0, -structureSliders.height / 2.0)


            // VERTEBRAE
            fun generateVertebrae(): List<List<Vector3>> {

                val f = vertebraeSliders.wavePhase

                return (0 until heightSegments).map { y ->

                    val ph = vertebraeSliders.waveFrequency / 500.0 * y
                    val yOffset = structureSliders.height / heightSegments * y

                    val waveforms = listOf(
                        lfo.sine(f, ph),
                        lfo.saw(f, ph),
                        lfo.square(f, ph),
                        lfo.triangle(f, ph)
                    )
                    val slider = vertebraeSliders.waveform

                    val width = mix(1.0, waveforms[slider], vertebraeSliders.waveAmount) * structureSliders.width
                    val contour = Circle(0.0, 0.0, width.coerceAtLeast(10.0)).contour
                    val sM = simplex(398, yOffset * 0.001 + t * vertebraeSliders.cNoiseFreq) * vertebraeSliders.cNoise
                    val subbedContour = contour.sub(sM + (t * vertebraeSliders.cOffset * vertebraeSliders.cNoise), sM + vertebraeSliders.visibility + (t * vertebraeSliders.cOffset  * vertebraeSliders.cNoise))

                    subbedContour.equidistantPositions(rotationSegments).mapIndexed { i, it ->
                        val scale = 0.01 * structureSliders.noiseScale
                        val n = (simplex(2844, it.x * scale, it.y * scale, t * 0.5 + y * structureSliders.noiseFrequency) * 0.5 + 0.5) * structureSliders.noise
                        val n2 = simplex(394, i * 0.009 + y * 0.05) * 20.0 * structureSliders.noise
                        it.mix(Vector2.ZERO, n).vector3(y = yOffset + n2, z = it.y * (dimensions - 2.0))
                    }
                }
            }
            val vertebraePoints = generateVertebrae()

            ThickLine(vertebraePoints, gui).apply {
                writeVertebrae(palette, t)
                drawVertebrae(drawer)
            }


            // SPINES
            fun generateSpines(): List<List<Vector3>> {
                return (0 until rotationSegments).map {
                    val col = mutableListOf<Vector3>()
                    for(row in vertebraePoints) {
                        col.add(row[it])
                    }
                    col
                }
            }
            val spinePoints = generateSpines()

            ThickLine(spinePoints, gui).apply {
                writeColumn(palette, t)
                drawColumn(drawer)
            }



            // CELLS (only for 2d)
            if(dimensions < 2.1 && vertebraePoints.isNotEmpty() && cellSliders.corners > 0.0) {
                drawer.isolated {
                    drawer.depthWrite = false
                    drawer.depthTestPass = DepthTestPass.ALWAYS

                    when (cellSliders.cellType) {
                        0 -> drawer.circles {
                                for(row in vertebraePoints){
                                    for(vertex in row) {

                                        val n = simplex(244, vertex.x * 0.003 + t * 0.3, vertex.y * 0.004 + t * 0.33) * 0.5 + 0.5
                                        val mn = n * 10.0 * cellSliders.corners

                                        this.fill = palette[0][0].mix(palette[1][0], n)

                                        this.circle(vertex.xy, ceil(mn * cellSliders.corners * 5.0))
                                    }
                                }
                            }
                        1 ->  drawer.rectangles {
                                for(row in vertebraePoints){
                                    for(vertex in row) {

                                        val n = simplex(244, vertex.x * 0.003 + t * 0.3, vertex.y * 0.004 + t * 0.33) * 0.5 + 0.5
                                        val mn = n * 10.0 * cellSliders.corners

                                        this.fill = palette[0][0].mix(palette[0][0], n)

                                        this.rectangle(Rectangle.fromCenter(vertex.xy, ceil(mn * cellSliders.corners * 5.0)))
                                    }
                                }
                            }
                    }
                }
            }




    }
}

