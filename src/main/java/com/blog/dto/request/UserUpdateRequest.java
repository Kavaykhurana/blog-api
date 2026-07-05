package com.blog.dto.request;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(max = 100) String displayName,
    String bio
) {}
