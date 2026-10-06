#version 150

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 worldPos;

uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform vec3 uBlockCenter;
uniform float uAlphaMode;

out vec4 fragColor;

void main() {
    // Wrap time to avoid float precision issues over long runs (infinite loop safety)
    float t = mod(uTime, 628.31853);

    // Get position relative to the block center to keep coordinates small and precise
    vec3 localPos = worldPos - uBlockCenter;

    // Map the 3D coordinates to a continuous 2D plane so waves flow seamlessly across all faces
    // Use x+y and z+y so that the top and bottom faces (where y is constant) also have full 2D variation
    vec2 uv = vec2(localPos.x + localPos.y, localPos.z + localPos.y) * 2.0;

    // Apply the wave/fractal formula
    for (float i = 1.0; i < 12.0; i++) {
        uv.x += 0.6 / i * cos(i * 2.5 * uv.y + t);
        uv.y += 0.6 / i * cos(i * 1.5 * uv.x + t);
    }

    // Calculate glow intensity based on the wave pattern
    float intensity = 0.1 / abs(sin(t - uv.y - uv.x));

    // Replace white to theme color (uColor1)
    // Keep a bright core by multiplying theme color by intensity (which can exceed 1.0)
    vec3 glowColor = uColor1 * intensity;

    // uAlphaMode > 0.5 means solid mode: render the full model, so alpha is only controlled by vertexColor.a (representing the overall fade)
    // otherwise only render the glowing parts
    float alpha = (uAlphaMode > 0.5) ? vertexColor.a : (clamp(intensity, 0.0, 1.0) * vertexColor.a);

    fragColor = vec4(glowColor, alpha);
}
