package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.math.Vector3
import org.openrndr.math.map

class PointCloud(val drawer: Drawer, positions: List<Vector3>) : Animatable() {

    var focusFactor = 0.0

    fun fadeIn() {
        this::focusFactor.animate(1.0, 500, Easing.CubicInOut)
    }

    fun fadeOut() {
        this::focusFactor.animate(0.0, 500, Easing.CubicInOut)
    }



    private val quad = vertexBuffer(
        vertexFormat {
            position(3)
        },
        4
    ).apply {
        put {
            write(Vector3(-1.0, -1.0, 0.0))
            write(Vector3(1.0, -1.0, 0.0))
            write(Vector3(-1.0, 1.0, 0.0))
            write(Vector3(1.0, 1.0, 0.0))
        }
    }

    private val offsets = vertexBuffer(
        vertexFormat {
            attribute("offset", VertexElementType.VECTOR3_FLOAT32)
            attribute("color", VertexElementType.VECTOR4_FLOAT32)
        },
        positions.size
    ).apply {
        put {
            for (position in positions) {
                write(position)
                val f = position.length.map(10.0, 12.0, 0.0, 1.0)
                //write(ColorRGBa.PINK.toOKLABa().mix(ColorRGBa.BLUE.toOKLABa(), f).toRGBa())
                write(ColorRGBa.GRAY)
            }
        }
    }

    private val shadeStyle = shadeStyle {
        fragmentPreamble = """
            in vec4 x_color;
        """.trimIndent()
        fragmentTransform = """
                        x_fill.rgb = vi_color.rgb * x_color.rgb;
                        
                    """.trimIndent()

        vertexPreamble = """
            out vec4 x_color;
        """.trimIndent()
        vertexTransform = """
                        
                        vec3 voffset = (x_viewMatrix * vec4(i_offset, 1.0)).xyz;
                        
                        
                        x_viewMatrix = mat4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
                        vec4 cp = x_projectionMatrix * vec4(voffset, 1.0);
                        vec2 sp = cp.xy / cp.w;
                        
                        vec2 pp = (sp * 0.5 + 0.5) * vec2(1920.0, 1080.0);

                        float size = 0.01;
                        float distance = length(pp-vec2(1920.0, 1080.0)/2.0);
                        
                        if (distance < 100.0) {
                            size += smoothstep(30.0, 0.0, distance) * 0.02 * p_focusFactor;
                            //x_color = vec4(1.0+p_focusFactor, 1.0+p_focusFactor, 1.0+p_focusFactor, 1.0);
                            x_color = vec4(1.0, 1.0, 1.0, 1.0);
                            x_color.a = 0.1;                        
                        } else {
                            x_color = vec4(1.0, 1.0, 1.0, 1.0);
                        }
                        
                        x_position.xyz *= size;
                        x_position.xyz += voffset;
                        

                        
                    """.trimIndent()

        parameter("focusFactor", focusFactor)
    }

    fun draw() {
        updateAnimation()
        drawer.isolated {
            drawer.shadeStyle = this@PointCloud.shadeStyle
            this@PointCloud.shadeStyle.parameter("focusFactor", focusFactor)
            drawer.depthWrite = false
            drawer.depthTestPass = DepthTestPass.ALWAYS
            drawer.vertexBufferInstances(
                listOf(quad),
                listOf(offsets),
                DrawPrimitive.TRIANGLE_STRIP,
                offsets.vertexCount
            )
            drawer.shadeStyle = null
        }
    }

}
