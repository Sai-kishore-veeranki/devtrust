package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Under /api/admin/** — already restricted to ROLE_ADMIN by the
 * SecurityConfig rule added in Phase B for AdminInviteController.
 * That single requestMatchers("/api/admin/**") rule covers this new
 * controller automatically, nothing to add here.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserProfileResponse> listUsers() {
        return userService.listAllUsers();
    }
}
