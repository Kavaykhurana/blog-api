package com.blog.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String usernameOrEmail,
    @NotBlank String password
) {}
