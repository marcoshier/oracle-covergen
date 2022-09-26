package documentation.resources.coverlayResources

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos

class ThickLine(val drawer: Drawer, val columns: List<List<Vector3>>, val sliders: Sliders) {

    private val heightSegments = columns.size
    private val rotSegments =  columns[0].size


    private val vb = vertexBuffer(vertexFormat {
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
                        val turbulence = (simplex(435, v * sliders.rowContours.turbulenceScale + t * sliders.rowContours.turbulenceScale) * 0.5 + 0.5) * sliders.rowContours.turbulenceAmount
                        val pp = sliders.rowContours.thickness * cos(pc * PI * 2) * sliders.rowContours.penPressure
                        val finalStrokeWeight = sliders.rowContours.thickness - pp - (turbulence * sliders.rowContours.thickness / 2)

                        val vertex1 = v.copy(y = v.y - (sliders.rowContours.thickness * finalStrokeWeight))

                        val vertex2 = v.copy(y = v.y + (sliders.rowContours.thickness * finalStrokeWeight))

                        write(vertex1)
                        write(color.toVector4())
                        write(vertex2)
                        write(color.toVector4())

                }
            }
        }
    }
    fun drawVertebrae() {
        drawer.shadeStyle = shadeStyle {
            fragmentTransform = "x_fill = va_color;"
        }

        drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLE_STRIP)
        vb.destroy()

    }


    fun writeColumns(palette: List<List<ColorRGBa>>, t: Double) {

        val tone1 = palette[1][2]
        val tone2 = palette[2][2]

        vb.put {
            columns.forEach {points ->
                points.forEachIndexed { i, v ->

                    val pc = i.toDouble() / rotSegments.toDouble()

                    val color = tone1.mix(tone2, pc)
                    val turbulence = (simplex(435, v * sliders.columnContours.turbulenceScale + t *  sliders.columnContours.turbulenceScale) * 0.5 + 0.5) *  sliders.columnContours.turbulenceAmount
                    val pp =  sliders.columnContours.thickness * cos(pc * PI * 2) *  sliders.columnContours.penPressure
                    val finalStrokeWeight =  sliders.columnContours.thickness - pp - (turbulence *  sliders.columnContours.thickness / 2)

                    val vertex1 = v.copy(x = v.x - ( sliders.columnContours.thickness * finalStrokeWeight))

                    val vertex2 = v.copy(x = v.x + ( sliders.columnContours.thickness * finalStrokeWeight))

                    write(vertex1)
                    write(color.toVector4())
                    write(vertex2)
                    write(color.toVector4())

                }
            }
        }
    }
    fun drawColumn() {
        drawer.shadeStyle = shadeStyle {
            fragmentTransform = "x_fill = va_color;"
        }

        drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLE_STRIP)
        vb.destroy()

    }

}