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

    public static class InputSnapshot {
        public int sequence;
        public float horizontal, vertical;
        public boolean isRunning;
        public float cameraYaw;
        public float delta;
        public boolean isFirstPerson;
    }

    private final java.util.LinkedList<InputSnapshot> pendingInputs = new java.util.LinkedList<>();
    private int currentSequence = 0;

    // Trạng thái dự đoán (Client-Side Prediction)
    private final PlayerState predictedState = new PlayerState();
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

    // Cache va chạm để phục vụ Rollback & Replay chính xác
    private com.futurecity.game.systems.CollisionSystem lastCollisionSystem;
    private java.util.List<GameObject> lastObstacles;

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
        // Cập nhật cache va chạm
        this.lastCollisionSystem = collisionSystem;
        this.lastObstacles = obstacles;

        // 1. CLIENT-SIDE PREDICTION: Áp dụng input ngay lập tức
        enqueueAndPredictLocalMovement(deltaTime, cameraYaw, isFirstPerson, collisionSystem, obstacles);

        // 3. Cập nhật transform từ predicted state (người chơi thấy ngay di chuyển)
        updateTransformFromPrediction(cameraYaw, isFirstPerson);

        // 4. Cập nhật Animation từ predicted state
        updateAnimationFromPrediction();

        // 3. Cập nhật AnimationController của LibGDX (bắt buộc)
        animationController.update(deltaTime);
    }

    private void enqueueAndPredictLocalMovement(float delta, float cameraYaw, boolean isFPS,
            com.futurecity.game.systems.CollisionSystem collisionSystem,
            java.util.List<GameObject> obstacles) {
        
        try {
            // 1. Capture Input
            InputSnapshot snap = new InputSnapshot();
            snap.sequence = ++currentSequence;
            snap.horizontal = input.getHorizontal();
            snap.vertical = input.getVertical();
            snap.isRunning = input.isRunPressed();
            snap.cameraYaw = cameraYaw;
            snap.delta = delta;
            snap.isFirstPerson = isFPS;
            pendingInputs.add(snap);

            // Anti-Memory Leak: Nếu lag quá nặng (> 10 giây không có phản hồi từ Server), 
            // xóa bớt input cũ nhất để tránh tràn bộ nhớ.
            if (pendingInputs.size() > 1000) {
                pendingInputs.removeFirst();
            }

            // 2. Predict locally (using the snapshot)
            simulatePhysicsState(predictedState, snap, collisionSystem, obstacles);
        } catch (Exception e) {
            System.err.println("[PREDICT ERROR] " + e.getMessage());
        }
    }

    private void simulatePhysicsState(PlayerState targetState, InputSnapshot snap,
            com.futurecity.game.systems.CollisionSystem collisionSystem,
            java.util.List<GameObject> obstacles) {
            
        com.futurecity.shared.physics.PlayerPhysics.processInput(targetState,
                snap.horizontal, snap.vertical,
                snap.isRunning, snap.cameraYaw, snap.isFirstPerson, snap.delta);

        float halfW = (GameConstants.PLAYER_WIDTH * GameConstants.WORLD_SCALE) * 0.6f;
        float halfD = (GameConstants.PLAYER_DEPTH * GameConstants.WORLD_SCALE) * 0.6f;
        float height = GameConstants.PLAYER_HEIGHT * GameConstants.WORLD_SCALE;

        // Di chuyển trục X
        float savedX = targetState.position.x;
        targetState.position.x += targetState.velocity.x * snap.delta;
        tmpMin.set(targetState.position.x - halfW, targetState.position.y + 0.5f,
                targetState.position.z - halfD);
        tmpMax.set(targetState.position.x + halfW, targetState.position.y + height,
                targetState.position.z + halfD);
        playerBox.set(tmpMin, tmpMax);
        if (collisionSystem != null && obstacles != null && collisionSystem.checkCollision(ghost, obstacles)) {
            targetState.position.x = savedX;
            targetState.velocity.x = 0;
        }

        // Di chuyển trục Z
        float savedZ = targetState.position.z;
        targetState.position.z += targetState.velocity.z * snap.delta;
        tmpMin.set(targetState.position.x - halfW, targetState.position.y + 0.5f,
                targetState.position.z - halfD);
        tmpMax.set(targetState.position.x + halfW, targetState.position.y + height,
                targetState.position.z + halfD);
        playerBox.set(tmpMin, tmpMax);
        if (collisionSystem != null && obstacles != null && collisionSystem.checkCollision(ghost, obstacles)) {
            targetState.position.z = savedZ;
            targetState.velocity.z = 0;
        }
    }

    public void onServerStateReceived(PlayerState serverState) {
        try {
            // Lưu server state gốc từ Server
            this.state.position.set(serverState.position);
            this.state.velocity.set(serverState.velocity);
            this.state.angle = serverState.angle;
            this.state.currentAnimation = serverState.currentAnimation;
            this.state.lastProcessedSequence = serverState.lastProcessedSequence;
            
            // CHỈ thực hiện Rollback & Replay khi nhận được State mới từ Server!
            reconcileWithServer();
        } catch (Exception e) {
            System.err.println("[RECONCILE ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void reconcileWithServer() {
        try {
            // eSports CSP: Rollback & Replay
            // 1. Loại bỏ các input cũ bé hơn hoặc bằng Sequence Server đã xử lý
            pendingInputs.removeIf(snap -> snap.sequence <= state.lastProcessedSequence);

            // 2. Rollback (Quay ngược thời gian): Đặt lại trạng thái predictedState bằng đúng vị trí Server
            predictedState.position.set(state.position);
            predictedState.velocity.set(state.velocity);
            predictedState.angle = state.angle;
            predictedState.currentAnimation = state.currentAnimation;

            // 3. Roll-forward (Replay - Phát lại): Tua cực nhanh các input chưa được Server xác nhận
            // Điều này giúp nhân vật giữ nguyên độ mượt cục bộ, bù đắp Lag mạng mà không bị kéo lùi
            // QUAN TRỌNG: Replay cũng cần check va chạm để tránh nhân vật xuyên tường rồi bị giật lại
            for (InputSnapshot snap : pendingInputs) {
                if (snap != null) {
                    simulatePhysicsState(predictedState, snap, lastCollisionSystem, lastObstacles);
                }
            }
        } catch (Exception e) {
            // Failure in replay: Fallback to server state safely
            predictedState.position.set(state.position);
            pendingInputs.clear();
        }
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
     * Lấy trạng thái đã được tính toán cục bộ (Client-Side Prediction).
     * Dùng cho NetworkManager lấy để gửi lên Server (MMO Trust Model).
     * @return Trạng thái dự đoán.
     */
    public PlayerState getPredictedState() {
        return predictedState;
    }

    public InputSnapshot getLatestSnapshot() {
        if (pendingInputs.isEmpty()) return null;
        return pendingInputs.getLast();
    }

    /**
     * Lấy góc quay hiện tại của nhân vật.
     * 
     * @return Góc quay (độ).
     */
    public float getRotation() {
        return state.angle;
    }

    public int getId() {
        return state.id;
    }

    public boolean isMoving() {
        return predictedState.velocity.len2() > 0.1f;
    }

    public boolean isOnGround() {
        // Hiện tại gán cứng là true vì chưa có logic rơi rụng phức tạp
        return true;
    }
}
