#version 150

#moj_import <mre:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float CornerSmoothness;
uniform float GlobalAlpha;

uniform float FresnelPower;
uniform vec3 FresnelColor;
uniform float FresnelAlpha;
uniform float BaseAlpha;
uniform bool FresnelInvert;
uniform float FresnelMix;
uniform float DistortStrength;
uniform float BlurRadius;

out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0/smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

uniform vec4 PanelClip;
in vec2 PanelPos;

void main() {
    if(PanelPos.x<PanelClip.x || PanelPos.y<PanelClip.y || PanelPos.x>PanelClip.z || PanelPos.y>PanelClip.w)discard;
    vec2 center = Size * 0.5;
    vec2 box_half_size = center - 1.0;
    vec2 pos = (FragCoord * Size) - center;

    float distance = roundedBoxSDF(-pos, box_half_size, Radius, CornerSmoothness);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);

    float distToEdge = abs(roundedBoxSDF(pos, box_half_size, Radius, CornerSmoothness));

    float max_dist_norm = min(box_half_size.x, box_half_size.y);
    float edge_gradient = 1.0 - clamp(distToEdge / max_dist_norm, 0.0, 1.0);

    float fresnel;
    float base = FresnelInvert ? edge_gradient : (1.0 - edge_gradient);

    if (FresnelPower > 20.0) {
        fresnel = exp(FresnelPower * log(clamp(base, 0.001, 1.0)));
    } else {
        fresnel = pow(base, FresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 dir = pos / max(length(pos), 0.0001);
    vec2 distortedTexCoord = TexCoord + dir * fresnel * DistortStrength;

    vec4 texColor = texture(Sampler0, distortedTexCoord);
    if (BlurRadius > 0.01) {
        vec2 d = vec2(BlurRadius) / vec2(textureSize(Sampler0, 0));
        texColor = (texColor * 4.0
            + texture(Sampler0, distortedTexCoord + vec2(d.x, 0.0))
            + texture(Sampler0, distortedTexCoord - vec2(d.x, 0.0))
            + texture(Sampler0, distortedTexCoord + vec2(0.0, d.y))
            + texture(Sampler0, distortedTexCoord - vec2(0.0, d.y))) / 8.0;
    }
    texColor *= FragColor;

    vec3 finalColor = mix(texColor.rgb, FresnelColor, fresnel * FresnelMix);
    float finalAlpha = mix(BaseAlpha, FresnelAlpha, fresnel) * alpha;

    if (finalAlpha < 0.001) {
        discard;
    }

    OutColor = vec4(finalColor, finalAlpha * GlobalAlpha);
}
