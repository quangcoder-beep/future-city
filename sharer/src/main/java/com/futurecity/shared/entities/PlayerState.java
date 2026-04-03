package com.futurecity.shared.entities;

import com.badlogic.gdx.math.Vector3;

/**
 * PlayerState giống như một "Bản sao kỹ thuật số" của nhân vật.
 *
 * Server: Chỉnh sửa các con số trong PlayerState (Vị trí, Vận tốc).
 * Mạng: Vác cái PlayerState này bay từ Server đến Client.
 * Client: Đọc các con số trong PlayerState để vẽ nhân vật lên màn hình 3D cho
 * bạn xem.
 */
public class PlayerState {

    /**
     * ID định danh duy nhất của người chơi (do Server cấp).
     */
    public int id;

    /**
     * Tên người chơi.
     */
    public String username;

    /**
     * ID Cơ sở dữ liệu của người chơi (Dùng để so khớp đơn hàng).
     */
    public int dbUserId;

    /**
     * Vị trí hiện tại của người chơi trong thế giới game.
     * Server là nguồn quyết định (authoritative) vị trí này.
     */
    public final Vector3 position = new Vector3();

    /**
     * Vận tốc hiện tại của người chơi.
     * Server tính toán vận tốc dựa trên input và các yếu tố vật lý.
     */
    public final Vector3 velocity = new Vector3();

    /**
     * Tên của animation hiện tại đang được phát.
     * Ví dụ: "Idle", "Walk", "Run".
     * Server quyết định trạng thái (đang đi, đang chạy) và Client sẽ dựa vào đó để
     * chọn animation phù hợp.
     */
    public String currentAnimation;

    /**
     * Góc xoay của nhân vật (theo trục Y).
     * Server tính toán để Client xoay model cho đúng.
     */
    public float angle;
    public int credits;
    public int deliveries;
    public float reputation;

    /**
     * Dành cho Server để báo cáo lại Sequence của Client đã xử lý gần nhất.
     */
    public int lastProcessedSequence;

    public PlayerState() {
        // Constructor mặc định
    }

    /**
     * Cập nhật trạng thái từ một PlayerState khác.
     * Hữu ích cho việc đồng bộ hóa.
     * 
     * @param other State nguồn để sao chép dữ liệu.
     */
    public void set(PlayerState other) {
        this.id = other.id;
        this.username = other.username;
        this.dbUserId = other.dbUserId;
        this.position.set(other.position);
        this.velocity.set(other.velocity);
        this.currentAnimation = other.currentAnimation;
        this.angle = other.angle;
        this.credits = other.credits;
        this.deliveries = other.deliveries;
        this.reputation = other.reputation;
        this.lastProcessedSequence = other.lastProcessedSequence;
    }
}
