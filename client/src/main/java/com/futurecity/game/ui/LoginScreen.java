package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.widget.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.futurecity.game.core.GameScreen;
import com.futurecity.game.core.Main;
import com.futurecity.game.managers.HttpAuthClientManager;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.TokenStoreManager;

public class LoginScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private NetworkManager networkManager;

    private VisTextField userField;
    private VisTextField passField;
    private VisLabel statusLabel;
    private VisTextButton loginBtn;
    private VisTextButton registerBtn;

    public LoginScreen(Main game) {
        this.game = game;
        this.stage = new Stage(new ExtendViewport(800, 600));
        this.networkManager = new NetworkManager();

        setupUI();
        setupNetwork();
        tryAutoLogin(); // Thử tự động login bằng token đã lưu
    }

    private void setupUI() {
        VisTable table = new VisTable();
        table.setFillParent(true);

        // --- Thêm Background Phía Sau ---
        try {
            com.badlogic.gdx.graphics.Texture bgTex = game.myAssetManager.get("ui/backgrounds/1/Day/1.png",
                    com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.scenes.scene2d.ui.Image bgImage = new com.badlogic.gdx.scenes.scene2d.ui.Image(bgTex);
            bgImage.setFillParent(true);
            stage.addActor(bgImage); // Thêm background vào trước, nó sẽ nằm dưới
        } catch (Exception e) {
            System.out.println("Could not load login background: " + e.getMessage());
        }

        stage.addActor(table); // Thêm bảng UI lên trên

        VisLabel title = new VisLabel("FUTURE CITY - LOGIN", "title");
        table.add(title).padBottom(50).row();

        // Ô nhập Username
        table.add(new VisLabel("Username:")).left().padBottom(10).row();
        userField = new VisTextField("");
        table.add(userField).width(300).padBottom(20).row();

        // Ô nhập Password
        table.add(new VisLabel("Password:")).left().padBottom(10).row();
        passField = new VisTextField("");
        passField.setPasswordMode(true);
        passField.setPasswordCharacter('*');
        table.add(passField).width(300).padBottom(20).row();

        // Nút bấm
        VisTable btnTable = new VisTable();
        loginBtn = new VisTextButton("LOGIN");
        registerBtn = new VisTextButton("REGISTER");

        btnTable.add(loginBtn).width(140).padRight(20);
        btnTable.add(registerBtn).width(140);
        table.add(btnTable).padBottom(20).row();

        // Trạng thái thông báo
        statusLabel = new VisLabel("");
        statusLabel.setColor(Color.YELLOW); // Màu vàng nổi bật cho thông báo lỗi/trạng thái
        table.add(statusLabel).row();

        // Sự kiện nút Login
        loginBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleAuth(false);
            }
        });

        // Sự kiện nút Register
        registerBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleAuth(true);
            }
        });
    }

    private void setupNetwork() {
        networkManager.setLoginListener(response -> {
            Gdx.app.postRunnable(() -> {
                if (response.status.equals("SUCCESS")) {
                    statusLabel.setText("Success: " + response.message);
                    if (response.newId > 0) {
                        game.setScreen(new GameScreen(game, networkManager));
                    }
                } else {
                    // Token hết hạn hoặc lỗi -> xóa file token, hiển thị login
                    TokenStoreManager.clear();
                    statusLabel.setText("Error: " + response.message);
                    loginBtn.setDisabled(false);
                    registerBtn.setDisabled(false);
                    game.setScreen(LoginScreen.this);
                }
            });
        });
    }

    /**
     * Khi bật game, thử dùng token đã lưu để vào mà không cần nhập lại mật khẩu.
     */
    private void tryAutoLogin() {
        String[] tokens = TokenStoreManager.load();
        if (tokens == null)
            return; // Không có token -> hiện màn hình login bình thường

        String savedAccessToken = tokens[0];
        String savedRefreshToken = tokens[1];

        statusLabel.setText("Connecting...");
        loginBtn.setDisabled(true);
        registerBtn.setDisabled(true);

        // Sử dụng Refresh Token để lấy Access Token mới
        // Vì Access Token thường hết hạn nhanh, ta dùng Refresh Token để "tự động"
        // login lại
        HttpAuthClientManager httpClient = new HttpAuthClientManager("localhost", 8080);

        httpClient.refresh(savedRefreshToken, new HttpAuthClientManager.AuthCallback() {
            @Override
            public void onSuccess(int userId, String newAccessToken, String ignored) {
                // Refresh thành công -> lưu access token mới, kết nối KryoNet
                TokenStoreManager.save(newAccessToken, savedRefreshToken);
                try {
                    networkManager.connect("localhost");
                    networkManager.loginWithToken(newAccessToken);
                } catch (Exception e) {
                    statusLabel.setText("Connect to server failed");
                    loginBtn.setDisabled(false);
                    registerBtn.setDisabled(false);
                }
            }

            @Override
            public void onError(String message) {
                // Refresh Token hết hạn (30 ngày) -> phải login lại
                TokenStoreManager.clear();
                statusLabel.setText("Login session expired. Please login again.");
                loginBtn.setDisabled(false);
                registerBtn.setDisabled(false);
            }
        });
    }

    private void handleAuth(boolean isRegistering) {
        String user = userField.getText();
        String pass = passField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Error: Please enter user and pass");
            return;
        }

        loginBtn.setDisabled(true);
        registerBtn.setDisabled(true);
        statusLabel.setText("Connecting...");

        // Chuyển sang LoadingScreen lúc chờ mạng
        LoadingScreen waitScreen = new LoadingScreen(game, LoadingScreen.LoadingType.INDETERMINATE,
                "CONNECTING TO SERVER...");
        game.setScreen(waitScreen);

        // Tạo HTTP client để gọi REST API
        HttpAuthClientManager httpClient = new HttpAuthClientManager("localhost", 8080);

        if (isRegistering) {
            // REGISTER: Chỉ gọi HTTP, không kết nối KryoNet
            httpClient.register(user, pass, new HttpAuthClientManager.AuthCallback() {
                @Override
                public void onSuccess(int userId, String accessToken, String refreshToken) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Registration successful! Please press Login.");
                        loginBtn.setDisabled(false);
                        registerBtn.setDisabled(false);
                        game.setScreen(LoginScreen.this);
                    });
                }

                @Override
                public void onError(String message) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Error: " + message);
                        loginBtn.setDisabled(false);
                        registerBtn.setDisabled(false);
                        game.setScreen(LoginScreen.this);
                    });
                }
            });
        } else {
            // LOGIN: Gọi HTTP lấy Token → Kết nối KryoNet với Token
            httpClient.login(user, pass, new HttpAuthClientManager.AuthCallback() {
                @Override
                public void onSuccess(int userId, String accessToken, String refreshToken) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Login OK! Connecting to game...");
                        // Lưu cả 2 token xuống file để auto-login lần sau
                        if (accessToken != null && refreshToken != null) {
                            TokenStoreManager.save(accessToken, refreshToken);
                        }
                        try {
                            networkManager.connect("localhost");
                            networkManager.loginWithToken(accessToken);
                        } catch (Exception e) {
                            statusLabel.setText("Error: Cannot connect KryoNet");
                            loginBtn.setDisabled(false);
                            registerBtn.setDisabled(false);
                            game.setScreen(LoginScreen.this);
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Error: " + message);
                        loginBtn.setDisabled(false);
                        registerBtn.setDisabled(false);
                        game.setScreen(LoginScreen.this);
                    });
                }
            });
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        // KHÔNG dispose skin ở đây vì nó được get từ VisUI (dùng chung)!
    }
}
