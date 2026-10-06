#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ProjMat;

out vec4 outColor;
out vec2 texCoord;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    outColor = Color;
    texCoord = UV0;
}