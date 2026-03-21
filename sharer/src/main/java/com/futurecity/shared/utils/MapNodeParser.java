package com.futurecity.shared.utils;

import com.futurecity.shared.enums.MapNodeType;

/**
 * Parser dùng để tách thông tin từ tên Node trong Blender.
 * Định dạng: loại_id_tênHiểnThị
 * Ví dụ: "npc_jane_Cô Chủ Tiệm"
 */
public class MapNodeParser {

    public static MapNodeData parse(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return new MapNodeData(MapNodeType.UNKNOWN, "", "unknown", "", "");
        }

        // Tách theo dấu gạch dưới
        String[] parts = nodeId.split("_");

        // 1. Tìm MapNodeType
        MapNodeType type = MapNodeType.fromId(nodeId);

        // 2. Lấy ID thật (Phần tử thứ 2)
        String id = (parts.length > 1) ? parts[1] : nodeId;

        // 3. Lấy tên hiển thị (Phần tử thứ 3 trở đi)
        StringBuilder displayName = new StringBuilder();
        if (parts.length > 2) {
            for (int i = 2; i < parts.length; i++) {
                displayName.append(parts[i]);
                if (i < parts.length - 1)
                    displayName.append(" ");
            }
        } else {
            displayName.append(id);
        }

        String prompt = type.getDefaultPrompt();

        return new MapNodeData(type, nodeId, id, displayName.toString(), prompt);
    }
}
