#version 150
in vec4 fragColor; in vec2 pos;
uniform vec2 Size; uniform vec4 Radius; uniform float Smoothness;
out vec4 outColor;
float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}
void main() {
    float distance = sdRoundBox(pos - Size / 2.0, Size / 2.0, Radius);
    float smoothedAlpha = 1.0 - smoothstep(0.0, Smoothness, distance);
    outColor = vec4(fragColor.rgb, fragColor.a * smoothedAlpha);
}