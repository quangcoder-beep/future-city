package com.futurecity.game.ui.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.futurecity.game.systems.MinimapLogic;

import java.util.List;

/**
 * MinimapRenderer
 * Chịu trách nhiệm Vẽ các thành phần lên Minimap.
 * - Render cảnh 3D từ trên cao vào FBO.
 * - Vẽ đường dẫn GPS với hiệu ứng Glow 3 lớp, đường cong mượt và mũi tên chỉ
 * hướng.
 * - Vẽ mũi tên người chơi và viền bản đồ.
 */
public class MinimapRenderer {
    private ShapeRenderer shapeRenderer;
    private MinimapLogic logic;

    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private OrthographicCamera minimapCam;

    private float pulseTime = 0;

    /** Khoảng cách (pixel) giữa các mũi tên chỉ hướng */
    private static final float ARROW_SPACING_PX = 40f;

    // Cache tọa độ path đã chuyển đổi (tránh tạo object mới mỗi frame)
    private float[] cachedPathX = new float[0];
    private float[] cachedPathY = new float[0];
    private int cachedPathSize = 0;

    public MinimapRenderer(MinimapLogic logic) {
        this.logic = logic;
        this.shapeRenderer = new ShapeRenderer();

        // Init FBO (1024x1024 for high quality)
        try {
            fbo = new FrameBuffer(Pixmap.Format.RGBA8888,
                    1024, 1024, true);
            fboRegion = new TextureRegion(fbo.getColorBufferTexture());
            fboRegion.flip(false, true);
        } catch (Exception e) {
            Gdx.app.error("MinimapRenderer", "Could not create FBO", e);
        }

        // Init Ortho Camera
        minimapCam = new OrthographicCamera(200, 200);
        minimapCam.near = 1f;
        minimapCam.far = 1200f; // Tăng far plane để nhìn xa hơn
    }

    // ==================== FBO UPDATE ====================

    /**
     * Chụp cảnh 3D từ trên cao vào FBO texture.
     * Được gọi trước khi vẽ UI mỗi frame.
     */
    public void update(net.mgsx.gltf.scene3d.scene.SceneManager sceneManager, Vector3 playerPos, float range) {
        if (fbo == null)
            return;

        minimapCam.viewportWidth = range * 2;
        minimapCam.viewportHeight = range * 2;
        minimapCam.position.set(playerPos.x, 500f, playerPos.z); // 500m - nhìn từ trên cao hơn để thấy nhiều nhà
        minimapCam.lookAt(playerPos.x, playerPos.y, playerPos.z);
        minimapCam.up.set(0, 0, -1); // North (-Z) = Up on screen
        minimapCam.update();

        fbo.begin();
        Gdx.gl.glClearColor(0.66f, 0.85f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Camera oldCam = sceneManager.camera;
        sceneManager.setCamera(minimapCam);
        sceneManager.render();
        sceneManager.setCamera(oldCam);

        fbo.end();
    }

    // ==================== MAIN RENDER ====================

    public void render(Batch batch, float x, float y, float width, float height, float scale, float rotationDeg) {
        pulseTime += Gdx.graphics.getDeltaTime();

        // 1. Vẽ FBO Texture (bản đồ 3D từ trên cao)
        if (fboRegion != null) {
            float originX = width / 2;
            float originY = height / 2;
            batch.draw(fboRegion, x, y, originX, originY, width, height, 1, 1, rotationDeg);
        } else {
            batch.end();
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.DARK_GRAY);
            shapeRenderer.rect(x, y, width, height);
            shapeRenderer.end();
            batch.begin();
        }

        // 2. Chuyển sang ShapeRenderer để vẽ overlay
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix());

        float centerX = x + width / 2;
        float centerY = y + height / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Mũi tên người chơi (luôn ở tâm)
        float arrowRot = logic.getPlayerRotation() + rotationDeg;
        drawPlayerArrow(centerX, centerY, arrowRot);

        // Đường dẫn GPS
        renderPath(centerX, centerY, scale, rotationDeg);

        shapeRenderer.end();

        // Viền bản đồ
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
    }

    // ==================== GPS PATH RENDERING ====================

    /**
     * Vẽ đường dẫn GPS lên Minimap theo phong cách Google Maps.
     * - Đường thẳng bám sát đường phố (không nội suy cong).
     * - Viền đậm + lõi sáng tạo cảm giác chuyên nghiệp.
     * - Bo tròn mượt tại các ngã rẽ.
     * - Mũi tên chỉ hướng nhỏ gọn dọc đường đi.
     */
    private void renderPath(float centerX, float centerY, float scale, float rotationDeg) {
        List<Vector3> path = logic.getCurrentPath();
        if (path == null || path.size() < 2)
            return;

        int size = path.size();

        // === Bước 1: Chuyển tọa độ World → pixel (dùng mảng cache, không tạo object
        // mới) ===
        if (cachedPathX.length < size) {
            cachedPathX = new float[size];
            cachedPathY = new float[size];
        }
        cachedPathSize = size;

        for (int i = 0; i < size; i++) {
            Vector3 pt = path.get(i);

            // CẬP NHẬT: Luôn dùng vị trí hiện tại của Player cho điểm bắt đầu (i=0)
            // để đường GPS bám sát người chơi khi di chuyển.
            if (i == 0) {
                Vector3 playerPos = logic.getPlayerPosition();
                if (playerPos != null) {
                    pt = playerPos;
                }
            }

            Vector2 mp = logic.worldToMapCoords(pt.x, pt.z, centerX, centerY, scale, rotationDeg);
            cachedPathX[i] = mp.x;
            cachedPathY[i] = mp.y;
        }

        // === Bước 2: Viền ngoài (xanh đậm) ===
        shapeRenderer.setColor(0.05f, 0.25f, 0.55f, 0.9f);
        drawPathLine(cachedPathX, cachedPathY, cachedPathSize, 8f * scale);

        // === Bước 3: Lõi sáng (xanh Google Maps) ===
        shapeRenderer.setColor(0.25f, 0.52f, 0.96f, 1f);
        drawPathLine(cachedPathX, cachedPathY, cachedPathSize, 5f * scale);

        // === Bước 4: Mũi tên chỉ hướng ===
        drawDirectionArrows(cachedPathX, cachedPathY, cachedPathSize, scale);

        // === Bước 5: Điểm đích ===
        drawDestinationMarker(cachedPathX[size - 1], cachedPathY[size - 1], scale);

        // === Bước 6: Điểm xuất phát ===
        shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
        shapeRenderer.circle(cachedPathX[0], cachedPathY[0], 4f * scale, 12);
        shapeRenderer.setColor(0.25f, 0.52f, 0.96f, 1f);
        shapeRenderer.circle(cachedPathX[0], cachedPathY[0], 2.5f * scale, 10);
    }

    /**
     * Vẽ đường thẳng nối các điểm với bo tròn tại khớp nối.
     */
    private void drawPathLine(float[] px, float[] py, int count, float width) {
        for (int i = 1; i < count; i++) {
            float x1 = px[i - 1], y1 = py[i - 1];
            float x2 = px[i], y2 = py[i];

            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.1f)
                continue;

            float angle = (float) Math.atan2(dy, dx) * (float) (180.0 / Math.PI);

            // Thân đường (hình chữ nhật xoay)
            shapeRenderer.rect(x1, y1 - width / 2f, 0, width / 2f, len, width, 1, 1, angle);

            // Bo tròn tại mỗi khớp nối (ngã rẽ)
            shapeRenderer.circle(x1, y1, width / 2f, 10);
        }
        // Bo tròn tại điểm cuối
        if (count > 0) {
            shapeRenderer.circle(px[count - 1], py[count - 1], width / 2f, 10);
        }
    }

    /**
     * Vẽ mũi tên chỉ hướng dọc theo đường đi.
     * Giãn cách đều nhau theo khoảng cách pixel.
     */
    private void drawDirectionArrows(float[] px, float[] py, int count, float scale) {
        if (count < 2)
            return;

        float spacing = ARROW_SPACING_PX * scale;
        float accumulated = 0;
        float arrowSize = 4f * scale;

        shapeRenderer.setColor(1f, 1f, 1f, 0.8f);

        for (int i = 1; i < count; i++) {
            float x1 = px[i - 1], y1 = py[i - 1];
            float x2 = px[i], y2 = py[i];

            float dx = x2 - x1;
            float dy = y2 - y1;
            float segLen = (float) Math.sqrt(dx * dx + dy * dy);
            if (segLen < 0.1f)
                continue;

            float ux = dx / segLen;
            float uy = dy / segLen;

            accumulated += segLen;

            while (accumulated >= spacing) {
                accumulated -= spacing;
                float ax = x2 - ux * accumulated;
                float ay = y2 - uy * accumulated;
                drawSmallArrow(ax, ay, ux, uy, arrowSize);
            }
        }
    }

    /**
     * Vẽ một mũi tên nhỏ tại vị trí (ax, ay) theo hướng (ux, uy).
     */
    private void drawSmallArrow(float ax, float ay, float ux, float uy, float size) {
        // Đỉnh mũi tên (phía trước)
        float tipX = ax + ux * size;
        float tipY = ay + uy * size;

        // Hai cánh bên (xoay ±90 độ so với hướng di chuyển, lui về sau)
        float perpX = -uy; // Vuông góc
        float perpY = ux;

        float leftX = ax - ux * size * 0.5f + perpX * size * 0.6f;
        float leftY = ay - uy * size * 0.5f + perpY * size * 0.6f;

        float rightX = ax - ux * size * 0.5f - perpX * size * 0.6f;
        float rightY = ay - uy * size * 0.5f - perpY * size * 0.6f;

        shapeRenderer.triangle(tipX, tipY, leftX, leftY, rightX, rightY);
    }

    /**
     * Vẽ điểm đích với hiệu ứng Pulse (nhấp nháy phóng to thu nhỏ).
     */
    private void drawDestinationMarker(float dx, float dy, float scale) {
        float pulse = (float) Math.sin(pulseTime * 4f) * 0.5f + 0.5f;

        // Vòng tròn Aura mở rộng
        shapeRenderer.setColor(0.2f, 0.85f, 1f, 0.25f * (1f - pulse));
        shapeRenderer.circle(dx, dy, (12f + 18f * pulse) * scale, 24);

        // Viá»n ngoĂ i tráº¯ng
        shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
        shapeRenderer.circle(dx, dy, 7f * scale, 20);

        // Lõi màu xanh sáng
        shapeRenderer.setColor(0.2f, 0.85f, 1f, 1f);
        shapeRenderer.circle(dx, dy, 5f * scale, 16);

        // Chấm sáng trung tâm
        shapeRenderer.setColor(1f, 1f, 1f, 0.7f);
        shapeRenderer.circle(dx, dy, 2f * scale, 10);
    }

    // ==================== PLAYER ARROW ====================

    private void drawPlayerArrow(float x, float y, float rotation) {
        // Vòng tròn bao quanh
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(x, y, 10f);

        // Mũi tên bên trong
        shapeRenderer.setColor(0f, 0.5f, 1f, 1f);

        float size = 7f;
        float rad = (float) Math.toRadians(-rotation + 180);

        float tipX = x + (float) Math.sin(rad) * size;
        float tipY = y + (float) Math.cos(rad) * size;

        float radLeft = (float) Math.toRadians(-rotation + 180 + 140);
        float leftX = x + (float) Math.sin(radLeft) * size;
        float leftY = y + (float) Math.cos(radLeft) * size;

        float radRight = (float) Math.toRadians(-rotation + 180 - 140);
        float rightX = x + (float) Math.sin(radRight) * size;
        float rightY = y + (float) Math.cos(radRight) * size;

        shapeRenderer.triangle(tipX, tipY, leftX, leftY, rightX, rightY);
    }
}
