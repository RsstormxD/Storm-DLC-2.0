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

void main() {
    if (LocalClip.z > 0.0 && (GlobalPos.x < LocalClip.x || GlobalPos.y < LocalClip.y || GlobalPos.x > LocalClip.x + LocalClip.z || GlobalPos.y > LocalClip.y + LocalClip.w)) discard;
    vec2 size = SizeSmooth.xy;
    float smoothness = SizeSmooth.z;
    float corner = SizeSmooth.w;
    vec2 center = size * 0.5;
    vec2 p = center - (FragCoord * size);
    float dist = corner > 1.001
        ? islandSdfSquircle(p, center - 1.0, Radius, corner)
        : islandSdf(p, center - 1.0, Radius);
    float alpha = 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
    vec4 white = vec4(1.0, 1.0, 1.0, alpha);
    vec4 color = white * texture(Sampler0, TexCoord) * FragColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
