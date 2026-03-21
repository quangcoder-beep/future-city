package com.example.server_spring.config;

import com.esotericsoftware.kryonet.Server;
import com.example.server_spring.handler.KryoNetworkHandler;
import com.futurecity.shared.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Quản lý vòng đời KryoNet Server.
 * Bật/tắt socket, đăng ký packet classes.
 */
@Configuration
public class KryoServerConfig {

    private Server server;

    @Autowired
    private KryoNetworkHandler networkHandler;

    /**
     * Khởi động KryoNet Server.
     * Mở cổng TCP/UDP và đăng ký bộ lắng nghe (listener).
     */
    @PostConstruct
    public void start() throws Exception {
        // Tăng giới hạn Buffer (Write, Read) lên 1MB để gửi mảng Vector3 dài
        server = new Server(1048576, 1048576);
        Network.register(server);

        server.addListener(networkHandler);
        server.bind(54555, 54777);
        server.start();
        System.out.println("[NET] KryoNet Server started: TCP=54555 | UDP=54777");
    }

    /**
     * Dừng KryoNet Server khi đóng ứng dụng Spring.
     */
    @PreDestroy
    public void stop() {
        if (server != null) {
            server.close();
            server.stop();
            System.out.println("[NET] KryoNet Server stopped.");
        }
    }

    public Server getServer() {
        return server;
    }
}
