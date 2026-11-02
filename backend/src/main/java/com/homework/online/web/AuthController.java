package com.homework.online.web;

import com.homework.online.common.CurrentUser;
import com.homework.online.dto.AuthDtos;
import com.homework.online.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public AuthDtos.UserDto me() {
        return authService.me(CurrentUser.get().id());
    }

    @PutMapping("/profile")
    public AuthDtos.UserDto updateProfile(@RequestBody AuthDtos.ProfileRequest request) {
        return authService.updateProfile(CurrentUser.get().id(), request);
    }
}
