import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.simplex1D
import org.openrndr.extra.palette.Palette
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extras.color.presets.ORANGE
import org.openrndr.extras.color.presets.ORCHID
import org.openrndr.extras.color.presets.VIOLET
import org.openrndr.math.CatmullRom3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.ShapeContour
import org.openrndr.math.transforms.transform
import kotlin.math.PI
import kotlin.math.cos

class VerticalThickLine(gui: GUI) {

    var columnVb: VertexBuffer? = null
    private var thickLineSlider = object {

        @DoubleParameter("Thickness", 0.1, 40.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.1, 100.0)
        var penPressure = 5.0

        @DoubleParameter("Turbulence", 0.0, 5.0)
        var turbulence = 0.5

        @DoubleParameter("Turbulence Scale", 0.001, 0.08)
        var turbulenceScale = 0.001

    }.addTo(gui, "ThickLine / Vertical")

    fun write(t: Double, vertexes: List<Vector3>, heightSegments: Int, palette: List<List<ColorRGBa>>){

        columnVb = vertexBuffer(vertexFormat {
            position(3)
            color(4)
        }, heightSegments * 2).apply {
            put {

                vertexes.forEachIndexed { i, vertex ->

                    val pc = i.toDouble() / heightSegments.toDouble()


                    val color = palette[2][1].mix(palette[1][3], pc)
                    val turbulence = simplex(124, vertex.x * thickLineSlider.turbulenceScale + t + (i * 0.05), vertex.y * thickLineSlider.turbulenceScale + t) * thickLineSlider.turbulence

                    write(vertex.copy(x = vertex.x - (thickLineSlider.thickness * turbulence)))
                    write(color.toVector4())
                    write(vertex.copy(x = vertex.x + (thickLineSlider.thickness * turbulence)))
                    write(color.toVector4())

                }

            }
        }

    }


    fun draw(drawer: Drawer) {
        drawer.run {
            shadeStyle = shadeStyle {
                fragmentTransform = "x_fill = va_color;"
            }
            vertexBuffer(columnVb!!, DrawPrimitive.TRIANGLE_STRIP)
        }

        columnVb!!.destroy()
    }
}