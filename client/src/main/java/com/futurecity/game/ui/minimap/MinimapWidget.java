package com.futurecity.game.ui.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.managers.CameraManager;
import com.futurecity.game.managers.MapManager;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.systems.MinimapLogic;
import com.futurecity.shared.packets.resonse.DeliveryGPSUpdate;

/**
 * MinimapWidget
 * Widget giao diện người dùng hiển thị bản đồ nhỏ.
 * Hỗ trợ hai chế độ:
 * - Chế độ Mini: Hiển thị ở góc màn hình khi đang chơi.
 * - Chế độ Full: Phóng to ra giữa màn hình để quan sát tổng thể và tìm đường.
 */
public class MinimapWidget extends Group {
    private MinimapLogic logic;
    private MinimapRenderer renderer;
    private MainCharactor player;
    private CameraManager cameraManager;
    private NetworkManager networkManager;
    private boolean isFullMode = false;
    private java.util.function.Supplier<Boolean> uiFocusChecker;

    /**
     * Set a checker to determine if the UI currently has focus on an input field.
     */
    public void setUiFocusChecker(java.util.function.Supplier<Boolean> checker) {
        this.uiFocusChecker = checker;
    }

    private boolean isUiFocused() {
        return uiFocusChecker != null && uiFocusChecker.get();
    }

    // Flag để đánh dấu người dùng đã chủ động tắt GPS, tránh bị server updates ép
    // bật lại
    private boolean manualCanceled = false;

    // Lưu đích đến cuối cùng để hỗ trợ click lại để hủy
    private Vector3 lastTargetPos = new Vector3(0, -1000, 0);

    // Timer cho tính năng tự động cập nhật đường đi (Auto-Recalculate)
    private float recalcTimer = 0f;
    private static final float RECALC_INTERVAL = 1.0f; // 1 giây

    // Cấu hình kích thước
    private static final float MINI_SIZE = 150f;
    private static final float MINI_RANGE = 250f; // Bán kính hiển thị chế độ thu nhỏ (mét)
    private static final float FULL_RANGE = 600f; // Bán kính hiển thị chế độ phóng to (mét)

    private VisTextButton closeBtn;
    private VisTextButton cancelGpsBtn; // Nút hủy dẫn đường

    private Actor inputLayer;

    public MinimapWidget(MainCharactor player, MapManager mapManager, Skin skin,
            CameraManager cameraManager, NetworkManager networkManager) {
        this.player = player;
        this.cameraManager = cameraManager;
        this.networkManager = networkManager;
        this.logic = new MinimapLogic(player, mapManager);
        this.renderer = new MinimapRenderer(logic);

        // Register Pathfinding Listener
        if (networkManager != null) {
            networkManager.setPathfindingListener(path -> {
                // Nếu người dùng đã hủy thủ công, không quan tâm đến đường đi mới từ server
                if (manualCanceled)
                    return;

                // System.out.println("CLIENT: Path received from Server. Nodes: " + (path !=
                // null ? path.size() : 0));
                logic.setCurrentPath(path);
            });
        }

        inputLayer = new Actor();
        this.addActor(inputLayer);

        // Click to expand/collapse OR Navigate
        inputLayer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isFullMode) {
                    setFullMode(true);
                } else {
                    // Trong chế độ Full, click để tìm đường
                    handleNavigationClick(x, y);
                }
            }
        });

        // 3. Close Button (Only for Full Mode)
        closeBtn = new VisTextButton("X");
        closeBtn.setSize(30, 30);
        closeBtn.setVisible(false);
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setFullMode(false);
            }
        });
        this.addActor(closeBtn);

        // 4. Cancel GPS Button (Only visible when path exists)
        cancelGpsBtn = new VisTextButton("Cancel GPS");
        cancelGpsBtn.setSize(80, 30);
        cancelGpsBtn.setVisible(false); // Mặc định ẩn
        cancelGpsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stopNavigation();
            }
        });
        this.addActor(cancelGpsBtn);

        setFullMode(false);
    }

    /**
     * Chuyển đổi giữa chế độ Mini (góc màn hình) và Full (phóng to giữa màn hình).
     * 
     * @param full true nếu muốn phóng to, false nếu muốn thu nhỏ.
     */
    private void setFullMode(boolean full) {
        this.isFullMode = full;
        updateLayout();
    }

    /**
     * Dừng GPS và nhớ trạng thái hủy thủ công.
     */
    public void stopNavigation() {
        logic.setCurrentPath(null);
        lastTargetPos.set(0, -1000, 0);
        manualCanceled = true;
        // System.out.println("CLIENT: Navigation stopped (Manual).");
    }

    public void updateLayout() {
        float stageW = getStage() != null ? getStage().getWidth() : Gdx.graphics.getWidth();
        float stageH = getStage() != null ? getStage().getHeight() : Gdx.graphics.getHeight();

        if (isFullMode) {
            float size = Math.min(stageW, stageH) - 100;
            setSize(size, size);
            setPosition((stageW - size) / 2, (stageH - size) / 2);

            closeBtn.setVisible(true);
            closeBtn.setPosition(getWidth() - 30, getHeight() - 30);
            cancelGpsBtn.setPosition(getWidth() / 2 - 40, 10); // Nút Hủy GPS nằm ở cạnh dưới giữa bản đồ
        } else {
            setSize(MINI_SIZE, MINI_SIZE);
            // Đặt góc trên bên trái: x = 20, y = chiều cao stage - kích thước minimap - 20
            setPosition(20, stageH - MINI_SIZE - 20);

            closeBtn.setVisible(false);
            cancelGpsBtn.setPosition(getWidth() / 2 - 40, -40); // Đưa nút ra rìa khi thu nhỏ
        }

        inputLayer.setSize(getWidth(), getHeight());
    }

    /**
     * Cập nhật logic của Widget mỗi frame.
     * Xử lý phím tắt 'R' để bật/tắt bản đồ và cập nhật tầm nhìn dựa trên chế độ
     * hiển thị.
     */
    @Override
    public void act(float delta) {
        super.act(delta);

        // Xử lý phím R để toggle
        if (!isUiFocused() && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            setFullMode(!isFullMode);
        }

        // Cập nhật logic Minimap (Lọc vật thể)
        float range = isFullMode ? FULL_RANGE : MINI_RANGE;
        logic.update(range);

        // Hiển thị/Ẩn nút "Hủy GPS" tùy theo có đường đi hay không
        boolean hasPath = logic.getCurrentPath() != null && !logic.getCurrentPath().isEmpty();
        cancelGpsBtn.setVisible(hasPath && isFullMode); // Chỉ hiện khi phóng to bản đồ

        // Tự động tính toán lại đường đi (Auto-Recalculate) khi đang chạy theo bản đồ
        if (lastTargetPos.y > -500 && hasPath) {
            Vector3 realDestination = logic.getCurrentPath().get(logic.getCurrentPath().size() - 1);

            // Đã đến đích thực tế (cách < 8m) -> Tự động dừng
            if (player != null && (player.getPosition().dst(realDestination) <= 8.0f ||
                    logic.getCurrentPath().size() <= 2)) {

                // Khi tự dừng do đến đích, ta không set manualCanceled = true
                // để nếu có đơn mới server vẫn tự bật được.
                logic.setCurrentPath(null);
                lastTargetPos.set(0, -1000, 0);
                manualCanceled = false;
                return;
            }

            recalcTimer += delta;
            if (recalcTimer >= RECALC_INTERVAL) {
                recalcTimer = 0f;
                // Xin đường mới định kỳ
                if (networkManager != null && !manualCanceled) {
                    networkManager.sendPathfindingRequest(lastTargetPos);
                }
            }
        }
    }

    /**
     * Vẽ bản đồ lên màn hình.
     * Sử dụng ScissorStack để đảm bảo bản đồ không bị vẽ trờm ra ngoài khung.
     * Tính toán rotation của bản đồ dựa trên hướng xoay của Camera.
     */
    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Removed validate() call as Group doesn't have it and we handle layout
        // manually

        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

        // Convert coordinates to draw correctly
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();

        // SCISSOR STACK: Cắt những gì vẽ ra ngoài khung minimap
        // Quan trọng: ScissorStack hoạt động với tọa độ màn hình, cần tính toán kỹ
        if (getStage() != null) {
            batch.flush();
            Rectangle scissors = new Rectangle();
            Rectangle clipBounds = new Rectangle(x, y, width, height);

            // Chuyển local coordinates sang screen coordinates
            // Vec2 tmp = new Vec2(x, y);
            // localToStageCoordinates(tmp);

            // Cách đơn giản hơn nếu widget không xoay: dùng ScissorStack.calculateScissors
            // Lưu ý: calculateScissors trả về void trong LibGDX bản mới, hoặc boolean bản
            // cũ.
            // Ta cần check document hoặc thử nghiệm. Ở dự án này, nó trả về void (theo fix
            // trước đó).
            ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {

                // Vẽ nền và nội dung Minimap

                // Để đơn giản, ta truyền ViewRadius vào Renderer để nó tự tính Scale nếu cần,
                // hoặc giữ nguyên logic cũ. Logic cũ: scale truyền vào render.
                float range = isFullMode ? FULL_RANGE : MINI_RANGE;
                float renderScale = getWidth() / (range * 2); // Dynamic scale based on FBO viewport

                // SINGLE MODE: Heading-Up (Centered)
                // Use Camera Yaw instead of Player Rotation for more intuitive navigation
                // Fix: Add 180 degrees to rotation to match camera/player orientation
                float mapRotation = -cameraManager.getYaw() + 180f;

                renderer.render(batch, x, y, width, height, renderScale, mapRotation);

                ScissorStack.popScissors();
            }
        }

        // Draw children (buttons) on top of the map
        super.draw(batch, parentAlpha);
    }

    /**
     * Cập nhật dữ liệu hình ảnh của bản đồ từ SceneManager.
     * Được gọi để làm mới các vật thể hiển thị trên Minimap.
     */
    public void updateSceneRender(net.mgsx.gltf.scene3d.scene.SceneManager sceneManager) {
        // Calculate center position: Player (No Offset in Single Mode)
        // NOTE: We must NOT modify player.getPosition(), so we create a temp vector or
        // copy
        Vector3 centerPos = new Vector3(player.getPosition());

        float range = isFullMode ? FULL_RANGE : MINI_RANGE;
        renderer.update(sceneManager, centerPos, range);
    }

    private void handleNavigationClick(float x, float y) {
        // [LOG REMOVED] Calculating coordinates

        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;
        float range = isFullMode ? FULL_RANGE : MINI_RANGE;
        float renderScale = getWidth() / (range * 2);
        float mapRotation = -cameraManager.getYaw() + 180f;

        Vector3 targetWorldPos = logic.mapToWorldCoords(x, y, centerX, centerY, renderScale, mapRotation);

        // Kiểm tra nếu click lại vào đích cũ (trong vòng 5m) và đã có đường đi -> Hủy
        // tìm đường
        float distToLast = targetWorldPos.dst(lastTargetPos);
        boolean hasPath = !logic.getCurrentPath().isEmpty();

        if (distToLast < 5.0f && hasPath) {
            stopNavigation();
            return;
        }

        lastTargetPos.set(targetWorldPos);
        manualCanceled = false; // Reset the manual cancellation flag when starting a new navigation
        if (networkManager != null) {
            networkManager.sendPathfindingRequest(targetWorldPos);
        }
    }

    /**
     * Bật GPS chỉ đường tới một tọa độ cụ thể.
     * MANUAL = true để ghi đè mọi trạng thái hủy trước đó.
     */
    public void setNavigationTarget(Vector3 targetWorldPos, boolean manual) {
        if (targetWorldPos == null)
            return;

        // Nếu là lệnh thủ công (từ điện thoại hoặc click map), reset cờ hủy
        if (manual) {
            this.manualCanceled = false;
        }

        // Tối ưu: Nếu đích đến mới gần đích cũ (< 5m), không xin đường lại
        if (lastTargetPos != null && targetWorldPos.dst(lastTargetPos) < 5.0f && !manual) {
            return;
        }

        // Ép xóa đường cũ để hiện vạch mới ngay
        logic.setCurrentPath(null);
        lastTargetPos.set(targetWorldPos);

        if (networkManager != null) {
            // System.out.println("CLIENT: GPS On " + (manual ? "Manual" : "Auto") + " to: "
            // + targetWorldPos);
            networkManager.sendPathfindingRequest(targetWorldPos);
        }
    }

    public void setNavigationTarget(Vector3 targetWorldPos) {
        setNavigationTarget(targetWorldPos, false);
    }

    /**
     * Cập nhật GPS động khi nhận tin từ server (Dùng cho Shipper theo dõi Buyer)
     */
    public void onDeliveryGPSUpdate(DeliveryGPSUpdate res) {
        // Chỉ cập nhật nếu KHÔNG phải do người dùng click thủ công điểm khác
        // Hoặc đơn giản là nếu đang có dẫn đường thì ưu tiên điểm mới nhất từ server
        if (manualCanceled)
            return;

        Vector3 newTarget = new Vector3(res.destX, res.destY, res.destZ);

        // Tối ưu: Nếu mục tiêu dịch chuyển > 5m thì mới xin đường lại
        if (lastTargetPos.dst(newTarget) > 5.0f) {
            // System.out.println("CLIENT: Dynamic GPS - Target moved. Updating path...");
            this.lastTargetPos.set(newTarget);
            if (networkManager != null) {
                networkManager.sendPathfindingRequest(newTarget);
            }
        }
    }
}
