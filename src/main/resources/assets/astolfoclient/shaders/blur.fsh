#version 150

in vec4 outColor;
in vec2 texCoord;

uniform sampler2D DiffuseSampler;
uniform vec2 Resolution;
uniform float Radius;

out vec4 fragColor;

void main() {
    vec2 pixel = Radius / Resolution;
    vec4 color = vec4(0.0);

    color += texture(DiffuseSampler, texCoord + vec2(0.0, -pixel.y * 2.0));
    color += texture(DiffuseSampler, texCoord + vec2(-pixel.x * 2.0, 0.0));
    color += texture(DiffuseSampler, texCoord + vec2(pixel.x * 2.0, 0.0));
    color += texture(DiffuseSampler, texCoord + vec2(0.0, pixel.y * 2.0));
    color += texture(DiffuseSampler, texCoord + vec2(-pixel.x, -pixel.y));
    color += texture(DiffuseSampler, texCoord + vec2(-pixel.x, pixel.y));
    color += texture(DiffuseSampler, texCoord + vec2(pixel.x, -pixel.y));
    color += texture(DiffuseSampler, texCoord + vec2(pixel.x, pixel.y));

    fragColor = color / 8.0 * outColor;
}