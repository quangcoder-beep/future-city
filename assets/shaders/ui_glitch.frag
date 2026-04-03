#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform float u_time;

void main() {
    vec2 uv = v_texCoords;
    
    // 1. Glitchy chromatic aberration
    float glitch = sin(u_time * 10.0) * 0.003 * step(0.95, sin(u_time * 2.0 + uv.y * 50.0));
    float r = texture2D(u_texture, uv + vec2(glitch, 0.0)).r;
    float g = texture2D(u_texture, uv).g;
    float b = texture2D(u_texture, uv - vec2(glitch, 0.0)).b;
    vec4 color = vec4(r, g, b, texture2D(u_texture, uv).a);

    // 2. Scanlines
    float scanline = sin(uv.y * 600.0) * 0.04;
    color.rgb -= scanline;

    // 3. Vignette
    float dist = distance(uv, vec2(0.5, 0.5));
    float vignette = smoothstep(0.8, 0.4, dist);
    color.rgb *= mix(0.7, 1.0, vignette);

    // 4. Subtle flicker
    float flicker = 1.0 + 0.015 * sin(u_time * 60.0);
    color.rgb *= flicker;

    gl_FragColor = color * v_color;
}
