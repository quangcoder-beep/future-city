package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.futurecity.game.inputs.IInputController;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.AnimationName;

/**
 * Lớp đại diện cho nhân vật chính ở phía Client.
 * Lớp này KHÔNG chứa logic di chuyển hay vật lý.
 * Nó chỉ là một "con rối" (puppet), có nhiệm vụ:
 * 1. Đọc dữ liệu từ PlayerState (do Server gửi về).
 * 2. Cập nhật vị trí, hướng xoay và animation của model 3D tương ứng.
 * 3. Gửi tín hiệu input (phím bấm) lên Server.
 */
public class MainCharactor extends GameObject {

    // Trạng thái cốt lõi của nhân vật, được đồng bộ từ Server.
    private final PlayerState state;

    // Trạng thái dự đoán (Client-Side Prediction)
    private final PlayerState predictedState = new PlayerState();
    private boolean needsReconciliation = false;
    private static final float SNAP_THRESHOLD = 3.0f;
    private static final float LERP_THRESHOLD = 0.5f;

    // Phục vụ cho tính toán va chạm Client-Side
    private final Vector3 tmpMin = new Vector3();
    private final Vector3 tmpMax = new Vector3();
    private final com.badlogic.gdx.math.collision.BoundingBox playerBox = new com.badlogic.gdx.math.collision.BoundingBox();
    private final GameObject ghost = new GameObject(null) {
        @Override
        public com.badlogic.gdx.math.collision.BoundingBox getWorldBounds() {
            return playerBox;
        }
    };

    // Controller đầu vào để đọc phím bấm của người chơi.
    private final IInputController input;

    // Controller quản lý Animation của LibGDX.
    private final AnimationController animationController;

    // Hằng số bù trừ góc xoay nếu model gốc bị lệch.
    private final float rotationOffset = 270f;

    /**
     * Constructor của nhân vật phía Client.
     * 
     * @param instance        Model 3D của nhân vật.
     * @param inputController Bộ xử lý input (bàn phím, chuột).
     * @param initialState    Trạng thái ban đầu của nhân vật.
     */
    public MainCharactor(ModelInstance instance, IInputController inputController, PlayerState initialState) {
        super(instance);
        this.input = inputController;
        this.state = initialState;

        // Cập nhật vị trí ban đầu
        this.position.set(state.position);

        // Khởi tạo predictedState giống hệt state
        this.predictedState.id = state.id;
        this.predictedState.position.set(state.position);
        this.predictedState.velocity.set(state.velocity);
        this.predictedState.angle = state.angle;
        this.predictedState.currentAnimation = state.currentAnimation;

        // --- CÀI ĐẶT VA CHẠM (BOUNDING BOX) ---
        // Bounding box vẫn cần ở Client để thực hiện các tác vụ cục bộ như
        // ray-casting (chọn mục tiêu) hoặc các hiệu ứng đơn giản.
        Vector3 min = new Vector3(-GameConstants.PLAYER_WIDTH / 2, 0, -GameConstants.PLAYER_DEPTH / 2);
        Vector3 max = new Vector3(GameConstants.PLAYER_WIDTH / 2, GameConstants.PLAYER_HEIGHT,
                GameConstants.PLAYER_DEPTH / 2);
        this.localBounds.set(min, max);
        updateBoundsFromTransform();

        // --- CÀI ĐẶT ANIMATION ---
        animationController = new AnimationController(instance);
        // Chạy animation ban đầu (thường là "Idle")
        if (state.currentAnimation != null && !state.currentAnimation.isEmpty()) {
            animationController.setAnimation(state.currentAnimation, -1);
        } else {
            animationController.setAnimation(AnimationName.IDLE.getValue(), -1);
        }
    }

    /**
     * Hàm update cho nhân vật ở Client.
     * 
     * @param deltaTime     Thời gian giữa các frame.
     * @param cameraYaw     Góc xoay của camera (dùng cho chế độ FPS).
     * @param isFirstPerson Cờ báo hiệu đang ở góc nhìn thứ nhất.
     */
    public void update(float deltaTime, float cameraYaw, boolean isFirstPerson,
            com.futurecity.game.systems.CollisionSystem collisionSystem,
            java.util.List<GameObject> obstacles) {
        // 1. CLIENT-SIDE PREDICTION: Áp dụng input ngay lập tức
        predictLocalMovement(deltaTime, cameraYaw, isFirstPerson, collisionSystem, obstacles);

        // 2. RECONCILIATION: Nếu nhận được state mới từ server
        if (needsReconciliation) {
            reconcileWithServer();
            needsReconciliation = false;
        }

        // 3. Cập nhật transform từ predicted state (người chơi thấy ngay di chuyển)
        updateTransformFromPrediction(cameraYaw, isFirstPerson);

        // 4. Cập nhật Animation từ predicted state
        updateAnimationFromPrediction();

        // 3. Cập nhật AnimationController của LibGDX (bắt buộc)
        animationController.update(deltaTime);
    }

    private void predictLocalMovement(float delta, float cameraYaw, boolean isFPS,
            com.futurecity.game.systems.CollisionSystem collisionSystem,
            java.util.List<GameObject> obstacles) {
        com.futurecity.shared.physics.PlayerPhysics.processInput(predictedState,
                input.getHorizontal(), input.getVertical(),
                input.isRunPressed(), cameraYaw, isFPS, delta);

        float halfW = (GameConstants.PLAYER_WIDTH * GameConstants.WORLD_SCALE) * 0.6f;
        float halfD = (GameConstants.PLAYER_DEPTH * GameConstants.WORLD_SCALE) * 0.6f;
        float height = GameConstants.PLAYER_HEIGHT * GameConstants.WORLD_SCALE;

        // Di chuyển trục X
        float savedX = predictedState.position.x;
        predictedState.position.x += predictedState.velocity.x * delta;
        tmpMin.set(predictedState.position.x - halfW, predictedState.position.y + 0.5f,
                predictedState.position.z - halfD);
        tmpMax.set(predictedState.position.x + halfW, predictedState.position.y + height,
                predictedState.position.z + halfD);
        playerBox.set(tmpMin, tmpMax);
        if (collisionSystem != null && obstacles != null && collisionSystem.checkCollision(ghost, obstacles)) {
            predictedState.position.x = savedX;
            predictedState.velocity.x = 0;
        }

        // Di chuyển trục Z
        float savedZ = predictedState.position.z;
        predictedState.position.z += predictedState.velocity.z * delta;
        tmpMin.set(predictedState.position.x - halfW, predictedState.position.y + 0.5f,
                predictedState.position.z - halfD);
        tmpMax.set(predictedState.position.x + halfW, predictedState.position.y + height,
                predictedState.position.z + halfD);
        playerBox.set(tmpMin, tmpMax);
        if (collisionSystem != null && obstacles != null && collisionSystem.checkCollision(ghost, obstacles)) {
            predictedState.position.z = savedZ;
            predictedState.velocity.z = 0;
        }
    }

    public void onServerStateReceived(PlayerState serverState) {
        // Lưu server state
        this.state.position.set(serverState.position);
        this.state.velocity.set(serverState.velocity);
        this.state.angle = serverState.angle;
        this.state.currentAnimation = serverState.currentAnimation;
        this.needsReconciliation = true;
    }

    private void reconcileWithServer() {
        float dist = predictedState.position.dst(state.position);
        if (dist > SNAP_THRESHOLD) {
            // Sai lệch quá lớn -> snap ngay lập tức về vị trí server
            predictedState.position.set(state.position);
        } else if (dist > LERP_THRESHOLD) {
            // Sai lệch nhỏ -> lerp mượt để che giấu độ trễ
            predictedState.position.lerp(state.position, 0.3f);
        }

        // Luôn đồng bộ lại các tham số không phải position
        predictedState.velocity.set(state.velocity);
        predictedState.angle = state.angle;
        predictedState.currentAnimation = state.currentAnimation;
    }

    /**
     * Cập nhật vị trí và hướng xoay của model 3D dựa trên PREDICTED state.
     */
    private void updateTransformFromPrediction(float cameraYaw, boolean isFirstPerson) {
        // Lấy vị trí từ predicted state
        this.position.set(predictedState.position);

        // Cập nhật ma trận transform của model - GIỐNG Y HỆT code gốc
        instance.transform.setToTranslation(this.position);

        if (isFirstPerson) {
            // Ở góc nhìn thứ nhất, model xoay theo camera
            instance.transform.rotate(Vector3.Y, cameraYaw + rotationOffset);
        } else {
            // Ở góc nhìn thứ ba, model xoay theo predicted angle
            instance.transform.rotate(Vector3.Y, predictedState.angle + rotationOffset);
        }

        // ⭐ CRITICAL: Scale model theo WORLD_SCALE như code gốc!
        instance.transform.scale(GameConstants.WORLD_SCALE, GameConstants.WORLD_SCALE, GameConstants.WORLD_SCALE);

        // Cập nhật bounding box
        updateBoundsFromTransform();
    }

    /**
     * Cập nhật animation dựa trên `currentAnimation` trong PREDICTED state.
     */
    private void updateAnimationFromPrediction() {
        String anim = predictedState.currentAnimation;
        if (anim == null || anim.isEmpty()) {
            anim = AnimationName.IDLE.getValue();
        }

        // Chỉ chuyển animation nếu khác với animation hiện tại để tránh giật
        if (animationController.current == null || !animationController.current.animation.id.equals(anim)) {
            // Chuyển đổi mượt mà (blending) giữa các animation
            animationController.animate(anim, -1, 1.0f, null, 0.2f);
        }
    }

    /**
     * Cung cấp quyền truy cập vào IInputController.
     * Dùng cho các hệ thống khác (như NetworkManager) để đọc và gửi input.
     * 
     * @return Bộ xử lý input.
     */
    public IInputController getInput() {
        return input;
    }

    /**
     * Cung cấp quyền truy cập vào PlayerState.
     * Dùng cho các hệ thống khác (như CameraManager) để đọc vị trí.
     * 
     * @return Trạng thái cốt lõi của nhân vật.
     */
    public PlayerState getState() {
        return state;
    }

    /**
     * Lấy góc quay hiện tại của nhân vật.
     * 
     * @return Góc quay (độ).
     */
    public float getRotation() {
        return state.angle;
    }
}
