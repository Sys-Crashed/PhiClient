#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform vec2 BlurXY;
uniform vec2 BlurCoord;
uniform float Radius;

float SCurve (float x) {


    // ---- by CeeJayDK

    x = x * 2.0 - 1.0;
    return -x * abs(x) * 0.5 + x + 0.5;

    //return dot(vec3(-x, 2.0, 1.0 ),vec3(abs(x), x, 1.0)) * 0.5; // possibly faster version




    // ---- original for posterity

    // How to do this without if-then-else?
    // +edited the too steep curve value

    // if (value < 0.5)
    // {
    //    return value * value * 2.0;
    // }

    // else
    // {
    // 	value -= 1.0;

    // 	return 1.0 - value * value * 2.0;
    // }
}

vec4 BlurH (sampler2D source, vec2 size, vec2 uv, float radius) {

    vec2 pixel = uv / oneTexel;
    vec2 regionMin = BlurXY * 2.0;
    vec2 regionMax = (BlurXY + BlurCoord) * 2.0;

    // Signed distance to the blur region in half-resolution pixels: negative inside, positive
    // outside. The mask feathers the edge, so the blur does not start with a hard rectangular seam.
    vec2 d = max(regionMin - pixel, pixel - regionMax);
    float dist = max(d.x, d.y);
    float feather = 24.0;
    float mask = 1.0 - smoothstep(0.0, feather, dist);

    if (mask <= 0.0)
    {
        return texture2D(source, uv);
    }

    vec4 A = vec4(0.0);
    vec4 C = vec4(0.0);

    float divisor = 0.0;
    float weight = 0.0;

    float radiusMultiplier = 1.0 / radius;

    for (float x = -radius; x <= radius; x++)
    {
        A = texture2D(source, uv + vec2(x * size) * BlurDir);

        weight = SCurve(1.0 - (abs(x) * radiusMultiplier));

        C += A * weight;

        divisor += weight;
    }

    vec4 blurred = vec4(C.r / divisor, C.g / divisor, C.b / divisor, 1.0);
    return mix(texture2D(source, uv), blurred, mask);
}

void main() {
    vec4 blurred = BlurH(DiffuseSampler, oneTexel, texCoord, Radius);
    gl_FragColor = blurred;
}