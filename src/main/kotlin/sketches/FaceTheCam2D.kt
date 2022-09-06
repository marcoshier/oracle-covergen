package demonstrator

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.google.gson.Gson
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import textSandbox.Coverlay
import textSandbox.Section
import java.io.File
import java.io.FileReader

fun main() = application {
    configure {
        width = 960 + 540
        height = 960
    }
    program {
        val skipPoints = 142082

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


        fun prepareTiles(n: Int = 66): ArrayTexture {
            val tiles = arrayTexture(4096,4096,n)
            for (i in 0 until n) {
                println("loading image $i")
                val image = loadImage("data/tiles-merged-128-v2/tiling-${String.format("%04d", i)}.png")

                image.copyTo(tiles, i)
                image.destroy()
            }
            return tiles
        }
        val tiles = prepareTiles(1)

        val csv = CsvReader().readAll(File("data/graph-tsne-d-100-i-100-p25-v2.csv")).drop(1)
        var points = csv.map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }.drop(skipPoints)
        val b = points.bounds
        points = points.map { it.map(b, Rectangle(0.0, 0.0, height * 1.0, height * 1.0)) }

        val tree = points.kdTree()


        // geometry
        val nVertex = points.size
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
            //attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            attribute("position", VertexElementType.VECTOR2_FLOAT32)
        }, nVertex).apply {
            put {
                for (pos in points.indices) {
                    write(points[pos])
                }
            }
        }

        val imageState = object  {
            var activeOverlay: Coverlay? = null

            var activeIndex: Int = -1
                get() {
                    return field
                }
                set(value) {
                    if (value != field) {
                        field = value
                        if (value != -1) {
                            activeOverlay = overlays[value]
                            activeOverlay?.backgroundImage?.destroy()
                            val testFile = File("data/generated/${String.format("%06d", value + skipPoints)}.png")
                            if (testFile.exists()) {
                                //TODO there should be a 0.5s debounce or so before this happens
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

        extend(ScreenRecorder()) {

            maximumDuration = 15.0
        }

        val c = extend(Camera2D())
        extend {
            val cursorPosition = (c.view.inversed * mouse.position.xy01).div.xy

            val p = tree.findNearest(cursorPosition)
            val i = points.indexOf(p)

            if (p!!.distanceTo(cursorPosition) < 5.0) {
                imageState.activeIndex = i
            } else {
                imageState.activeIndex = -1
            }



            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = null
            drawer.rectangle(points.bounds)

            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                    float scale = 1.0;
                    x_position.xy = x_position.xy * scale + i_position.xy;                        
                """

                fragmentTransform = """
                    //c_instance;
                    
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
            drawer.vertexBufferInstances(listOf(vb), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, nVertex)

            drawer.defaults()
            drawer.translate(960.0, 0.0)
            if(imageState.activeIndex != -1) {
                if (imageState.activeOverlay != null) {
                    try {
                        overlays[imageState.activeIndex].draw(drawer)
                    } catch(e:Throwable) {
                        e.printStackTrace()
                    }
                }
            }

        }
    }

}
