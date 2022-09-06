package demonstrator

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }
    program {
        var latentPoints = CsvReader().readAll(File("offline-data/resolved/cover-latent.csv")).map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }
        latentPoints = latentPoints.map { it.map(latentPoints.bounds, drawer.bounds) }

        var points = CsvReader().readAll(File("data/graph-tsne-d-100-i-100-p25-v2.csv")).drop(1).map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }
        points = points.map { it.map(points.bounds, drawer.bounds) }


        val nVertex = 8000

        // geometry
        val vb = vertexBuffer(vertexFormat {
            position(3)
        }, 4).apply {
            put {
                write(Vector3(-1.0, -1.0, 0.0))
                write(Vector3(-1.0, 1.0, 0.0))
                write(Vector3(1.0, -1.0, 0.0))
                write(Vector3(1.0, 1.0, 0.0))
            }
        }

        // transforms
        val transforms = vertexBuffer(vertexFormat {
            //attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            attribute("position0", VertexElementType.VECTOR2_FLOAT32)
            attribute("position1", VertexElementType.VECTOR2_FLOAT32)

        }, nVertex).apply {
            put {
                for (pos in 0..nVertex) {
                    write(points[pos])
                    write(latentPoints[pos])
                }
            }
        }

/*        extend(ScreenRecorder()) {
            maximumDuration = 10.0
            frameRate = 60
        }*/

        extend(Orbital()) {
            eye = Vector3.UNIT_Z * 100.0
        }



        extend {

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE.opacify(0.25)

            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                    x_position.xy = x_position.xy + i_position0.xy * p_morph + i_position1.xy * (1.0 - p_morph);                        
                """

                parameter("morph", (sin(seconds * 0.1 * 2 * PI) * 0.5 + 0.5))
            }
            drawer.vertexBufferInstances(listOf(vb), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, nVertex)
        }
    }

}
