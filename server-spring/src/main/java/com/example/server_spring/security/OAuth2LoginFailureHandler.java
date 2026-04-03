package com.example.server_spring.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý khi Social Login thất bại: Hiện thông báo lỗi và tự đóng trình duyệt.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
            "<html>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <title>NEURAL LINK CRITICAL FAILURE</title>" +
            "  <style>" +
            "    body { background-color: #050505; color: #ff0044; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; overflow: hidden; }" +
            "    .card { background: rgba(20, 20, 20, 0.95); border: 1px solid #ff0044; padding: 40px; border-radius: 4px; text-align: center; box-shadow: 0 0 20px rgba(255, 0, 68, 0.2); position: relative; max-width: 400px; }" +
            "    h1 { font-size: 24px; margin-bottom: 10px; letter-spacing: 2px; text-transform: uppercase; color: #ff0044; text-shadow: 0 0 10px rgba(255, 0, 68, 0.5); }" +
            "    p { color: rgba(255, 255, 255, 0.7); font-size: 14px; line-height: 1.6; }" +
            "    .error-code { color: #ffaa00; font-weight: bold; margin: 20px 0; font-family: monospace; border: 1px dashed #ffaa00; padding: 10px; }" +
            "    .btn { background: transparent; border: 1px solid #ff0044; color: #ff0044; padding: 12px 30px; text-transform: uppercase; letter-spacing: 1px; cursor: pointer; transition: 0.3s; margin-top: 20px; font-weight: bold; }" +
            "    .btn:hover { background: #ff0044; color: #000; box-shadow: 0 0 15px #ff0044; }" +
            "  </style>" +
            "</head>" +
            "<body>" +
            "  <div class='card'>" +
            "    <h1>Neural Link Failed</h1>" +
            "    <p>Authentication protocol rejected. External provider returned a critical signal.</p>" +
            "    <div class='error-code'>ERROR_LOG: " + exception.getMessage().toUpperCase() + "</div>" +
            "    <p style='font-size: 11px; opacity: 0.5;'>Manual de-initialization required if terminal remains active.</p>" +
            "    <button class='btn' onclick='window.close()'>Abort & Return</button>" +
            "    <script>" +
            "      // Tự đóng sau 5 giây để người dùng kịp đọc lỗi" +
            "      setTimeout(function(){ window.close(); }, 5000);" +
            "    </script>" +
            "  </div>" +
            "</body>" +
            "</html>"
        );
    }
}
