import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import kotlin.math.PI
import kotlin.math.cos

class ThickLine(gui: GUI) {

    var columnVb: VertexBuffer? = null
    var rowVb: VertexBuffer? = null
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


    fun write(t: Double, vertexes: List<Vector3>, nSegments: Int, palette: List<List<ColorRGBa>>, isRow: Boolean) {
        Random.resetState()


        val tone1 = if (isRow) palette[0][1] else palette[1][2]
        val tone2 = if (isRow) palette[1][1] else palette[2][2]


        var tempBuffer = vertexBuffer(vertexFormat {
            position(3)
            color(4)
        }, nSegments * 2).apply {

            val penPressure = if (isRow) rowContours.penPressure else columnContours.penPressure
            val thickness = if (isRow) rowContours.thickness else columnContours.thickness
            val turbulenceAmount = if(isRow) rowContours.turbulenceAmount else columnContours.turbulenceAmount
            val turbulenceScale = if(isRow) rowContours.turbulenceScale else columnContours.turbulenceScale

            put {
                vertexes.forEachIndexed { i, vertex ->

                    val pc = i.toDouble() / nSegments.toDouble()

                    val color = tone1.mix(tone2, pc)
                    val turbulence = (simplex(435, vertex * turbulenceScale + t * turbulenceScale) * 0.5 + 0.5) * turbulenceAmount
                    val pp = thickness * cos(pc * PI * 2) * penPressure
                    val finalStrokeWeight = thickness - pp - (turbulence * thickness / 2)

                    val vertex1 = if(isRow) vertex.copy(y = vertex.y - (rowContours.thickness * finalStrokeWeight))
                                  else vertex.copy(x = vertex.x - (columnContours.thickness * finalStrokeWeight))

                    val vertex2 = if(isRow) vertex.copy(y = vertex.y + (rowContours.thickness * finalStrokeWeight))
                                  else vertex.copy(x = vertex.x + (columnContours.thickness * finalStrokeWeight))

                    write(vertex1)
                    write(color.toVector4())
                    write(vertex2)
                    write(color.toVector4())

                }

            }

        }


        if(isRow) {
            rowVb = tempBuffer
        } else {
            columnVb = tempBuffer
        }


    }

    fun draw(drawer: Drawer, isRow: Boolean) {


        drawer.shadeStyle = shadeStyle {
            fragmentTransform = "x_fill = va_color;"
        }

        if(isRow) {

            drawer.vertexBuffer(rowVb!!, DrawPrimitive.TRIANGLE_STRIP)
            rowVb!!.destroy()

        } else {

            drawer.vertexBuffer(columnVb!!, DrawPrimitive.TRIANGLE_STRIP)
            columnVb!!.destroy()

        }


    }
}