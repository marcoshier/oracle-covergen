import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.simplex1D
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.CatmullRom3
import org.openrndr.math.Vector3
import org.openrndr.shape.ShapeContour
import org.openrndr.math.transforms.transform
import kotlin.math.PI
import kotlin.math.cos

class ThickLineBackup(gui: GUI) {

    var geometry: VertexBuffer? = null
    var transforms: VertexBuffer? = null
    private var thickLineSlider = object {

        @DoubleParameter("Thickness", 0.1, 40.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.1, 100.0)
        var penPressure = 5.0

        @DoubleParameter("Turbulence", 0.0, 5.0)
        var turbulence = 0.5

    }.addTo(gui, "ThickLine / Contour")

    fun write(t: Double, contour: ShapeContour, y: Int, yOffset: Double, rotSegments: Int, dimensions: Double) {
        val vertexes = mutableListOf<Vector3>()

        geometry = vertexBuffer(vertexFormat {
            position(3)
            color(4)
        }, rotSegments * 2).apply {
            put {
                for(j in 0 until rotSegments) {

                    val pc = j / (rotSegments - 1.0)
                    val pos = contour.position(pc)
                    val turbulence = simplex(124, pos.x * 0.01 + t + (y * 0.5), pos.y * 0.01 + t) * thickLineSlider.turbulence

                    val vertex = Vector3(pos.x, yOffset, pos.y * (dimensions - 2.0)).also { vertexes.add(it) }

                    val color = ColorRGBa.WHITE.mix(ColorRGBa.BLUE, pc)

                    write(vertex.copy(y = yOffset - thickLineSlider.thickness * turbulence))
                    write(color.toVector4())
                    write(vertex.copy(y = yOffset + thickLineSlider.thickness * turbulence))
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
        geometry!!.destroy()
    }
}