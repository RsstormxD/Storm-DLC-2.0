#version 150

#moj_import <song-island:island.glsl>
uniform sampler2D Sampler0;
uniform vec4 LocalClip;
in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;
out vec4 fragColor;

void main() {
    if (LocalClip.z > 0.0 && (GlobalPos.x < LocalClip.x || GlobalPos.y < LocalClip.y
        || GlobalPos.x > LocalClip.x + LocalClip.z || GlobalPos.y > LocalClip.y + LocalClip.w)) discard;
    vec2 size = SizeSmooth.xy;
    vec2 p = (FragCoord - 0.5) * size;
    float distance = islandSdf(p, size * 0.5 - 1.0, Radius);
    float mask = 1.0 - smoothstep(-0.7, 0.7, distance);
    if (mask < 0.001) discard;
    float hover = clamp(Extra.x, 0.0, 1.0);
    float press = clamp(Extra.y, 0.0, 1.0);
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    float rim = 1.0 - smoothstep(0.0, 6.0, abs(distance));
    vec2 normal = normalize(p / max(size * 0.5, vec2(1.0)) + vec2(0.0001));
    vec2 uv = clamp(TexCoord + normal * texel * rim * (5.0 + 2.0 * hover - 2.0 * press), texel, 1.0 - texel);
    vec2 blur = texel * (2.5 + 1.5 * hover);
    vec3 background = texture(Sampler0, uv).rgb * 0.28;
    background += texture(Sampler0, uv + vec2(blur.x, 0.0)).rgb * 0.12;
    background += texture(Sampler0, uv - vec2(blur.x, 0.0)).rgb * 0.12;
    background += texture(Sampler0, uv + vec2(0.0, blur.y)).rgb * 0.12;
    background += texture(Sampler0, uv - vec2(0.0, blur.y)).rgb * 0.12;
    background += texture(Sampler0, uv + blur).rgb * 0.055;
    background += texture(Sampler0, uv - blur).rgb * 0.055;
    background += texture(Sampler0, uv + vec2(blur.x, -blur.y)).rgb * 0.055;
    background += texture(Sampler0, uv + vec2(-blur.x, blur.y)).rgb * 0.055;
    vec3 glass = mix(background * 0.80, FragColor.rgb * 0.22, 0.25 + hover * 0.12);
    glass += FragColor.rgb * (0.045 + hover * 0.045 - press * 0.025);
    float highlight = exp(-abs(distance + 0.8) * 1.8);
    glass += highlight * mix(0.09, 0.38, 1.0 - FragCoord.y);
    vec2 spot = (FragCoord - clamp(Extra.zw, 0.0, 1.0)) * vec2(1.0, 0.65);
    glass += exp(-dot(spot, spot) * 12.0) * hover * 0.065;
    glass += (1.0 - FragCoord.y) * 0.018;
    fragColor = vec4(glass, mask * FragColor.a);
}
