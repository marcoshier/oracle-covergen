package sketches

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import extensions.QuaternionCameraSmooth
import org.openrndr.PresentationMode
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import textSandbox.Coverlay
import textSandbox.Section
import java.io.File
import java.io.FileReader

fun main() {
    application {
        configure {
            width = 1920 / 2 + 540
            height = 1080 / 2
            multisample = WindowMultisample.SampleCount(32)
        }
        program {
            program.window.presentationMode = PresentationMode.MANUAL
            val skipPoints = 142082

            // article data
            val articleData = Gson().fromJson(FileReader(File("data/mapped-v2r1.json")),Array<Entry>::class.java)
            val entries = articleData.map {
                listOf(it.ogdata["Title"], it.ogdata["Author"], it.ogdata["Faculty"], it.ogdata["Department"], it.ogdata["Date"]) as List<String>
            }.drop(skipPoints)
            val overlays = entries.mapIndexed { i, it ->
                val initialFrame = Rectangle(0.0, 0.0, 540.0, 960.0)
                val c = Coverlay(initialFrame, it).apply {
                    val s = Section(initialFrame)
                    subdivide(s)
                }
                c
            }

            // prepare points
            val points = csvReader().readAllWithHeader(File("data/graph-tsne-d-100-i-100-p25-v2.csv")).map {
                Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
            }.drop(skipPoints)
            val bounds = points.bounds
            val llbounds = Rectangle(-180.0, 0.0, 360.0, 180.0)
            val latlon = points.map { it.map(bounds, llbounds) }

            val positions = latlon.map { Spherical(it.x, it.y, 10.0) }

            // prepare tiles
            val tiles = arrayTexture(4096,4096,66)
            for (i in 0 until 1) {
                println("loading image $i")
                val image = loadImage("data/tiles-merged-128-v2/tiling-${String.format("%04d", i)}.png")

                image.copyTo(tiles, i)
                image.destroy()
            }



            val quad = vertexBuffer(
                vertexFormat {
                    position(3)
                    textureCoordinate(2)
                },
                4
            )
            quad.put {
                write(Vector3(-1.0, -1.0, 0.0))
                write(Vector2(0.0, 1.0))
                write(Vector3(-1.0, 1.0, 0.0))
                write(Vector2(0.0, 0.0))
                write(Vector3(1.0, -1.0, 0.0))
                write(Vector2(1.0, 1.0))
                write(Vector3(1.0, 1.0, 0.0))
                write(Vector2(1.0, 0.0))
            }

            val vertices = mutableListOf<Vector3>()
            val offsets = vertexBuffer(
                vertexFormat {
                    attribute("offset", VertexElementType.VECTOR3_FLOAT32)
                },
                positions.size
            )
            offsets.put {
                for (position in positions) {
                    val c = position.cartesian
                    vertices.add(c)
                    write(c)
                }
            }

            val tree = vertices.kdTree()
            val coverlay = object  {
                var activeOverlay: Coverlay? = null

                var activeIndex: Int = -1
                    set(value) {
                        if (value != field) {
                            field = value
                            if (value != -1) {
                                activeOverlay = overlays[value]
                                activeOverlay?.backgroundImage?.destroy()
                                val testFile = File("data/generated/${String.format("%06d", value + skipPoints)}.png")
                                if (testFile.exists()) {
                                    activeOverlay!!.backgroundImage = loadImage(testFile)
                                    activeOverlay!!.unfold()
                                } else {
                                    activeOverlay!!.backgroundImage = null
                                }
                            } else {
                                activeOverlay = null
                            }
                        }
                    }

            }

            val neighbors = object {
                var activeIndexes = listOf<Int>()
                    set(value) {
                        if(value != field) {
                            field = value
                            if(activeIndexes.isNotEmpty()) {
                                // highlight them in the shader
                                // make minified covers (with static images and title) for each
                            }
                        }
                    }
            }

            val qcs = QuaternionCameraSmooth()
            extend(qcs)
            extend {

                val radius = 100.0
                val pointOnSphere = (drawer.view * Vector4(0.0, 0.0, -10.0,1.0)).xyz
                val closestPoint = tree.findNearest(pointOnSphere)
                val pointsInRadius = tree.findAllInRadius(pointOnSphere, radius)

                if(closestPoint!!.distanceTo(pointOnSphere) < 5.0) {
                    coverlay.activeIndex = vertices.indexOf(closestPoint)
                } else {
                    coverlay.activeIndex = -1
                }

                if(pointsInRadius.isNotEmpty()) {
                    neighbors.activeIndexes = pointsInRadius.map { vertices.indexOf(it) }
                } else {
                    neighbors.activeIndexes = listOf()
                }

                drawer.shadeStyle = shadeStyle {

                    vertexTransform = """
                        x_position.xyz *= 0.01;
                        x_position.xyz += (x_viewMatrix * vec4(i_offset, 1.0)).xyz;
                        
                        x_viewMatrix = mat4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
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
                        c.b += sdx + sdy;
                        c.a += sdx + sdy;
                        
                        x_fill = c;
                    """.trimIndent()


                    parameter("tiles", tiles)
                }

                drawer.vertexBufferInstances(listOf(quad), listOf(offsets), DrawPrimitive.TRIANGLE_STRIP, positions.size)

                drawer.defaults()
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(drawer.bounds.center, radius)

                drawer.defaults()
                if(coverlay.activeIndex != -1) {
                    if (coverlay.activeOverlay != null) {
                        try {
                            drawer.translate(drawer.width - 240.0, 480.0)
                            drawer.scale(0.5)
                            drawer.translate(-540.0, -960.0)
                            //TODO something needs to be fixed in the extension with the manual drawing
                            overlays[coverlay.activeIndex].draw(drawer)
                        } catch(e:Throwable) {
                            e.printStackTrace()
                        }
                    }
                }


            }
        }
    }
}