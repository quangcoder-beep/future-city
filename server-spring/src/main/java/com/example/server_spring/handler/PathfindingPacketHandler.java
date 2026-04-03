package com.example.server_spring.handler;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.services.MapService;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.request.PathfindingRequest;
import com.futurecity.shared.packets.resonse.PathfindingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles pathfinding (GPS) packets.
 * - Receives target coordinates from Client.
 * - Finds path using A* algorithm in a background thread.
 * - Returns result to Client.
 */
@Component
public class PathfindingPacketHandler {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private MapService mapService;

    /**
     * Processes pathfinding requests.
     * Runs A* algorithm on a separate thread (async) to avoid blocking the Game Loop.
     */
    public void handlePathfinding(Connection conn, PathfindingRequest req) {
        // Get starting position: prefer currentPos from request, otherwise use player state
        Vector3 startPos = req.currentPos;
        if (startPos == null) {
            ServerPlayer player = playerService.get(conn.getID());
            if (player != null) {
                startPos = new Vector3(player.getState().position);
            }
        }

        final Vector3 finalStart = startPos;
        final Vector3 finalTarget = new Vector3(req.targetPos);

        // Run A* on background thread
        CompletableFuture.runAsync(() -> {
            try {
                List<Vector3> path = mapService.findPath(finalStart, finalTarget);

                // Wrap result and send back to Client
                PathfindingResponse resp = new PathfindingResponse();
                resp.path = path;
                resp.success = (path != null && !path.isEmpty());

                // Synchronize on connection to avoid KryoNet thread-safety issues
                synchronized (conn) {
                    conn.sendTCP(resp);
                }
            } catch (Exception e) {
                System.err.println("[Pathfinding] Failed to find path: " + e.getMessage());
                PathfindingResponse errorResp = new PathfindingResponse();
                errorResp.success = false;
                errorResp.path = null;
                synchronized (conn) {
                    conn.sendTCP(errorResp);
                }
            }
        });
    }
}
