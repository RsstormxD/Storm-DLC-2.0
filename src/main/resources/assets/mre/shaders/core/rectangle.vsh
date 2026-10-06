#version 150

#moj_import <mre:common.glsl>

in vec3 Position; // POSITION_COLOR vertex attributes
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 FragColor;

uniform mat4 LocalMat;
out vec2 PanelPos;

void main() {
    PanelPos=(LocalMat*vec4(Position,1.0)).xy;
    FragCoord = rvertexcoord(gl_VertexID);
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}