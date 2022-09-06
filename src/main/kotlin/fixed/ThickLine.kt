package fixed

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.*
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos

class ThickLine(val columns: List<List<Vector3>>, val gui: GUI) {

    private var columnContours = object {

        @DoubleParameter("Thickness", 0.0, 20.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.0, 1.0)
        var penPressure = 1.0

        @DoubleParameter("Turbulence Amount", 0.0, 1.0)
        var turbulenceAmount = 0.5

        @DoubleParameter("Turbulence Scale", 0.001, 0.08)
        var turbulenceScale = 0.001

    }.addTo(gui, "ThickLine / Vertical")
    private var rowContours = object {

        @DoubleParameter("Thickness", 0.0, 20.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.0, 1.0)
        var penPressure = 1.0

        @DoubleParameter("Turbulence Amount", 0.0, 1.0)
        var turbulenceAmount = 0.5

        @DoubleParameter("Turbulence Scale", 0.001, 0.08)
        var turbulenceScale = 0.001

    }.addTo(gui, "Contour / Horizontal")

    val heightSegments = columns.size
    val rotSegments =  columns[0].size

    val vb = vertexBuffer(vertexFormat {
        position(3)
        color(4)
    }, heightSegments * rotSegments * 2)

    fun writeVertebrae(palette: List<List<ColorRGBa>>, t: Double) {

        val tone1 = palette[0][1]
        val tone2 = palette[1][1]

        vb.put {
            columns.forEach {points ->
                points.forEachIndexed { i, v ->

                        val pc = i.toDouble() / rotSegments.toDouble()

                        val color = tone1.mix(tone2, pc)
                        val turbulence = (simplex(435, v * rowContours.turbulenceScale + t * rowContours.turbulenceScale) * 0.5 + 0.5) * rowContours.turbulenceAmount
                        val pp = rowContours.thickness * cos(pc * PI * 2) * rowContours.penPressure
                        val finalStrokeWeight = rowContours.thickness - pp - (turbulence * rowContours.thickness / 2)

                        val vertex1 = v.copy(y = v.y - (rowContours.thickness * finalStrokeWeight))

                        val vertex2 = v.copy(y = v.y + (rowContours.thickness * finalStrokeWeight))

                        write(vertex1)
                        write(color.toVector4())
                        write(vertex2)
                        write(color.toVector4())

                }
            }
        }
    }

    fun drawVertebrae(drawer: Drawer) {
        drawer.shadeStyle = shadeStyle {
            fragmentTransform = "x_fill = va_color;"
        }

        drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLE_STRIP)
        vb.destroy()

    }


    fun writeColumn(palette: List<List<ColorRGBa>>, t: Double) {

        val tone1 = palette[1][2]
        val tone2 = palette[2][2]

        vb.put {
            columns.forEach {points ->
                points.forEachIndexed { i, v ->

                    val pc = i.toDouble() / rotSegments.toDouble()

                    val color = tone1.mix(tone2, pc)
                    val turbulence = (simplex(435, v * columnContours.turbulenceScale + t * columnContours.turbulenceScale) * 0.5 + 0.5) * columnContours.turbulenceAmount
                    val pp = columnContours.thickness * cos(pc * PI * 2) * columnContours.penPressure
                    val finalStrokeWeight = columnContours.thickness - pp - (turbulence * columnContours.thickness / 2)

                    val vertex1 = v.copy(x = v.x - (columnContours.thickness * finalStrokeWeight))

                    val vertex2 = v.copy(x = v.x + (columnContours.thickness * finalStrokeWeight))

                    write(vertex1)
                    write(color.toVector4())
                    write(vertex2)
                    write(color.toVector4())

                }
            }
        }
    }

    fun drawColumn(drawer: Drawer) {
        drawer.shadeStyle = shadeStyle {
            fragmentTransform = "x_fill = va_color;"
        }

        drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLE_STRIP)
        vb.destroy()

    }

}