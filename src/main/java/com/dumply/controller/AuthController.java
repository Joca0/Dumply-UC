package com.dumply.controller;

import com.dumply.common.dto.*;
import com.dumply.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oracle.jdbc.proxy.annotation.Post;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Driver;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> login(@RequestBody LoginRequestDTO body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @PatchMapping("/complete-welcome")
    public ResponseEntity<ProfileDTO> completeWelcome() {
        return ResponseEntity.ok(authService.completeWelcome());
    }


    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> me() {
        return ResponseEntity.ok(authService.getLoggedUser());
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO> register(@RequestBody RegisterRequestDTO body) {
        return ResponseEntity.ok(authService.register(body));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/2fa/verify")
    public ResponseEntity<ResponseDTO> verify2FA(@RequestParam String email, @RequestParam String code) {
        return ResponseEntity.ok(authService.verify2FA(email, Integer.parseInt(code)));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setup2FA() {
        return ResponseEntity.ok(authService.setup2FA());
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<Void> confirm2FA(@RequestParam int code) {
        authService.confirmEnable2FA(code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/disable/request")
    public ResponseEntity<Void> requestDisable2FA() {
        authService.requestDisable2FACode();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/disable/confirm")
    public ResponseEntity<Void> confirmDisable2FA(@RequestParam String code) {
        authService.confirmDisable2FA(code);
        return ResponseEntity.ok().build();
    }


}
