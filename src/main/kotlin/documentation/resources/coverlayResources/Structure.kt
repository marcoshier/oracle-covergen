package documentation.resources.coverlayResources

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

class Structure(val drawer: Drawer, val sliders: Sliders) {

    var width = sliders.structureSliders.width
    var height = sliders.structureSliders.height

    var rotationSegments = sliders.structureSliders.rotationSegments
    var heightSegments = sliders.structureSliders.heightSegments

    fun generateVertebrae(lfo: LFO, t: Double): List<List<Vector3>> {

        val f = sliders.vertebraeSliders.wavePhase

        return (0 until heightSegments).map { y ->

            val ph = sliders.vertebraeSliders.waveFrequency / 500.0 * y
            val yOffset = sliders.structureSliders.height / heightSegments * y

            val waveforms = listOf(
                lfo.sine(f, ph),
                lfo.saw(f, ph),
                lfo.square(f, ph),
                lfo.triangle(f, ph)
            )
            val slider = sliders.vertebraeSliders.waveform

            val width = mix(1.0, waveforms[slider], sliders.vertebraeSliders.waveAmount) * sliders.structureSliders.width
            val contour = Circle(0.0, 0.0, width.coerceAtLeast(10.0)).contour
            val sM = simplex(398, yOffset * 0.001 + t * sliders.vertebraeSliders.cNoiseFreq) * sliders.vertebraeSliders.cNoise
            val subbedContour = contour.sub(sM + (t * sliders.vertebraeSliders.cOffset * sliders.vertebraeSliders.cNoise), sM + sliders.vertebraeSliders.visibility + (t * sliders.vertebraeSliders.cOffset  * sliders.vertebraeSliders.cNoise))

            subbedContour.equidistantPositions(rotationSegments).mapIndexed { i, it ->
                val scale = 0.01 * sliders.structureSliders.noiseScale
                val n = (simplex(2844, it.x * scale, it.y * scale, t * 0.5 + y * sliders.structureSliders.noiseFrequency) * 0.5 + 0.5) * sliders.structureSliders.noise
                val n2 = simplex(394, i * 0.009 + y * 0.05) * 20.0 * sliders.structureSliders.noise
                it.mix(Vector2.ZERO, n).vector3(y = yOffset + n2, z = it.y * (sliders.cameraSliders.dimensions - 2.0))
            }
        }
    }

    fun generateSpines(vertebraePoints: List<List<Vector3>>): List<List<Vector3>> {
        return (0 until rotationSegments).map {
            val col = mutableListOf<Vector3>()
            for(row in vertebraePoints) {
                col.add(row[it])
            }
            col
        }
    }


    fun draw(t: Double, lfo: LFO, complexity: Double, palette: List<List<ColorRGBa>>) {

            height = sliders.structureSliders.height
            width = sliders.structureSliders.height

            heightSegments = ceil(sliders.structureSliders.heightSegments.times(complexity)).toInt().coerceAtLeast(5)
            rotationSegments = ceil(sliders.structureSliders.rotationSegments.times(complexity)).toInt().coerceAtLeast(5)

            drawer.translate(0.0, -sliders.structureSliders.height / 2.0)

            // VERTEBRAE
            val vertebraePoints = generateVertebrae(lfo, t)
            ThickLine(drawer, vertebraePoints, sliders).apply {
                writeVertebrae(palette, t)
                drawVertebrae()
            }

            // SPINES
            val spinePoints = generateSpines(vertebraePoints)
            ThickLine(drawer, spinePoints, sliders).apply {
                writeColumns(palette, t)
                drawColumn()
            }

            // CELLS (only for 2d)
            if(sliders.cameraSliders.dimensions < 2.1 && vertebraePoints.isNotEmpty() && sliders.cellSliders.corners > 0.0) {
                drawer.isolated {
                    drawer.depthWrite = false
                    drawer.depthTestPass = DepthTestPass.ALWAYS

                    when (sliders.cellSliders.cellType) {
                        0 -> drawer.circles {
                                for(row in vertebraePoints){
                                    for(vertex in row) {

                                        val n = simplex(244, vertex.x * 0.003 + t * 0.3, vertex.y * 0.004 + t * 0.33) * 0.5 + 0.5
                                        val mn = n * 10.0 * sliders.cellSliders.corners

                                        this.fill = palette[0][0].mix(palette[1][0], n)

                                        this.circle(vertex.xy, ceil(mn * sliders.cellSliders.corners * 5.0))
                                    }
                                }
                            }
                        1 ->  drawer.rectangles {
                                for(row in vertebraePoints){
                                    for(vertex in row) {

                                        val n = simplex(244, vertex.x * 0.003 + t * 0.3, vertex.y * 0.004 + t * 0.33) * 0.5 + 0.5
                                        val mn = n * 10.0 * sliders.cellSliders.corners

                                        this.fill = palette[0][0].mix(palette[0][0], n)

                                        this.rectangle(Rectangle.fromCenter(vertex.xy, ceil(mn * sliders.cellSliders.corners * 5.0)))
                                    }
                                }
                            }
                    }
                }
            }




    }
}

