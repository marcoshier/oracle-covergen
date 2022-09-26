package documentation

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.google.gson.Gson
import components.skipPoints
import demonstrator.Camera2D
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import java.io.FileReader
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1000
        height = 1000
        position = IntVector2(1200, -1700)
    }
    program {

        val tiles = arrayTexture(4096,4096,66).also {
            for (i in 0 until 66) {
                println(i)
                val image = loadImage("offline-data/tiles/tiles-merged-128-v2/tiling-${String.format("%04d", i)}.png")

                image.copyTo(it, i)
                image.destroy()
            }
        }

        val csv = CsvReader().readAll(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).drop(1)
        val protoIndexes = CsvReader().readAll(File("offline-data/resolved/proto-row-idx.csv")).map { it[0] }

       var protoPoints = csv.filterIndexed { i, _ -> protoIndexes.contains(i.toString()) }.map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }
        val pb = protoPoints.bounds
        protoPoints = protoPoints.map { it.map(pb, drawer.bounds) }

        class protoAnimatable(delay: Long = 0L): Animatable() {
            var opacity = 0.0
        }
        val pa = protoAnimatable()

        pa.apply {
            ::opacity.animate(250.0, 15000, Easing.QuadIn, 4000)
        }



        var points = csv.map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }.drop(skipPoints)
        val b = points.bounds
        points = points.map { it.map(b, drawer.bounds) }

        val tree = protoPoints.kdTree()
        val nVertex = points.size

        // geometry
        val vb = vertexBuffer(vertexFormat {
            position(3)
            textureCoordinate(2)
        }, 4).apply {
            put {
                write(Vector3(-1.0, -1.0, 0.0))
                write(Vector2(0.0, 1.0))
                write(Vector3(-1.0, 1.0, 0.0))
                write(Vector2(0.0, 0.0))
                write(Vector3(1.0, -1.0, 0.0))
                write(Vector2(1.0, 1.0))
                write(Vector3(1.0, 1.0, 0.0))
                write(Vector2(1.0, 0.0))
            }
        }

        val transforms = vertexBuffer(vertexFormat {
            attribute("position", VertexElementType.VECTOR2_FLOAT32)
            attribute("closestProto", VertexElementType.VECTOR2_FLOAT32)
            attribute("opacity", VertexElementType.FLOAT32)
        }, nVertex).apply {
            put {
                for (pos in points) {
                    // find closest protovisual point
                    val nearestProto = tree.findNearest(pos)

                    nearestProto?.let {
                        if(nearestProto != pos) {
                            write(pos)
                            write(nearestProto)
                        } else {
                            write(pos)
                            write(Vector2.ZERO)
                        }
                    }
                    write(1.0f)
                }
            }
        }



        extend(Camera2D())

        extend {
            pa.updateAnimation()

            drawer.shadeStyle = shadeStyle {

                fragmentPreamble = """
                    in float x_opacity;
                """.trimIndent()

                fragmentTransform = """
                    int i = c_instance % 32;
                    int j = (c_instance / 32)%32;
                    int k = c_instance / (32*32);

                    vec2 uv = va_texCoord0 / 32.0;
                    uv.x += i/32.0;
                    uv.y += j/32.0;
                    uv.y = 1.0 - uv.y;

                    float dx = abs(va_texCoord0.x-0.5);
                    float dy = abs(va_texCoord0.y-0.5);

                    float sdx = smoothstep(0.44,0.5, dx);
                    float sdy = smoothstep(0.44,0.5, dy);

                    vec4 c = texture(p_tiles, vec3(uv, k));
                    
                    
                    x_fill = c * x_opacity;
                 
                """.trimIndent()


                vertexPreamble = """
                    out float x_opacity;
                """.trimIndent()

                vertexTransform = """
                    x_position.xy = x_position.xy + i_position.xy;
                    
                    float distance = length(x_position.xy - i_closestProto.xy);
                    
                    if(i_closestProto.xy != vec2(0, 0)) {
                         if (distance < p_radius) {  
                            x_opacity = smoothstep(p_radius, 0.0, distance);       
                        } else {
                            x_opacity = 0.0;
                        }
                    } else {
                        x_opacity = 1.0;
                    }
                   
                """.trimIndent()




                val t = sin(seconds) * 0.5 + 0.5

                parameter("tiles", tiles)
                parameter("radius", pa.opacity)
            }
            drawer.vertexBufferInstances(listOf(vb), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, nVertex)
            drawer.shadeStyle = null

            drawer.fill = ColorRGBa.RED
            drawer.stroke = null
            //drawer.circles(protoPoints, 1.0)

        }
    }
}