#version 150

uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform vec3 u_color;
uniform float u_radius;

in vec2 fragTexCoord;
out vec4 outColor;

void main() {
    // In version 150, we use 'texture()' instead of 'texture2D()'
    vec4 center = texture(u_texture, fragTexCoord);

    // If we hit the solid player silhouette, draw the white core
    if (center.a > 0.0) {
        outColor = vec4(1.0, 1.0, 1.0, 0.8);
        return;
    }

    // Gaussian Bloom Math
    float alpha = 0.0;

    for (float x = -u_radius; x <= u_radius; x++) {
        for (float y = -u_radius; y <= u_radius; y++) {
            float dist = sqrt(x * x + y * y);
            if (dist <= u_radius) {
                vec4 sampleData = texture(u_texture, fragTexCoord + vec2(x, y) * u_texelSize);
                if (sampleData.a > 0.0) {
                    float weight = (u_radius - dist) / u_radius;
                    alpha += weight;
                }
            }
        }
    }

    if (alpha > 0.0) {
        alpha = clamp(alpha / (u_radius * 0.5), 0.0, 1.0);
        outColor = vec4(u_color, alpha); // Draw the Theme Color Aura
    } else {
        outColor = vec4(0.0); // Transparent empty space
    }
}