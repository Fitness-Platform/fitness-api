package com.fitnessplatform.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> findAll(
            @RequestParam(required = false) String email
    ) {
        return adminUserService.findAll(email)
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @GetMapping("/{userId}")
    public AdminUserResponse findById(@PathVariable UUID userId) {
        return AdminUserResponse.from(adminUserService.findById(userId));
    }
}
