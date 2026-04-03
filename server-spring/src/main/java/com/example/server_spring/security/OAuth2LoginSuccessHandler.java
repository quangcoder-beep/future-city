package com.example.server_spring.security;

import com.example.server_spring.repository.UserRepository;
import com.example.server_spring.services.SessionService;
import com.futurecity.shared.packets.resonse.LoginResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý sau khi Social Login thành công: Tạo JWT và lưu vào SessionService cho
 * Client Polling.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // Lấy userId thực sự từ CustomOAuth2User
        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
        int userId = customUser.getUserId();

        // Tạo JWT
        String accessToken = jwtUtil.generateToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId); // Thêm Refresh Token

        LoginResponse loginResponse = new LoginResponse(userId, accessToken);
        loginResponse.refreshToken = refreshToken; // Lưu vào response
        loginResponse.status = "SUCCESS";
        loginResponse.nickname = userRepository.getNickname(userId);
        loginResponse.avatarUrl = userRepository.getAvatarUrl(userId);
        loginResponse.needsNickname = (loginResponse.nickname == null || loginResponse.nickname.isEmpty());

        // Lấy sessionId từ HttpSession (đã được lưu tại social-login entry point)
        HttpSession session = request.getSession();
        String pollingSessionId = (String) session.getAttribute("pollingSessionId");
        System.out.println("DEBUG: OAuth2 Success. PollingSessionId from Session: " + pollingSessionId);

        if (pollingSessionId != null) {
            sessionService.saveSession(pollingSessionId, loginResponse);
            System.out.println("DEBUG: Saved LoginResponse to SessionService for polling.");
        } else {
            System.err.println("DEBUG ERROR: PollingSessionId is NULL. Game will not resume!");
        }

        // Phản hồi cho trình duyệt: Đóng trình duyệt hoặc hiện thông báo thành công
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
            "<html>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <title>NEURAL LINK ESTABLISHED</title>" +
            "  <style>" +
            "    body { background-color: #050505; color: #00f2ff; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; overflow: hidden; }" +
            "    .card { background: rgba(20, 20, 20, 0.95); border: 1px solid #00f2ff; padding: 40px; border-radius: 4px; text-align: center; box-shadow: 0 0 20px rgba(0, 242, 255, 0.2); position: relative; max-width: 450px; }" +
            "    h1 { font-size: 24px; margin-bottom: 15px; letter-spacing: 2px; text-transform: uppercase; color: #00f2ff; text-shadow: 0 0 10px rgba(0, 242, 255, 0.5); }" +
            "    p { color: rgba(255, 255, 255, 0.7); font-size: 14px; line-height: 1.6; }" +
            "    .status { color: #50ffab; font-weight: bold; margin: 20px 0; font-family: monospace; }" +
            "    .btn { background: transparent; border: 1px solid #00f2ff; color: #00f2ff; padding: 15px 25px; text-transform: uppercase; letter-spacing: 1px; cursor: pointer; transition: 0.3s; margin-top: 20px; font-weight: bold; font-size: 13px; }" +
            "    .btn:hover { background: #00f2ff; color: #000; box-shadow: 0 0 15px #00f2ff; }" +
            "    .fallback-msg { display: none; margin-top: 20px; color: #ff0055; font-weight: bold; border-top: 1px solid #333; padding-top: 15px; }" +
            "  </style>" +
            "</head>" +
            "<body>" +
            "  <div class='card'>" +
            "    <h1 id='title'>Login Successful</h1>" +
            "    <p id='desc'>Protocol verified. Connection established with neural network.</p>" +
            "    <div class='status' id='status'>SYNCHRONIZATION COMPLETED</div>" +
            "    <button class='btn' id='closeBtn' onclick='closeWindow()'>LOGIN SUCCESSFUL, PLEASE RETURN TO GAME</button>" +
            "    <div id='fallback' class='fallback-msg'>TAB CANNOT AUTO-CLOSE.<br>PLEASE ALT-TAB MANUALLY</div>" +
            "    <script>" +
            "      function closeWindow() {" +
            "         window.open('', '_self', '');" +
            "         window.close();" +
            "         setTimeout(function() {" +
            "            window.open('about:blank', '_self').close();" +
            "         }, 100);" +
            "         setTimeout(function() {" +
            "            document.getElementById('fallback').style.display = 'block';" +
            "            document.getElementById('closeBtn').style.display = 'none';" +
            "         }, 1000);" +
            "      }" +
            "      setTimeout(closeWindow, 2000);" +
            "    </script>" +
            "  </div>" +
            "</body>" +
            "</html>"
        );
    }
}
