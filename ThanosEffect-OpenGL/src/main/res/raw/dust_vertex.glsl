#version 300 es
precision highp float;

layout(location = 0) in vec2 inRelativePosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in float inAlpha;
layout(location = 3) in float inLifetime;
layout(location = 4) in float inTime;
layout(location = 5) in float inScale;
layout(location = 6) in float inVy;
layout(location = 7) in float inDy;
layout(location = 8) in float inDx;

uniform mat4 projectionMatrix;
uniform float tx;
uniform float ty;
uniform float time;
uniform float seed;
uniform float centerX;
uniform float centerY;
uniform float rendering;
uniform float optimizedSize;

uniform bool isRenderingBitmap;
uniform vec4 viewSrc;
uniform vec4 viewDst;
out vec2 viewPosition;

out vec4 fragColor;
out float fraction;


float rand(vec2 n) {
	return fract(sin(dot(n,vec2(12.9898,4.1414-seed*.42)))*4375.5453);
}

vec2 mapPosition(vec4 src) {
    return vec2[4](
        vec2(src.x, src.w),  // bottom left
        vec2(src.z, src.w),  // bottom right
        vec2(src.z, src.y),  // top right
        vec2(src.x, src.y)   // top left
    )[gl_VertexID];
}

void main() {
    fraction = (time - inTime) / inLifetime;
    if (!isRenderingBitmap && fraction <= 1.0) {
        vec2 position = vec2(0.0, 0.0);
        float renderingEffect = sqrt(rendering);

        float dy = inDy;

        position.y = ty + inRelativePosition.y +
            (inRelativePosition.y - centerY) / centerY *
            dy * fraction * renderingEffect;
        position.y -= pow(fraction * inVy, 2.0) * sqrt(renderingEffect);

        position.x = tx + inRelativePosition.x +
            (inRelativePosition.x - centerX) / centerX *
            inDx * fraction * renderingEffect;

        float firstRenderingFraction = (time - inTime) / 200.0;
        float scale = inScale * 2.0;
        if (firstRenderingFraction > 1.0) {
            float scaleTimeFraction = (time - inTime - 200.0) / (inLifetime - 200.0);
            scale = min(15.0, scale * (1.0 - scaleTimeFraction));
        } else {
            float originalSize = max(scale, optimizedSize * 1.5);
            scale = originalSize - (originalSize - scale) * firstRenderingFraction;
        }

        gl_PointSize = scale;
        gl_Position = projectionMatrix * vec4(position, 0.0, 1.0);
        fragColor = vec4(inColor, inAlpha * min(1.2 - fraction, 1.0));
    } else if (isRenderingBitmap) {
        fraction = -1.0;
        vec2 position = mapPosition(viewDst);
        position.x += tx;
        position.y += ty;

        gl_Position = projectionMatrix * vec4(position, 0.0, 1.0);
        viewPosition = mapPosition(viewSrc);
    }
}