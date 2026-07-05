package com.blog.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String displayName,
    String bio,
    String role,
    LocalDateTime createdAt
) {}
