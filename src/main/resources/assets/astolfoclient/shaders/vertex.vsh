#version 150

in vec2 position;
in vec2 texCoord;

out vec2 fragTexCoord;

void main() {
    fragTexCoord = texCoord;
    // We bypass the camera matrices entirely and draw directly flat to the monitor!
    gl_Position = vec4(position, 0.0, 1.0);
}