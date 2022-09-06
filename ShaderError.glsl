#version 450 core
// <primitive-types> (ShadeStyleGLSL.kt)
#define d_vertex_buffer 0
#define d_image 1
#define d_circle 2
#define d_rectangle 3
#define d_font_image_map 4
#define d_expansion 5
#define d_fast_line 6
#define d_mesh_line 7
#define d_point 8
#define d_custom 9
#define d_primitive d_vertex_buffer
// </primitive-types>

uniform sampler2DArray p_tiles;
uniform float p_morph; 
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
// <drawer-uniforms(true, true)> (ShadeStyleGLSL.kt)
            
layout(shared) uniform ContextBlock {
    uniform mat4 u_modelNormalMatrix;
    uniform mat4 u_modelMatrix;
    uniform mat4 u_viewNormalMatrix;
    uniform mat4 u_viewMatrix;
    uniform mat4 u_projectionMatrix;
    uniform float u_contentScale;
    uniform float u_modelViewScalingFactor;
    uniform vec2 u_viewDimensions;
};
            
layout(shared) uniform StyleBlock {
    uniform vec4 u_fill;
    uniform vec4 u_stroke;
    uniform float u_strokeWeight;
    uniform float[25] u_colorMatrix;
};
// </drawer-uniforms>
in vec3 va_position;
in vec2 va_texCoord0;
in vec2 vi_position0;
in vec2 vi_position1;


// <transform-varying-in> (ShadeStyleGLSL.kt)
in vec3 v_worldNormal;
in vec3 v_viewNormal;
in vec3 v_worldPosition;
in vec3 v_viewPosition;
in vec4 v_clipPosition;
flat in mat4 v_modelNormalMatrix;
// </transform-varying-in>

out vec4 o_color;


flat in int v_instance;

void main(void) {
        // -- fragmentConstants
    int c_instance = v_instance;
    int c_element = v_instance;
    vec2 c_screenPosition = gl_FragCoord.xy / u_contentScale;
    float c_contourPosition = 0.0;
    vec3 c_boundsPosition = vec3(0.0);
    vec3 c_boundsSize = vec3(0.0);
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
       //c_instance;

vec2 uv = va_texCoord0 / 32.0;
float layer = c_instance / (32*32);

texture(p_tiles, vec3(uv, layer));
x_fill = vec4(, 0.0, 1.0);
    }
         o_color = x_fill;
    o_color.rgb *= o_color.a;

}// -------------
// shade-style-custom:vertex-buffer-1600365613
// created 2022-09-02T13:57:08.266395700
/*
0(78) : error C0000: syntax error, unexpected ',', expecting "::" at token ","
*/
