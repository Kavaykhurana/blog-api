package com.blog.dto.response;

public record AuthResponse(
    String token,
    String username,
    String email,
    String role
) {}
