package com.dumply.common.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
