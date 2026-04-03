package com.example.server_spring.security;

import com.example.server_spring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Xử lý thông tin người dùng từ Google/Facebook sau khi đăng nhập thành công.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.server_spring.repository.AdminRepository adminRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String socialId = "";
        String avatarUrl = "";

        if ("google".equals(registrationId)) {
            socialId = (String) attributes.get("sub");
            avatarUrl = (String) attributes.get("picture");
        } else if ("facebook".equals(registrationId)) {
            socialId = (String) attributes.get("id");
            @SuppressWarnings("unchecked")
            Map<String, Object> picture = (Map<String, Object>) attributes.get("picture");
            if (picture != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) picture.get("data");
                if (data != null) {
                    avatarUrl = (String) data.get("url");
                }
            }
        }

        // Cập nhật hoặc tạo mới User trong DB
        int userId = -1;
        if ("google".equals(registrationId)) {
            userId = userRepository.getUserIdByGoogleId(socialId);
        } else {
            userId = userRepository.getUserIdByFacebookId(socialId);
        }

        if (userId == -1) {
            // Tạo mới
            if ("google".equals(registrationId)) {
                userId = userRepository.createSocialUser(socialId, null, avatarUrl);
            } else {
                userId = userRepository.createSocialUser(null, socialId, avatarUrl);
            }
        } else {
            // Cập nhật Avatar mới nhất
            userRepository.updateAvatar(userId, avatarUrl);
        }

        // --- BAN CHECK ---
        String ip = "";
        try {
            ip = ((org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                .getRequest().getRemoteAddr();
        } catch (Exception e) {}

        if (adminRepo.isBanned(userId, ip)) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                new org.springframework.security.oauth2.core.OAuth2Error("server_error"),
                "Tài khoản bị khóa đến: " + adminRepo.getBanReason(userId, ip));
        }

        // Truyền userId vào wrapper CustomOAuth2User để SuccessHandler lấy ra
        return new CustomOAuth2User(oAuth2User, userId);
    }
}
