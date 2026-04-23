package com.dumply.common.dto;

public record ChangePasswordRequest(String oldPassword, String newPassword) {}
