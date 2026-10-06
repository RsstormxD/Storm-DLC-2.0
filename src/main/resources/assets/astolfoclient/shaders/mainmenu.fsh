#version 150

in vec2 fragTexCoord;
out vec4 fragColor;

uniform vec2 iResolution;
uniform float iTime;
uniform vec2 iMouse;

#define ITERATIONS 150
const vec3 MainColor = vec3(1.0);
const float pi = 3.14159265359;

float hash(vec3 p)
{
    p  = fract(p * vec3(.1031, .1030, .0973));
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float noise(in vec3 x)
{
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    
    return mix(mix(mix( hash(p+vec3(0,0,0)), hash(p+vec3(1,0,0)), f.x),
                   mix( hash(p+vec3(0,1,0)), hash(p+vec3(1,1,0)), f.x), f.y),
               mix(mix( hash(p+vec3(0,0,1)), hash(p+vec3(1,0,1)), f.x),
                   mix( hash(p+vec3(0,1,1)), hash(p+vec3(1,1,1)), f.x), f.y), f.z) * 2.0 - 1.0;
}

float saturate(float x)
{
    return clamp(x, 0.0, 1.0);
}

vec3 saturate(vec3 x)
{
    return clamp(x, vec3(0.0), vec3(1.0));
}

float rand(vec2 coord)
{
    return saturate(fract(sin(dot(coord, vec2(12.9898, 78.223))) * 43758.5453));
}

float pcurve( float x, float a, float b )
{
    float clampedX = clamp(x, 0.0, 1.0);
    float k = pow(a+b, a+b) / (pow(a, a)*pow(b, b));
    return k * pow( clampedX, a ) * pow( 1.0-clampedX, b );
}

float sdTorus(vec3 p, vec2 t)
{
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q)-t.y;
}

float sdSphere(vec3 p, float r)
{
  return length(p)-r;
}

float atan2(float y, float x)
{
    if (x > 0.0)
    {
        return atan(y / x);
    }
    else if (x == 0.0)
    {
        if (y > 0.0)
        {
            return pi / 2.0;
        }
        else if (y < 0.0)
        {
            return -(pi / 2.0);
        }
        else
        {
            return 0.0;
        }
    }
    else //(x < 0.0)
    {
        if (y >= 0.0)
        {
            return atan(y / x) + pi;
        }
        else
        {
            return atan(y / x) - pi;
        }
    }
}

void Haze(inout vec3 color, vec3 pos, float alpha)
{
    vec2 t = vec2(1.0, 0.01);
    float torusDist = length(sdTorus(pos + vec3(0.0, -0.05, 0.0), t));
    float bloomDisc = 1.0 / (pow(torusDist, 2.0) + 0.001);
    
    vec3 col = MainColor;
    bloomDisc *= length(pos) < 0.5 ? 0.0 : 1.0;
    
    // Scale by 0.02 (the combined multi-pass scaling factor: 2.9 * 0.02 = 0.058)
    color += col * bloomDisc * (0.058 / float(ITERATIONS)) * (1.0 - alpha * 1.0);
}

void GasDisc(inout vec3 color, inout float alpha, vec3 pos)
{
    float discRadius = 3.2;
    float discWidth = 5.3;
    float discInner = discRadius - discWidth * 0.5;
    float discOuter = discRadius + discWidth * 0.5;
    
    vec3 origin = vec3(0.0, 0.0, 0.0);
    vec3 discNormal = normalize(vec3(0.0, 1.0, 0.0));
    float discThickness = 0.1;

    float distFromCenter = distance(pos, origin);
    float distFromDisc = dot(discNormal, pos - origin);
    
    float radialGradient = 1.0 - saturate((distFromCenter - discInner) / discWidth * 0.5);
    float coverage = pcurve(radialGradient, 4.0, 0.9);

    discThickness *= radialGradient;
    coverage *= saturate(1.0 - abs(distFromDisc) / discThickness);

    vec3 dustColorLit = MainColor;

    // Apply combined scaling factors directly: 8.2 * 0.02 = 0.164
    float dustGlow = 1.0 / (pow(1.0 - radialGradient, 2.0) * 290.0 + 0.002);
    vec3 dustColor = dustColorLit * dustGlow * 0.164;

    coverage = saturate(coverage * 0.7);

    float fade = pow((abs(distFromCenter - discInner) + 0.4), 4.0) * 0.04;
    float bloomFactor = 1.0 / (pow(distFromDisc, 2.0) * 40.0 + fade + 0.00002);
    vec3 b = dustColorLit * pow(bloomFactor, 1.5);
    
    b *= mix(vec3(1.7, 1.1, 1.0), vec3(0.5, 0.6, 1.0), vec3(pow(radialGradient, 2.0)));
    b *= mix(vec3(1.7, 0.5, 0.1), vec3(1.0), vec3(pow(radialGradient, 0.5)));

    // Apply combined scaling factors directly: 150.0 * 0.02 = 3.0
    dustColor = mix(dustColor, b * 3.0, saturate(1.0 - coverage * 1.0));
    coverage = saturate(coverage + bloomFactor * bloomFactor * 0.1);
    
    if (coverage < 0.01)
    {
        return;   
    }
    
    vec3 radialCoords;
    radialCoords.x = distFromCenter * 1.5 + 0.55;
    radialCoords.y = atan2(-pos.x, -pos.z) * 1.5;
    radialCoords.z = distFromDisc * 1.5;
    radialCoords *= 0.95;
    
    float speed = 0.06;
    
    // Original multi-octave product noise logic to restore thread-like filaments
    float noise1 = 1.0;
    vec3 rc = radialCoords + 0.0;               rc.y += iTime * speed;
    noise1 *= noise(rc * 3.0) * 0.5 + 0.5;      rc.y -= iTime * speed;
    noise1 *= noise(rc * 6.0) * 0.5 + 0.5;      rc.y += iTime * speed;
    noise1 *= noise(rc * 12.0) * 0.5 + 0.5;     rc.y -= iTime * speed;
    noise1 *= noise(rc * 24.0) * 0.5 + 0.5;     rc.y += iTime * speed;

    float noise2 = 2.0;
    rc = radialCoords + 30.0;
    noise2 *= noise(rc * 3.0) * 0.5 + 0.5;      rc.y += iTime * speed;
    noise2 *= noise(rc * 6.0) * 0.5 + 0.5;      rc.y -= iTime * speed;
    noise2 *= noise(rc * 12.0) * 0.5 + 0.5;     rc.y += iTime * speed;
    noise2 *= noise(rc * 24.0) * 0.5 + 0.5;     rc.y -= iTime * speed;
    noise2 *= noise(rc * 48.0) * 0.5 + 0.5;     rc.y += iTime * speed;
    noise2 *= noise(rc * 92.0) * 0.5 + 0.5;     rc.y -= iTime * speed;

    dustColor *= noise1 * 0.998 + 0.002;
    coverage *= noise2;
    
    radialCoords.y += iTime * speed * 0.5;
    
    // Procedural simulation of iChannel1 noise texture lookup using dual-octave detail noise
    float d1 = noise(vec3(radialCoords.yx * vec2(0.15, 0.27) * 6.0, iTime * 0.03)) * 0.5 + 0.5;
    float d2 = noise(vec3(radialCoords.yx * vec2(0.15, 0.27) * 12.0, -iTime * 0.05)) * 0.5 + 0.5;
    float detail = mix(d1, d2, 0.4);
    dustColor *= pow(detail, 2.0) * 4.0;

    coverage = saturate(coverage * 1200.0 / float(ITERATIONS));
    dustColor = max(vec3(0.0), dustColor);

    coverage *= pcurve(radialGradient, 4.0, 0.9);

    color = (1.0 - alpha) * dustColor * coverage + color;
    alpha = (1.0 - alpha) * coverage + alpha;
}

vec3 rotate(vec3 p, float x, float y, float z)
{
    mat3 matx = mat3(1.0, 0.0, 0.0,
                     0.0, cos(x), sin(x),
                     0.0, -sin(x), cos(x));

    mat3 maty = mat3(cos(y), 0.0, -sin(y),
                     0.0, 1.0, 0.0,
                     sin(y), 0.0, cos(y));

    mat3 matz = mat3(cos(z), sin(z), 0.0,
                     -sin(z), cos(z), 0.0,
                     0.0, 0.0, 1.0);

    p = matx * p;
    p = matz * p;
    p = maty * p;

    return p;
}

void RotateCamera(inout vec3 eyevec, inout vec3 eyepos)
{
    vec2 mouse = iMouse;
    if (length(mouse) < 0.01) {
        mouse = iResolution * vec2(0.35, 0.5);
    }
    float mousePosY = mouse.y / iResolution.y;
    float mousePosX = mouse.x / iResolution.x;

    vec3 angle = vec3(mousePosY * 0.05 + 0.05, 1.0 + mousePosX * 1.0, -0.45);

    eyevec = rotate(eyevec, angle.x, angle.y, angle.z);
    eyepos = rotate(eyepos, angle.x, angle.y, angle.z);
}

void WarpSpace(inout vec3 eyevec, inout vec3 raypos)
{
    vec3 origin = vec3(0.0, 0.0, 0.0);
    float singularityDist = length(raypos);
    if (singularityDist < 0.01) return;
    
    float warpFactor = 1.0 / (singularityDist * singularityDist + 0.000001);
    vec3 singularityVector = -raypos / singularityDist;
    float warpAmount = 5.0;
    eyevec = normalize(eyevec + singularityVector * warpFactor * warpAmount / float(ITERATIONS));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord.xy / iResolution.xy;
    float aspect = iResolution.x / iResolution.y;

    vec3 eyevec = normalize(vec3((uv * 2.0 - 1.0) * vec2(aspect, 1.0), 6.0));
    vec3 eyepos = vec3(0.0, -0.0, -10.0);
    
    vec2 mousepos = iMouse.xy / iResolution.xy;
    if (length(iMouse) < 0.01)
    {
        mousepos = vec2(0.35, 0.5);
    }
    eyepos.x += mousepos.x * 3.0 - 1.5;
    
    const float far = 15.0;
    RotateCamera(eyevec, eyepos);

    vec3 color = vec3(0.0);
    float dither = 0.0;
    float alpha = 0.0;
    
    vec3 raypos = eyepos + eyevec * dither * far / float(ITERATIONS);
    for (int i = 0; i < ITERATIONS; i++)
    {        
        WarpSpace(eyevec, raypos);
        raypos += eyevec * far / float(ITERATIONS);
        
        GasDisc(color, alpha, raypos);
        Haze(color, raypos, alpha);
        
        // Terminate ray only when it hits deep inside the singularity to prevent NaN
        if (length(raypos) < 0.15)
        {
            break;
        }
    }
    
    // Apply the tonemapping and color grading from final bloom pass
    color = pow(color, vec3(1.5));
    color = color / (1.0 + color);
    color = pow(color, vec3(1.0 / 1.5));

    color = mix(color, color * color * (3.0 - 2.0 * color), vec3(1.0));
    color = pow(color, vec3(1.3, 1.20, 1.0));    

    color = saturate(color * 1.01);
    color = pow(color, vec3(0.7 / 2.2));

    fragColor = vec4(color, 1.0);
}

void main() {
    vec2 coord = fragTexCoord * iResolution;
    mainImage(fragColor, coord);
}
