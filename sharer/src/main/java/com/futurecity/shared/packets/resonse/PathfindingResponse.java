package com.futurecity.shared.packets.resonse;

import com.badlogic.gdx.math.Vector3;
import java.util.List;

/**
 * GĂ³i tin tráº£ vá» Ä‘Æ°á»ng Ä‘i tá»« Server cho Client.
 */
public class PathfindingResponse {
    public boolean success;
    public List<Vector3> path;

    public PathfindingResponse() {
    }
}
