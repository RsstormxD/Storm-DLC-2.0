#version 150
in vec2 Position; in vec4 Color;
uniform mat4 ProjMat;
out vec4 fragColor; out vec2 pos;
void main() {
    gl_Position = ProjMat * vec4(Position, 0.0, 1.0);
    fragColor = Color; pos = Position;
}