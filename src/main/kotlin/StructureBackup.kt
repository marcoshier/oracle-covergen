/*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.math.*
import org.openrndr.shape.*

class StructureBackup(gui: GUI) {

    private val structureSliders = object {

        @DoubleParameter("Height", 5.0, 960.0)
        var height = 640.0

        @IntParameter("Height Segments", 10, 250)
        var heightSegments = 100


        @DoubleParameter("Width", 5.0, 540.0)
        var width = 540.0

        @IntParameter("Width / Rotation Segments", 10, 100)
        var rotationSegments = 100


        @DoubleParameter("Depth", 1.0, 540.0)
        var depth = 540.0

        @IntParameter("Depth Segments", 10, 100)
        var depthSegments = 100

    }.addTo(gui, "Structure")
    private val vertebraeSliders = object {

        @DoubleParameter("Waveform", 0.0, 2.99)
        var waveform = 0.0

        @DoubleParameter("Visibility", 0.0, 1.0)
        var visibility = 0.5

        @DoubleParameter("Wave amount", 0.0, 1.0)
        var waveAmount = 1.0

        @DoubleParameter("Wave phase", 0.1, 100.0)
        var wavePhase = 1.0

        @DoubleParameter("Wave frequency", 0.01, 0.5)
        var waveFrequency = 1.0

    }.addTo(gui, "Vertebrae")
    private val spinesSliders = object {

        @DoubleParameter("Visibility", 0.0, 1.0)
        var visibility = 0.5

        @DoubleParameter("Wave amount", 0.0, 1.0)
        var waveAmount = 1.0

        @DoubleParameter("Wave phase", 0.1, 100.0)
        var wavePhase = 1.0

        @DoubleParameter("Wave frequency", 0.1, 5.0)
        var waveFrequency = 1.0

    }.addTo(gui, "Spines")
    private val cornerSliders = object {

        @DoubleParameter("Corners amount", 0.0, 1.0)
        var corners = 0.5

    }.addTo(gui, "Corners")

    val thickLine = ThickLine(gui)



    fun draw(drawer: Drawer, t: Double, lfo: LFO, dimensions: Double = 2.0) {

            drawer.translate(250.0, (960.0 - structureSliders.height) / 2.0)
            val rows = mutableListOf<List<Vector2>>()

            for(y in 0 until structureSliders.heightSegments) {

                val f = vertebraeSliders.waveFrequency
                val ph = vertebraeSliders.wavePhase / 500.0 * y // actually works like frequency?

                val waveforms = listOf(
                    lfo.sine(f, ph),
                    lfo.saw(f, ph),
                    lfo.square(f, ph),
                    lfo.triangle(f, ph)
                )
                val slider = vertebraeSliders.waveform.toInt()


                val width = mix(structureSliders.width, (waveforms[slider] * structureSliders.width), vertebraeSliders.waveAmount)

                val yOffset = structureSliders.height / structureSliders.heightSegments * y
                val contour = Circle(0.0, 0.0, width).contour

                val movement = vertebraeSliders.waveAmount
                val p = lfo.saw(0.05 * vertebraeSliders.wavePhase) + (yOffset + 1) * (0.005 * vertebraeSliders.waveFrequency)
                val subbedContour = contour.sub(p * movement, mix(1.0, p + vertebraeSliders.visibility, movement))
                                           .equidistantPositions(structureSliders.rotationSegments)

                rows.add(subbedContour)

                val finalContour = ShapeContour.fromPoints(subbedContour, true)


                thickLine.write(t, finalContour, y, yOffset, structureSliders.rotationSegments, dimensions)
                if(vertebraeSliders.visibility > 0.0) {
                    thickLine.draw(drawer)
                }
            }







    }
}
*/
