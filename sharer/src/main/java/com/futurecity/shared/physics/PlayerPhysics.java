package com.futurecity.shared.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.AnimationName;

/**
 * Class chứa TOÀN BỘ logic vật lý và di chuyển của nhân vật.
 * Class này được dùng chung cho cả Client và Server:
 * - Server: Tính toán authoritative state
 * - Client: Client-side prediction
 * 
 * Điều này đảm bảo logic giữa Client và Server GIỐNG Y HỆT 100%.
 */
public class PlayerPhysics {

    // ThreadLocal: mỗi thread có bộ vector riêng — thread-safe không cần synchronized
    private static final ThreadLocal<Vector3[]> TL_VECS = ThreadLocal.withInitial(
        () -> new Vector3[] { new Vector3(), new Vector3(), new Vector3() }
    );

    /**
     * Xử lý input và cập nhật vận tốc của người chơi.
     * 
     * @param state         PlayerState hiện tại (sẽ được cập nhật)
     * @param horizontal    Input trục ngang (-1 = trái, 0 = không, 1 = phải)
     * @param vertical      Input trục dọc (-1 = lui, 0 = không, 1 = tiến)
     * @param isRunning     Có đang giữ nút chạy không
     * @param cameraYaw     Góc xoay của camera (dùng để tính hướng di chuyển)
     * @param isFirstPerson Có đang ở góc nhìn thứ nhất không
     * @param deltaTime     Thời gian giữa các frame
     * @return Tên animation nên phát (IDLE/WALK/RUN)
     */
    public static String processInput(PlayerState state, float horizontal, float vertical,
            boolean isRunning, float cameraYaw,
            boolean isFirstPerson, float deltaTime) {

        float targetMaxSpeed = isRunning ? GameConstants.PLAYER_RUN_SPEED : GameConstants.PLAYER_WALK_SPEED;

        // Lấy vector từ ThreadLocal — an toàn khi chạy multi-thread
        Vector3[] v = TL_VECS.get();
        Vector3 tempForward = v[0];
        Vector3 tempRight   = v[1];
        Vector3 tempMove    = v[2];

        if (horizontal != 0 || vertical != 0) {
            // Tính hướng di chuyển theo Camera
            float sinYaw = MathUtils.sinDeg(cameraYaw);
            float cosYaw = MathUtils.cosDeg(cameraYaw);

            tempForward.set(sinYaw, 0, cosYaw).nor(); // Hướng trước mặt camera
            tempRight.set(tempForward).rotate(Vector3.Y, -90); // Hướng bên phải camera

            tempMove.setZero();
            // Cộng vector hướng
            if (vertical > 0)
                tempMove.add(tempForward);
            else if (vertical < 0)
                tempMove.sub(tempForward);

            if (horizontal > 0)
                tempMove.add(tempRight);
            else if (horizontal < 0)
                tempMove.sub(tempRight);

            tempMove.nor();

            // Tính vận tốc mục tiêu
            Vector3 targetVel = tempMove.scl(targetMaxSpeed);

            // Lerp vận tốc (Tạo đà quán tính) - QUAN TRỌNG: Đây là acceleration
            // CRITICAL: Clamp alpha về [0, 1] để tránh overshoot!
            float accelAlpha = MathUtils.clamp(GameConstants.PLAYER_ACCELERATION * deltaTime, 0f, 1f);
            state.velocity.x = MathUtils.lerp(state.velocity.x, targetVel.x, accelAlpha);
            state.velocity.z = MathUtils.lerp(state.velocity.z, targetVel.z, accelAlpha);

            // Xoay nhân vật theo hướng đi (Chỉ xoay ở TPS)
            if (!isFirstPerson) {
                float targetAngle = MathUtils.atan2(state.velocity.x, state.velocity.z) * MathUtils.radiansToDegrees;
                state.angle = lerpAngleDeg(state.angle, targetAngle, 10f * deltaTime);
            }
        } else {
            // Giảm tốc khi thả phím - QUAN TRỌNG: Đây là deceleration
            // CRITICAL: Clamp alpha về [0, 1] để tránh overshoot!
            float decelAlpha = MathUtils.clamp(GameConstants.PLAYER_DECELERATION * deltaTime, 0f, 1f);
            state.velocity.x = MathUtils.lerp(state.velocity.x, 0, decelAlpha);
            state.velocity.z = MathUtils.lerp(state.velocity.z, 0, decelAlpha);
        }

        // Tính animation dựa trên vận tốc
        return calculateAnimation(state, isRunning);
    }

    /**
     * Tính toán trọng lực và cập nhật vị trí Y.
     * 
     * @param state     PlayerState hiện tại
     * @param groundY   Độ cao mặt đất tại vị trí hiện tại
     * @param deltaTime Thời gian giữa các frame
     */
    public static void applyGravity(PlayerState state, float groundY, float deltaTime) {
        // Cập nhật vị trí Y dựa trên vận tốc rơi
        state.position.y += state.velocity.y * deltaTime;

        // Kiểm tra va chạm với mặt đất
        if (state.position.y <= groundY) {
            state.position.y = groundY;
            state.velocity.y = 0;
        } else {
            // Áp dụng trọng lực (gia tốc rơi)
            state.velocity.y += GameConstants.GRAVITY * deltaTime;
        }
    }

    /**
     * Tính toán tên animation dựa vào vận tốc hiện tại.
     */
    private static String calculateAnimation(PlayerState state, boolean isRunning) {
        // Tự động phát hiện đang di chuyển dựa vào vận tốc
        float speedSq = state.velocity.x * state.velocity.x + state.velocity.z * state.velocity.z;
        boolean isMoving = speedSq > 0.1f;

        if (isMoving) {
            return isRunning ? AnimationName.RUN.getValue() : AnimationName.WALK.getValue();
        } else {
            return AnimationName.IDLE.getValue();
        }
    }

    /**
     * Lerp angle với xử lý wrap-around đúng (0-360 degrees).
     * Helper function từ code cũ.
     */
    private static float lerpAngleDeg(float from, float to, float progress) {
        float delta = ((to - from + 360 + 180) % 360) - 180;
        return (from + delta * progress + 360) % 360;
    }
}
