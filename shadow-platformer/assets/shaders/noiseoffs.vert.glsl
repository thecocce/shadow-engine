#import basic_imports;
#import noise2D;

attribute vec2 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

varying vec4 v_color;
varying vec2 v_texCoord;

#setting vec2 s_noise;

uniform mat4 u_projTrans;

void main() {
    v_color = a_color;
    v_texCoord = a_texCoord0;

    gl_Position = u_projTrans * vec4(a_position + ((0.5 * vec2(snoise(s_noise.xy + a_position.yx), snoise(s_noise.yx + a_position.xy))) + 0.25), 0.0, 1.0);
}