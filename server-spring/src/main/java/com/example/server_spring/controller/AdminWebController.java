package com.example.server_spring.controller;

import com.example.server_spring.services.AdminService;
import com.example.server_spring.services.DashboardService;
import com.example.server_spring.repository.AdminRepository;
import com.example.server_spring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import jakarta.annotation.PostConstruct;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    @PostConstruct
    public void initAdmin() {
        // Cưỡng chế: Nếu user 'admin' chưa tồn tại thì tạo mới
        // Nếu đã tồn tại thì BẮT BUỘC cập nhật Role thành 'ADMIN' (không dùng ROLE_ prefix ở DB)
        try {
            int userId = userRepo.getUserIdByUsername("admin");
            if (userId == -1) {
                userRepo.registerUser("admin", encoder.encode("admin123"), "System Admin");
                System.out.println("[SECURITY] Created default admin account: admin / admin123");
            }
            
            // Ép quyền bằng SQL thô để đảm bảo 100% thành công
            jdbc.update("UPDATE Users SET Role = 'ADMIN' WHERE Username = 'admin'");
            System.out.println("[SECURITY] Enforced ADMIN role for user: admin");
            
        } catch (Exception e) {
            System.err.println("[SECURITY-ERROR] Failed to initialize default admin: " + e.getMessage());
        }
    }

    private String getLoggedInUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        try {
            // Sử dụng DashboardService để lấy toàn bộ dữ liệu chỉ với 1 dòng code
            model.addAllAttributes(dashboardService.getDashboardData());
            
        } catch (Exception e) {
            System.err.println("[ADMIN-ERROR] Critical failure in dashboard controller: " + e.getMessage());
            e.printStackTrace();
            // Cung cấp các giá trị mặc định tối thiểu nếu có lỗi cực nặng
            model.addAttribute("players", List.of());
            model.addAttribute("ramUsage", 0);
            model.addAttribute("maxRam", 1);
        }
        return "admin/dashboard";
    }

    @PostMapping("/kick")
    public String kickPlayer(@RequestParam("id") int connectionId, 
                             @RequestParam(value = "reason", defaultValue = "Kicked by Admin") String reason) {
        adminService.kickPlayer(connectionId, reason, getLoggedInUser());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/ban")
    public String banPlayer(@RequestParam("id") int connectionId,
                            @RequestParam("userId") int userId,
                            @RequestParam("ip") String ip,
                            @RequestParam("reason") String reason,
                            @RequestParam(value = "duration", required = false) Integer duration,
                            Principal principal) {
        adminService.banPlayer(connectionId, userId, ip, reason, duration, principal.getName());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/unban")
    public String unbanPlayer(@RequestParam("banId") int banId, Principal principal) {
        adminService.unbanPlayer(banId, principal.getName());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/warn")
    public String warnPlayer(@RequestParam("id") int connectionId, 
                             @RequestParam("reason") String reason) {
        adminService.warnPlayer(connectionId, reason, getLoggedInUser());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/warp")
    public String warpPlayer(@RequestParam("id") int connectionId) {
        adminService.warpPlayerToSpawn(connectionId, getLoggedInUser());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/clearLogs")
    public String clearLogs() {
        dashboardService.clearSystemLogs();
        adminRepo.logAction(getLoggedInUser(), "CLEAR_LOGS", null, "Force cleared all system and audit logs");
        return "redirect:/admin/dashboard";
    }
}
