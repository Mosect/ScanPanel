precision mediump float;
varying vec2 colorCoord;
uniform sampler2D textureNum;

void main() {
    gl_FragColor = texture2D(textureNum, colorCoord);
}