package com.example.server_spring.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper cho OAuth2User để đính kèm thêm userId từ Database.
 */
public class CustomOAuth2User implements OAuth2User {
    private final OAuth2User oauth2User;
    private final int userId;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(OAuth2User oauth2User, int userId) {
        this.oauth2User = oauth2User;
        this.userId = userId;
        // Copy attributes và thêm userId vào để thuận tiện truy xuất
        this.attributes = new HashMap<>(oauth2User.getAttributes());
        this.attributes.put("db_user_id", userId);
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }
}
