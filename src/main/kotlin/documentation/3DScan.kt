package documentation

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import documentation.resources.DataModel
import org.openrndr.KEY_SPACEBAR
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.camera.ProjectionType
import org.openrndr.extra.color.presets.DARK_ORANGE
import org.openrndr.extra.fx.Post
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.noise.Random
import org.openrndr.extra.objloader.loadOBJ
import org.openrndr.extra.objloader.loadOBJEx
import org.openrndr.extra.objloader.loadOBJasVertexBuffer
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class Triangle3D(val x1:Vector3, val x2:Vector3, val x3: Vector3, val uv0:Vector2, val uv1:Vector2, val uv2:Vector2) {

    fun randomPoint(random: kotlin.random.Random = kotlin.random.Random.Default): Pair<Vector3, Vector2> {
        val u = random.nextDouble()
        val v = random.nextDouble()
        val su0 = sqrt(u)
        val b0 = 1.0 - su0
        val b1 = v * su0
        val b = Vector3(b0, b1, 1.0 - b0 - b1)
        return Pair(x1 * b.x + x2 * b.y + x3 * b.z, uv0 * b.x + uv1 * b.y + uv2 * b.z)
    }

}

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {

        val csv = CsvReader().readAll(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).drop(1)
        var points = csv.map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }
        val bounds = points.bounds
        points.map(bounds, Rectangle(-5.0, -1.0, 11.0, 11.0))
        val kd = points.kdTree()

        val meshes = (1..4).map { loadOBJ(File("data/meshes/Floor$it/textured_output.obj")).toList().map { it.second }.flatten() }
        val textures = (1..4).map { loadImage("data/meshes/Floor$it/textured_output.jpg") }

        val vbs = meshes.map { lt ->


            val triedges = lt.map { Triangle3D(it.positions[0], it.positions[1], it.positions[2], it.textureCoords[0], it.textureCoords[1], it.textureCoords[2]) }

            val randomPoints = triedges.flatMap { t -> (0 until 10).map { t.randomPoint() } }


            val gb = vertexBuffer(vertexFormat {
                position(3)
            }, 4).apply {
                put {
                    write(Vector3(-1.0, -1.0, 0.0))
                    write(Vector3(1.0, -1.0, 0.0))
                    write(Vector3(-1.0, 1.0, 0.0))
                    write(Vector3(1.0, 1.0, 0.0))
                }
            }

            val ib = vertexBuffer(vertexFormat {
                attribute("position1", VertexElementType.VECTOR3_FLOAT32)
                attribute("texCoord", VertexElementType.VECTOR2_FLOAT32)
                attribute("position2", VertexElementType.VECTOR3_FLOAT32)
            }, randomPoints.size).apply {
                put {
                    for(rp in randomPoints) {
                        //val p2 = kd.findNearest(rp.first.xy)

                        write(rp.first)
                        write(rp.second)
                        write(points.random().vector3(z = 0.0))

                    }
                }
            }

            Pair(gb, ib)

        }

        var morph = object : Animatable() {
            var amt = 0.0
        }

        keyboard.keyUp.listen {
            if (it.key == KEY_SPACEBAR) {
                morph.apply {
                    ::amt.animate(1.0 - amt, 4000, Easing.CubicInOut)
                }
            }
        }

        val c = extend(Orbital()) {
            projectionType = ProjectionType.ORTHOGONAL
            eye = Vector3.UNIT_Z * 25.0
            camera.magnitude = 10.0
            camera.magnitudeEnd = 10.0
            dampingFactor = 0.0
        }


        extend(ScreenRecorder())

        extend {
            morph.updateAnimation()
            drawer.clear(ColorRGBa.fromHex("#3636e5"))
            println(c.camera.magnitude)

            vbs.forEachIndexed { i, (vb, ib) ->
                    drawer.shadeStyle = shadeStyle {
                        vertexTransform = """
                            float size = 0.0025 + (0.03 * p_morph);
                            x_position.xyz = x_position.xyz * size + i_position1 * (1.0 - p_morph) + i_position2.xyz * p_morph;
                        """.trimIndent()
                        fragmentTransform = """
                            x_fill = texture(p_tex, vi_texCoord.xy);
                        """.trimIndent()

                        parameter("morph", morph.amt)
                        parameter("tex", textures[i])
                    }

                    drawer.pushTransforms()
                    drawer.translate(0.0, i * 2.1 * (1.0 - morph.amt), 0.0)
                    drawer.vertexBufferInstances(listOf(vb), listOf(ib), DrawPrimitive.TRIANGLE_STRIP, ib.vertexCount)
                    drawer.popTransforms()

                }




        }
    }
}