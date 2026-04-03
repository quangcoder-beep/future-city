package com.example.server_spring.security;

import com.example.server_spring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private com.example.server_spring.repository.AdminRepository adminRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Kiểm tra User tồn tại
        int userId = userRepo.getUserIdByUsername(username);
        String passwordHash = userRepo.getPasswordHash(username);
        if (passwordHash == null || userId <= 0) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // 2. --- KIỂM TRA BAN (CHỐT CHẶN CUỐI CÙNG) ---
        String ip = "";
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                ip = attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {}

        if (userId > 0 && adminRepo.isBanned(userId, ip)) {
            // Ném ngoại lệ Locked để Spring Security hiển thị thông báo "Tài khoản bị khóa"
            throw new org.springframework.security.authentication.LockedException(
                "Tài khoản này đang bị khóa. Thời gian mở: " + adminRepo.getBanReason(userId, ip));
        }

        String role = userRepo.getRoleByUsername(username);
        // Spring Security yêu cầu Prefix ROLE_ cho phân quyền theo Role
        String springRole = "ROLE_" + (role != null ? role.toUpperCase() : "PLAYER");

        return new User(
                username,
                passwordHash,
                Collections.singletonList(new SimpleGrantedAuthority(springRole))
        );
    }
}
