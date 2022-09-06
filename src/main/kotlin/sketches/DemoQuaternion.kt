package sketches

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import extensions.QuaternionCamera
import org.openrndr.PresentationMode
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.noise.uniform
import org.openrndr.math.*
import org.openrndr.math.transforms.perspective
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File

fun main() {
    application {
        configure {
            width = 1920
            height = 1080
            multisample = WindowMultisample.SampleCount(32)
        }
        program {
            program.window.presentationMode = PresentationMode.MANUAL
            //val positions = (0 until 100000).map { Spherical(Double.uniform(-180.0, 180.0), Double.uniform(-180.0, 180.0), 10.0 + Double.uniform(0.0, 2.0)) }

             val points = csvReader().readAllWithHeader(File("data/graph-tsne-d-100-i-100-p25-v2.csv")).map {
                 Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
             }
            val bounds = points.bounds
            val llbounds = Rectangle(-180.0, 0.0, 360.0, 180.0)
            val latlon = points.map { it.map(bounds, llbounds) }

            val positions = latlon.map { Spherical(it.x, it.y, 10.0) }



            val quad = vertexBuffer(
                vertexFormat {
                    position(3)
                },
                4
            )
            quad.put {
                write(Vector3(-1.0, -1.0, 0.0))
                write(Vector3(1.0, -1.0, 0.0))
                write(Vector3(-1.0, 1.0, 0.0))
                write(Vector3(1.0, 1.0, 0.0))
            }

            val offsets = vertexBuffer(
                vertexFormat {
                    attribute("offset", VertexElementType.VECTOR3_FLOAT32)
                    attribute("color", VertexElementType.VECTOR4_FLOAT32)
                },
                positions.size
            )
            offsets.put {
                for (position in positions) {
                    write(position.cartesian)
                    val f = position.radius.map(10.0, 12.0, 0.0,1.0)
                    write(ColorRGBa.RED.toOKLABa().mix(ColorRGBa.BLUE.toOKLABa(),f).toRGBa())
                }
            }

            extend(QuaternionCamera())
            extend {

                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        x_fill.rgb = vi_color.rgb;
                        
                    """.trimIndent()

                    vertexTransform = """
                        x_position.xyz *= 0.01;
                        x_position.xyz += (x_viewMatrix * vec4(i_offset, 1.0)).xyz;
                        
                        x_viewMatrix = mat4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
                    """.trimIndent()
                }

                drawer.vertexBufferInstances(listOf(quad), listOf(offsets), DrawPrimitive.TRIANGLE_STRIP, positions.size)

                drawer.defaults()
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}