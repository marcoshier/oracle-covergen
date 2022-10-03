package documentation

import animatedCover.opacify
import documentation.resources.coverlayProxy
import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.camera.ProjectionType
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.uniform
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.h264
import org.openrndr.math.*
import org.openrndr.math.transforms.transform

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {
        val n = 20 * 10 * 12 * 12
        val book = boxMesh(0.4, 2.0, 0.8)

        val transforms = vertexBuffer(vertexFormat {
            attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            attribute("transform2", VertexElementType.MATRIX44_FLOAT32)
        }, n)
        transforms.put {
            val bookPositions = (-10 until 10).flatMap { x -> (-5 until 5).map { y -> IntVector2(x, y) } }
            val shelfPositions = (-6 until 6).flatMap { x -> (-6 until 6).map { y -> IntVector2(x, y) } }

            for(sp in shelfPositions) {
                for(bp in bookPositions) {

                    var scale  = gaussian(1.0, (bp.x - 20) * 0.025)
                    var scale2 = gaussian(1.0, (bp.x - 20) * 0.325)

                    write(transform {
                        scale(Vector3.ONE.copy(x = scale))
                        translate(Vector2(bp.x * 0.41, bp.y * 2.1) + Vector2(sp.x * 8.0 + (sp.x * 4.0), sp.y * 20.0  + (sp.y * 4.0)) - Vector2(sp.x * 2.0, sp.y * 2.0))
                    })
                    write(transform {

                        scale(Vector3.ONE.copy(x = scale2))
                        translate(Vector2(bp.x * 0.42, bp.y * 2.1) + Vector2(sp.x * 8.0 + (sp.x * 4.0), sp.y * 20.0  + (sp.y * 4.0)) - Vector2(sp.x * 2.0, sp.y * 2.0))
                    })
                }
            }

        }

        val proxyProgram = libraryScreen(application)
        val rt = renderTarget(3960, 1920, multisample = BufferMultisample.SampleCount(32)) {
            colorBuffer()
            depthBuffer()
        }
        val resolved = colorBuffer(3960, 1920)


        val animatables = object: Animatable() {
            var deviation = 0.0
            var zoom = 100.0
            var rotation = 0.0
            var zOffset = 0.0
            var screenOpacity = 0.0
            var screenHeight = 0.0
            var dummy = 0.0
            var booksOpacity = 1.0

            var currentSet = Array(40) { Random.int0(n) }


            fun newSet() {
                ::zOffset.cancel()
                ::zOffset.animate(0.0, 1500, Easing.CubicInOut).completed.listen {
                    currentSet = Array(100) { Random.int0(n) }
                    ::zOffset.animate(2.0, 1500, Easing.CubicInOut)
                }

            }
        }



        val c = extend(Orbital()) {
            projectionType = ProjectionType.ORTHOGONAL
            eye = Vector3.UNIT_Z * -5.0
            near = 0.1
        }
        extend(Screenshots())
        extend(ScreenRecorder()) {
            h264 {
                constantRateFactor = 30
            }
        }


        val pointsChanged: (() -> Unit) by proxyProgram.userProperties
        fun start() {
            animatables.apply {
                ::deviation.animate(0.97, 8500, Easing.CubicOut)
                ::deviation.animate(1.0, 18500, Easing.SineInOut, predelayInMs = 8500)
                ::dummy.animate(0.5, 2000, predelayInMs = 4500).completed.listen {
                    zoom = 80.0
                    ::zoom.animate(60.0, 1500, Easing.CubicOut).completed.listen {
                        zoom = 60.0
                        ::rotation.animate(1.0, 1500, Easing.CubicOut)
                        ::rotation.complete()
                        ::screenHeight.animate(1.0, 2250, Easing.CubicInOut, 200).completed.listen {
                            ::screenOpacity.animate(1.0, 1250, predelayInMs = 100)
                        }
                        // select books
                        ::dummy.animate(0.0, 20000).completed.listen {
                            ::booksOpacity.animate(0.0, 2500, Easing.CubicInOut)
                            ::zoom.animate(30.0, 2500, Easing.CubicOut)
                        }
                    }
                }
            }
        }

        start()

        extend {
            animatables.updateAnimation()

            c.camera.setView(Vector3.ZERO, Spherical(-219.0 + (39.0 * animatables.rotation) , 45.0 + (45.0 * animatables.rotation), 5.0), c.fov)
            c.camera.magnitude = animatables.zoom

            drawer.clear(ColorRGBa.fromHex("#3030ff").shade(1.0 - animatables.rotation))
            drawer.shadeStyle = shadeStyle {

                vertexPreamble = """
                    out vec4 x_color;
                """.trimIndent()

                vertexTransform = """
                    mat4 transform = (i_transform * p_mix + i_transform2 * (1.0 - p_mix));
                    
//                    for (int i = 0; i <= 100; ++i) {
//                        if(c_instance == p_set[i]) {
//                            transform[3][2] = -p_zOffset;
//                        }
//                    }
                    
                    if(i_transform[3][0] >= -49.5 && i_transform[3][0] <= 49.5 && i_transform[3][1] >= -24.0 && i_transform[3][1] <= 24.0) {
                        x_color = vec4(1.0, 1.0, 0.0, 1.0);
                    } else {
                        x_color = vec4(1.0, 1.0, 1.0, 1.0);
                    }
                    
                    x_viewMatrix = x_viewMatrix * transform;
                """.trimIndent()


                fragmentPreamble = """
                    in vec4 x_color;
                """.trimIndent()

                fragmentTransform = """
                    vec4 c = x_color * clamp(v_viewNormal.z, 0.25, 1.0 - (p_rotation / 3)) * p_booksOpacity;
                    
 /*                   for (int i = 0; i <= 100; ++i) {
                        if(c_instance == p_set[i]) {
                            c = vec4(1.0, 1.0, 0.0, 0.7);
                        }
                    }
                    */
                    x_fill.rgba = c;
                """.trimIndent()

                parameter("booksOpacity", animatables.booksOpacity)
                parameter("rotation", animatables.rotation)
                parameter("zOffset", animatables.zOffset)
                parameter("set", animatables.currentSet.toIntArray())
                parameter("mix", animatables.deviation)
            }
            drawer.vertexBufferInstances(listOf(book), listOf(transforms), DrawPrimitive.TRIANGLES, n)
            drawer.shadeStyle = null

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.fromHex("#3030FF"))
                drawer.defaults()
                drawer.depthWrite = false
                drawer.depthTestPass = DepthTestPass.ALWAYS

                proxyProgram.drawImpl()
            }
            rt.colorBuffer(0).copyTo(resolved)


            drawer.translate(49.5, 24.0, -0.8)
            drawer.scale(0.025)
            drawer.rotate(-180.0)

            drawer.opacify(animatables.screenOpacity)
            drawer.image(resolved)
            drawer.shadeStyle = null


            drawer.stroke = null
            drawer.depthWrite = false
            drawer.depthTestPass = DepthTestPass.ALWAYS

            drawer.translate(0.0, 0.0, -0.8)
            drawer.fill = ColorRGBa.YELLOW.opacify(0.8 - animatables.screenOpacity)
            drawer.rectangle(0.0, 0.0, 3960.0, 1920 * animatables.screenHeight)

        }
    }
}