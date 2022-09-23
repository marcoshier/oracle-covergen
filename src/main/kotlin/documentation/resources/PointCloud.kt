package documentation.resources

import components.DataModel
import components.DateFilterModel
import components.FacultyFilterModel
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.map

class PointCloud(val drawer: Drawer, dataModel: documentation.resources.DataModel, val filterModel: documentation.resources.FacultyFilterModel, var dateModel: DateFilterModel) : Animatable() {

    val positions = dataModel.points
    val tiles = arrayTexture(4096,4096,2)
    val facultyIndexes = dataModel.facultyIndexes
    val years = dataModel.years

    init {
        for (i in 0 until 2) {
            val image = loadImage("offline-data/tiles/tiling-${String.format("%04d", i)}.png")

            image.copyTo(tiles, i)
            image.destroy()
        }
        tiles.filterMag = MagnifyingFilter.NEAREST
    }


    var focusFactor = 1.0

    fun fadeIn() {
        this::focusFactor.animate(1.0, 500, Easing.CubicInOut)
    }

    fun fadeOut() {
        this::focusFactor.animate(0.0, 500, Easing.CubicInOut)
    }



    private val quad = vertexBuffer(
        vertexFormat {
            position(3)
            textureCoordinate(2)
        },
        4
    ).apply {
        put {
            write(Vector3(-1.0, -1.0, 0.0))
            write(Vector2(0.35, 0.35))
            write(Vector3(1.0, -1.0, 0.0))
            write(Vector2(0.65, 0.35))
            write(Vector3(-1.0, 1.0, 0.0))
            write(Vector2(0.35, 0.65))
            write(Vector3(1.0, 1.0, 0.0))
            write(Vector2(0.65, 0.65))
        }
    }

    private val offsets = vertexBuffer(
        vertexFormat {
            attribute("offset", VertexElementType.VECTOR3_FLOAT32)
            attribute("color", VertexElementType.VECTOR4_FLOAT32)
            attribute("fac0", VertexElementType.VECTOR4_FLOAT32)
            attribute("fac1", VertexElementType.VECTOR4_FLOAT32)
            attribute("fac2", VertexElementType.VECTOR4_FLOAT32)
            attribute("year", VertexElementType.FLOAT32)
        },
        positions.size
    ).apply {
        put {
            for ((index, position) in positions.withIndex()) {
                write(position)
                val f = position.length.map(10.0, 12.0, 0.0, 1.0)
                //write(ColorRGBa.PINK.toOKLABa().mix(ColorRGBa.BLUE.toOKLABa(), f).toRGBa())
                //write(ColorRGBa.GRAY)

                val activeFaculty = facultyIndexes[index].let { if (it == -1) 9 else it }
                write(dataModel.facultyColors.getOrNull(activeFaculty)?:ColorRGBa.WHITE)

                for (j in 0 until 9) {
                    val value = if (j == activeFaculty) 1 else 0
                    write(value.toFloat())
                }
                write(0.0f)
                write(0.0f)
                write(0.0f)


                val activeYear = years[index]
                //println(activeYear)
                write(activeYear)
            }
        }
    }

    var shadeStyle = shadeStyle {
        fragmentPreamble = """
            in vec4 x_color;
        """.trimIndent()
        fragmentTransform = """
                          
                    
                    int i = c_instance % 256;
                    int j = (c_instance / 256)%256;
                    int k = c_instance / (256*256);
                    
                    vec2 uv = va_texCoord0 / 256.0;
                    uv.x += i/256.0;
                    uv.y += j/256.0;
                    uv.y = 1.0 - uv.y;
                    
                    float dx = abs(va_texCoord0.x-0.5);
                    float dy = abs(va_texCoord0.y-0.5);

                    float sdx = smoothstep(0.44,0.5, dx);
                    float sdy = smoothstep(0.44,0.5, dy);
                        
                                        
                                                                                
                                                            
                    vec4 c = texture(p_tiles, vec3(uv, k));
  
                    float cd = dot(c.rgb, vec3(0.33,0.33,0.33));
                    vec4 m = vec4(cd, cd, cd, 1.0);
                    vec4 mc = mix(c, m, 0.5);
                                        
                    x_fill = mc * x_color;
                    
                    """.trimIndent()

        vertexPreamble = """
            out vec4 x_color;
        """.trimIndent()
        vertexTransform = """
                        vec3 voffset = (x_viewMatrix * vec4(i_offset, 1.0)).xyz;
                        
                        x_viewMatrix = mat4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
                        vec4 cp = x_projectionMatrix * vec4(voffset, 1.0);
                        vec2 sp = cp.xy / cp.w;
                        
                        vec2 pp = (sp * 0.5 + 0.5) * vec2(2880.0, 1920.0);

                        float size = 0.05;
                        float distance = length(pp-vec2(2880.0, 1920.0)/2.0);
                        
                        x_color = i_color;
                        if (distance < p_radius) {  
                            float d = smoothstep(p_radius + 100.0, 0.0, distance);
                            x_color.a = d;                     
                        } else {
                            x_color.a = 0.0;
                        }
                        

                        float f = 0.0;
                        for (int i = 0; i < 4; ++i) {
                            if (i_fac0[i] > 0.5) {
                                f += p_filterFades[i];                                                                                
                            }
                        }
                        for (int i = 0; i < 4; ++i) {
                            if (i_fac1[i] > 0.5) {
                                f += p_filterFades[i+4];                                                                                
                            }
                        }
                        
                        for (int i = 0; i < 1; ++i) {
                            if (i_fac2[i] > 0.5) {
                                f += p_filterFades[i+8];                                                                                
                            }
                        }
                        
                        float y0 = i_year - p_yearRange[0];
                        float y1 = p_yearRange[1] - i_year; 
                        
                        
                        float d = smoothstep(-1.0, 0.0, y0) * smoothstep(-1.0, 0.0, y1);
                       
                        x_color.a *= (f * d * 0.99 + 0.01);                        
                        x_position.xyz *= size;
                        x_position.xyz += voffset;
                        
                    """.trimIndent()



        parameter("tiles", tiles)
        parameter("focusFactor", focusFactor)
    }

    var radius = 0.1

    fun draw() {
        updateAnimation()
        drawer.isolated {

            val filterFades = filterModel.states.map { it.fade }.toDoubleArray()
            require(filterFades.size == 9)

            this@PointCloud.shadeStyle.parameter("radius", radius)
            this@PointCloud.shadeStyle.parameter("filterFades", filterFades)
            this@PointCloud.shadeStyle.parameter("yearRange", dateModel.range().toDoubleArray())

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
