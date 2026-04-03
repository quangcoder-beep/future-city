#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform float u_time;

void main() {
    vec2 uv = v_texCoords;
    
    // 1. Chromatic Aberration (Slight R/B shift)
    float shift = 0.0015 * sin(u_time * 5.0);
    float r = texture2D(u_texture, uv + vec2(shift, 0.0)).r;
    float g = texture2D(u_texture, uv).g;
    float b = texture2D(u_texture, uv - vec2(shift, 0.0)).b;
    vec4 color = vec4(r, g, b, texture2D(u_texture, uv).a);

    // 2. Scanlines
    float count = 480.0; // Number of scanlines
    float scanline = sin(uv.y * count * 3.14159);
    scanline = (scanline * 0.5 + 0.5); // 0 to 1
    scanline = pow(scanline, 0.1); // Sharpen lines
    color.rgb *= mix(0.85, 1.0, scanline);

    // 3. Vignette
    float dist = distance(uv, vec2(0.5, 0.5));
    float vignette = smoothstep(0.8, 0.4, dist);
    color.rgb *= mix(0.7, 1.0, vignette);

    // 4. Subtle Flicker
    float flicker = 1.0 + 0.01 * sin(u_time * 50.0);
    color.rgb *= flicker;

    gl_FragColor = color * v_color;
}
