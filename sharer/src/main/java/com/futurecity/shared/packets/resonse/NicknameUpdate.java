package com.futurecity.shared.packets.resonse;

/**
 * Gói tin cập nhật Nickname cho nhân vật.
 * Dùng để đồng bộ tức thời khi người chơi đổi tên trong Profile.
 */
public class NicknameUpdate {
    public int id; // KryoNet connection ID
    public String newNickname;

    public NicknameUpdate() {
    }

    public NicknameUpdate(int id, String newNickname) {
        this.id = id;
        this.newNickname = newNickname;
    }
}
