#if __VERSION__ >= 130
  #define attribute in
  #define varying out
#endif

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

attribute vec4  attribute_Position;
attribute vec2  a_texCoord;

varying   vec2  v_texCoords;

void main(void) {
    mat4    uniform_Projection = mat4(1);

    v_texCoords = a_texCoord;
    gl_Position = uniform_Projection * attribute_Position;
}
