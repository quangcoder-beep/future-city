package com.example.server_spring;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.badlogic.gdx.graphics.GL20;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.reflect.Proxy;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Graphics;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class ServerApplication {

    @PostConstruct
    public void init() {
        // Force Vietnam Timezone for accurate logs and ban durations
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.out.println("[SERVER] Default Timezone set to Asia/Ho_Chi_Minh (UTC+7)");
    }

    public static void main(String[] args) {
        setupHeadlessEnvironment();
        SpringApplication.run(ServerApplication.class, args);
    }

    private static void setupHeadlessEnvironment() {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        if (Gdx.gl == null) {
            GL20 mockGL = (GL20) Proxy.newProxyInstance(
                    GL20.class.getClassLoader(),
                    new Class[] { GL20.class },
                    (proxy, method, a) -> {
                        if (method.getName().equals("hashCode"))
                            return System.identityHashCode(proxy);
                        if (method.getName().equals("equals"))
                            return proxy == a[0];
                        if (method.getName().equals("toString"))
                            return "MockGL";
                        if (method.getName().startsWith("glGen"))
                            return 1;
                        if (method.getReturnType().equals(int.class))
                            return 0;
                        if (method.getReturnType().equals(boolean.class))
                            return true;
                        return null;
                    });
            Gdx.gl = mockGL;
            Gdx.gl20 = mockGL;
            System.out.println("[SERVER] Mock OpenGL ready.");
        }
        if (Gdx.files == null) {
            Gdx.files = new HeadlessFiles();
            System.out.println("[SERVER] HeadlessFiles ready.");
        }
        if (Gdx.app == null) {
            Gdx.app = (Application) Proxy.newProxyInstance(
                    Application.class.getClassLoader(),
                    new Class[] { Application.class },
                    (proxy, method, a) -> {
                        if (method.getName().equals("hashCode"))
                            return System.identityHashCode(proxy);
                        if (method.getName().equals("equals"))
                            return proxy == a[0];
                        if (method.getName().equals("toString"))
                            return "MockApplication";
                        if (method.getName().equals("getType"))
                            return Application.ApplicationType.HeadlessDesktop;
                        if (method.getReturnType().equals(int.class))
                            return 0;
                        if (method.getReturnType().equals(long.class))
                            return 0L;
                        if (method.getReturnType().equals(float.class))
                            return 0f;
                        if (method.getReturnType().equals(boolean.class))
                            return false;
                        return null;
                    });
            System.out.println("[SERVER] Mock Application ready.");
        }
        if (Gdx.graphics == null) {
            Gdx.graphics = (Graphics) Proxy.newProxyInstance(
                    Graphics.class.getClassLoader(),
                    new Class[] { Graphics.class },
                    (proxy, method, a) -> {
                        if (method.getName().equals("hashCode"))
                            return System.identityHashCode(proxy);
                        if (method.getName().equals("equals"))
                            return proxy == a[0];
                        if (method.getName().equals("toString"))
                            return "MockGraphics";
                        if (method.getReturnType().equals(int.class))
                            return 0;
                        if (method.getReturnType().equals(float.class))
                            return 0f;
                        if (method.getReturnType().equals(boolean.class))
                            return true;
                        if (method.getReturnType().equals(long.class))
                            return 0L;
                        return null;
                    });
            System.out.println("[SERVER] Mock Graphics ready.");
        }
    }
}
