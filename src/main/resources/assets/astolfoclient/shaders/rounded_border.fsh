#version 150
in vec4 fragColor;
in vec2 pos;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Thickness;
out vec4 outColor;

// ТОЧНО ТАКАЯ ЖЕ ФУНКЦИЯ, КАК В ШЕЙДЕРЕ ФОНА
float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    float distance = abs(sdRoundBox(pos - Size / 2.0, Size / 2.0, Radius));
    float smoothedAlpha = 1.0 - smoothstep(Thickness - 1.0, Thickness + 1.0, distance);
    outColor = vec4(fragColor.rgb, fragColor.a * smoothedAlpha);
}