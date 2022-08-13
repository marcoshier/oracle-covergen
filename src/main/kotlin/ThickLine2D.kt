import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.simplex1D
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos

class ThickLine2D(gui: GUI) {

    var geometry: VertexBuffer? = null
    private var thickLineSlider = object {

        @DoubleParameter("Thickness", 0.1, 40.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.1, 100.0)
        var penPressure = 5.0

        @DoubleParameter("Turbulence", 0.0, 5.0)
        var turbulence = 0.5

    }.addTo(gui, "ThickLine / Contour")

    fun write(t: Double, contour: ShapeContour) {

        val vc = 50
        geometry = vertexBuffer(vertexFormat {
            position(3)
            color(4)
        }, vc * 2).apply {
            put {
                for(j in 0 until vc) {

                    val pc = j / (vc - 1.0)
                    val n = simplex(232, j * 0.05, t) + j
                    val subbedContour = contour
                    val pos = subbedContour.position(pc)

                    val turbulence = (1 * Random.perlin(pos * thickLineSlider.turbulence))
                    val penPressure = (thickLineSlider.thickness * (cos(pc * PI * 2))) * thickLineSlider.penPressure
                    val normal = subbedContour.normal(pc).normalized * (thickLineSlider.thickness - penPressure + turbulence)

                    val color = ColorRGBa.WHITE.mix(ColorRGBa.BLUE, pc)

                    write((pos + normal).vector3(z = 0.0))
                    write(color.toVector4())
                    write((pos - normal).vector3(z = 0.0))
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

            vertexBuffer(geometry!!, DrawPrimitive.TRIANGLE_STRIP)
        }

    }
}