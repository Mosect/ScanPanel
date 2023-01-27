uniform mat4 cameraMatrix;
uniform mat4 textureMatrix;
attribute vec3 position;
attribute vec2 textureCoord;
varying vec2 colorCoord;

void main() {
    gl_Position = cameraMatrix * vec4(position, 1.0f);
    vec4 cp = textureMatrix * vec4(textureCoord.x, textureCoord.y, 0.0f, 1.0f);
    colorCoord = cp.xy;
}
