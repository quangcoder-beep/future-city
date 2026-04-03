package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.AnimationName;
import com.futurecity.shared.enums.InteractionAction;

/**
 * Đại diện cho người chơi KHÁC trong thế giới game.
 * Class này không xử lý input, chỉ hiển thị (Render) và làm mượt chuyển động
 * (Interpolation).
 */
public class RemotePlayer extends GameObject implements Interactable {

    // Controller để chạy animation (Đi bộ, đứng yên...)
    private final AnimationController animationController;

    // --- Interpolation Variables ---
    private final Vector3 targetPosition = new Vector3();
    private float targetAngle = 0f;

    // Offset góc xoay (do model gốc bị lệch)
    private final float rotationOffset = 270f;

    // ID của người chơi
    private int playerId = -1;
    private int dbUserId = -1;
    private String username = "";
    private net.mgsx.gltf.scene3d.scene.Scene scene;

    public RemotePlayer(ModelInstance instance, PlayerState initialState) {
        super(instance);

        // Khởi tạo vị trí ban đầu ngay lập tức
        if (initialState != null) {
            this.playerId = initialState.id;
            this.dbUserId = initialState.dbUserId;
            this.username = initialState.username != null ? initialState.username : "Player " + playerId;
            this.position.set(initialState.position);
            this.targetPosition.set(initialState.position);
            this.targetAngle = initialState.angle;
        }

        // --- CÀI ĐẶT VA CHẠM (BOUNDING BOX) ---
        // Sử dụng kích thước cố định như MainCharactor để đảm bảo tương tác khớp nhau
        Vector3 min = new Vector3(-GameConstants.PLAYER_WIDTH / 2, 0, -GameConstants.PLAYER_DEPTH / 2);
        Vector3 max = new Vector3(GameConstants.PLAYER_WIDTH / 2, GameConstants.PLAYER_HEIGHT,
                GameConstants.PLAYER_DEPTH / 2);
        this.localBounds.set(min, max);
        updateBoundsFromTransform();

        // Setup Animation
        this.animationController = new AnimationController(instance);
        if (initialState != null && initialState.currentAnimation != null && !initialState.currentAnimation.isEmpty()) {
            animationController.setAnimation(initialState.currentAnimation, -1);
        } else {
            animationController.setAnimation(AnimationName.IDLE.getValue(), -1);
        }
    }

    /**
     * Cập nhật trạng thái mục tiêu từ Server.
     * Dùng để nội suy vị trí và xoay, tránh hiện tượng giật lag khi mạng không ổn
     * định.
     */
    public void setTargetState(PlayerState state) {
        // Cập nhật đích đến mới
        this.targetPosition.set(state.position);
        this.targetAngle = state.angle;
        this.dbUserId = state.dbUserId;
        if (state.username != null)
            this.username = state.username;

        // Cập nhật Animation nếu thay đổi
        String animId = state.currentAnimation;
        if (animId == null || animId.isEmpty())
            animId = AnimationName.IDLE.getValue();

        if (animationController.current == null || !animationController.current.animation.id.equals(animId)) {
            // Chuyển animation mượt trong 0.2s
            animationController.animate(animId, -1, 1f, null, 0.2f);
        }
    }

    /**
     * HĂ m update gá»i má»—i frame (60 FPS)
     */
    /**
     * Cập nhật logic của người chơi khác mỗi khung hình.
     */
    public void update(float delta) {
        // 1. INTERPOLATION (Làm mượt vị trí)
        float distSq = this.position.dst2(targetPosition);
        
        // Epsilon Snapping: Nếu cách điểm đích < 0.05 (gần như đúng tâm), Snap luôn để dứt điểm
        // Hoặc nếu cách quá 25 (hơn 5 mét), khả năng là lag nặng hoặc dịch chuyển tức thời -> Snap liền để tránh trượt xa
        if (distSq < 0.005f || distSq > 25f) {
            this.position.set(targetPosition);
        } else {
            // Nội suy mềm mượt (LERP) nếu ở khoảng cách vừa phải
            // Tốc độ đuổi theo phụ thuộc vào delta để chạy mượt ở mọi FPS
            float alpha = 10f * delta;
            if (alpha > 1f) alpha = 1f;
            this.position.lerp(targetPosition, alpha);
        }

        // 2. Cập nhật Model Transform (Vị trí + Xoay + Scale)
        updateTransform();

        // 3. Update Animation
        animationController.update(delta);
    }

    private void updateTransform() {
        // Reset transform về vị trí mới
        instance.transform.setToTranslation(this.position);

        // Xoay theo góc server gửi về
        instance.transform.rotate(Vector3.Y, targetAngle + rotationOffset);

        // Scale model theo tỉ lệ game
        instance.transform.scale(GameConstants.WORLD_SCALE, GameConstants.WORLD_SCALE, GameConstants.WORLD_SCALE);

        // Cập nhật bounding box
        updateBoundsFromTransform();
    }

    // --- INTERACTABLE IMPLEMENTATION ---

    @Override
    public BoundingBox getCollisionBox() {
        return this.getWorldBounds();
    }

    @Override
    public void onInteract() {
        System.out.println("Tương tác với người chơi: " + playerId);
    }

    @Override
    public String getInteractionPrompt() {
        return "Press F to interact";
    }

    @Override
    public float getRequiredDot() {
        return 0.5f;
    }

    @Override
    public String getNameId() {
        return "player_" + playerId;
    }

    public int getDbUserId() {
        return dbUserId;
    }

    @Override
    public InteractionAction getAction() {
        return InteractionAction.INTERACT;
    }

    public int getId() {
        return playerId;
    }

    public String getUsername() {
        return (username != null && !username.isEmpty()) ? username : "Player " + playerId;
    }

    public void setUsername(String newName) {
        if (newName != null && !newName.isEmpty()) {
            this.username = newName;
        }
    }

    public net.mgsx.gltf.scene3d.scene.Scene getScene() {
        return scene;
    }

    public void setScene(net.mgsx.gltf.scene3d.scene.Scene scene) {
        this.scene = scene;
    }
}
