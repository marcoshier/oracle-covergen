package demonstrator

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.google.gson.Gson
import org.openrndr.KEY_SPACEBAR
import org.openrndr.MouseEventType
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blend.Overlay
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.fx.color.ColorMix
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import textSandbox.Coverlay
import textSandbox.Section
import java.io.File
import java.io.FileReader
import kotlin.math.PI
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }
    program {

        val skipPoints = 142082
        val articleData = Gson().fromJson(FileReader(File("data/mapped-v2r1.json")),Array<Entry>::class.java)
        val entries = articleData.map {
            listOf(it.ogdata["Title"], it.ogdata["Author"], it.ogdata["Faculty"], it.ogdata["Department"], it.ogdata["Date"]) as List<String>
        }.drop(skipPoints)

        println(articleData.indexOfFirst { it.ogdata.isNotEmpty() })

        val latent = CsvReader().readAll(File("offline-data/resolved/cover-latent.csv"))
        var latentPoints = latent.map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }.drop(skipPoints)
        val lb = latentPoints.bounds
        latentPoints = latentPoints.map { it.map(lb, Rectangle(0.0, 0.0, 1000.0, 1000.0)) }

        val tiles = arrayTexture(4096,4096,66)

        for (i in 0 until 66) {
            println("loading image $i")
            val image = loadImage("data/tiles-merged-128-v2/tiling-${String.format("%04d", i)}.png")

            image.copyTo(tiles, i)
            image.destroy()
        }

        val csv = CsvReader().readAll(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).drop(1)
        var points = csv.map {
            Vector2(it[0].toDouble(), it[1].toDouble())
        }.drop(skipPoints)
        val b = points.bounds
        points = points.map { it.map(b, Rectangle(0.0, 0.0, 1000.0, 1000.0)) }

        val overlays = entries.map {
            val initialFrame = Rectangle(0.0, 0.0, 540.0, 960.0)
            val c = Coverlay(initialFrame, it)
            c.subdivide(Section(initialFrame))
            c.unfold()
            c
        }


        println(points.size)

        val tree = points.kdTree()

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

        // transforms
        val transforms = vertexBuffer(vertexFormat {
            //attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            attribute("position0", VertexElementType.VECTOR2_FLOAT32)
            attribute("position1", VertexElementType.VECTOR2_FLOAT32)

        }, nVertex).apply {
            put {
                for (pos in points.indices) {
                    write(points[pos])
                    write(latentPoints[pos])
                }
            }
        }

/*        extend(ScreenRecorder()) {
            maximumDuration = 15.0
            frameRate = 60
        }*/

        val anim = object: Animatable() {
            var k = 0.0
        }

        var dir = 1.0
        keyboard.keyUp.listen {
            if(it.key == KEY_SPACEBAR) {
                anim.apply {
                    animate(::k, dir, 6000, Easing.SineInOut).completed.listen {
                        dir = 1.0 - dir
                    }
                }
            }
        }


        val imageState = object  {

            var image: ColorBuffer? = null

            var activeIndex: Int = -1
                get() {
                    return field
                }
                set(value) {
                    if (value != field) {
                        field = value
                        image?.destroy()
                        if (value != -1) {
                            val testFile = File("data/generated/${String.format("%06d", value + skipPoints)}.png")
                            if (testFile.exists()) {
                                image = loadImage(testFile)
                            } else {
                                image = null
                            }
                        } else {
                            image = null
                        }
                    }
                }

        }


        val c = extend(Camera2D())
        extend(Screenshots()){
            contentScale = 4.0
            trigger()
        }
/*
        extend(Post()) {
            val cc = ColorCorrection()
            cc.gamma = 0.7
            cc.saturation = 1.5
            post { i, o ->
                cc.apply(i, o)
            }

        }

        extend(TemporalBlur()) {
            this.fps = 60.0
            samples = 120
            duration = 1.0
            this.colorMatrix = { t:Double ->
                grayscale(1.0/3.0, 1.0/3.0, 1.0/3.0) * t + Matrix55.IDENTITY * (1.0-t)
            }
        }
*/


        extend {
            anim.updateAnimation()
            val cursorPosition = (c.view.inversed * mouse.position.xy01).div.xy

            val p = tree.findNearest(cursorPosition)
            val i = points.indexOf(p)

            if (p!!.distanceTo(cursorPosition) < 5.0) {
                imageState.activeIndex = i
            } else {
                imageState.activeIndex = -1
            }


            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE.opacify(0.25)


            drawer.translate(drawer.bounds.center)
            drawer.scale(1.0)
            drawer.translate(-drawer.bounds.center)

            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                    x_position.xy = x_position.xy*1.0 + i_position0.xy * p_morph + i_position1.xy * (1.0 - p_morph);                        
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
  
                    
                    x_fill = c;
                """.trimIndent()


                val t = sin(seconds) * 0.5 + 0.5

                parameter("tiles", tiles)
                println(seconds*60.0 + 1.0 )
                parameter("morph", seconds*60.0 + 1.0)
            }
            drawer.drawStyle.blendMode = BlendMode.ADD
            drawer.vertexBufferInstances(listOf(vb), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, nVertex)
            drawer.drawStyle.blendMode = BlendMode.BLEND

            drawer.defaults()/*
            if(imageState.activeIndex != -1) {
                if (imageState.image != null) {
                    drawer.image(imageState.image!!, 1050.0, 0.0)
                }
                //println(imageState.activeIndex)
                try {
                    //println(overlays[imageState.activeIndex].data)

                    drawer.fill = ColorRGBa.WHITE
                    drawer.translate(1050.0, 0.0)
                    val font = loadFont("data/fonts/default.otf", 34.0)
                    drawer.fontMap = font
                    drawer.writer {
                        box = Rectangle(0.0, 0.0, 540.0, 960.0).offsetEdges(-15.0).scaledBy(0.7, 1.0, 0.0, 0.0)
                        newLine()
                        text(overlays[imageState.activeIndex].data[0])
                    }
                    overlays[imageState.activeIndex].draw(drawer)
                } catch(e:Throwable) {


                    e.printStackTrace()
                }
            }*/

        }
    }

}
