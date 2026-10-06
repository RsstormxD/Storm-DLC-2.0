#version 150

#moj_import <mre:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Smoothness; // edge smoothness

out vec4 OutColor;

uniform vec4 PanelClip;
in vec2 PanelPos;

void main() {
    if(PanelPos.x<PanelClip.x || PanelPos.y<PanelClip.y || PanelPos.x>PanelClip.z || PanelPos.y>PanelClip.w)discard;
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}