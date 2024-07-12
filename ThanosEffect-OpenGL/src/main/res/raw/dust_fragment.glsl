#version 300 es

precision highp float;

uniform sampler2D viewTexture;

in vec4 fragColor;
in float fraction;
in vec2 viewPosition;

out vec4 fragColorOut;

void main() {
  if (fraction >= 0.0) {
    if (fraction > 1.0) {
        discard;
    }
    vec2 circCoord = 2.0 * gl_PointCoord - 1.0;
    if (dot(circCoord, circCoord) > 1.0) {
        discard;
    }

    fragColorOut = fragColor;
  } else {
    fragColorOut = texture(viewTexture, viewPosition);
  }
}