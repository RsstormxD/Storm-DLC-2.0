#version 150

uniform mat4 ModelViewMat;
uniform vec4 ColorModulator;
#moj_import <song-island:island.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;
uniform vec4 LocalClip;

out vec4 fragColor;

const float DPI = 6.28318530718;
const float STEP = DPI / 16.0;

void main() {
    if (LocalClip.z > 0.0 && (GlobalPos.x < LocalClip.x || GlobalPos.y < LocalClip.y || GlobalPos.x > LocalClip.x + LocalClip.z || GlobalPos.y > LocalClip.y + LocalClip.w)) discard;
    vec2 size = SizeSmooth.xy;
    float smoothness = SizeSmooth.z;
    float corner = SizeSmooth.w;
    float blurRadius = Extra.x;

    vec2 multiplier = blurRadius / vec2(textureSize(Sampler0, 0));
    vec3 average = texture(Sampler0, TexCoord).rgb;
    for (float d = 0.0; d < DPI; d += STEP) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            average += texture(Sampler0, TexCoord + vec2(cos(d), sin(d)) * multiplier * i).rgb;
        }
    }
    average /= 80.0;

    vec2 center = size * 0.5;
    vec2 p = center - (FragCoord * size);
    float dist = corner > 1.001
        ? islandSdfSquircle(p, center - 1.0, Radius, corner)
        : islandSdf(p, center - 1.0, Radius);
    float alpha = 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
    vec4 color = vec4(average, alpha) * FragColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
