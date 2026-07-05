package com.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PostRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String content,
    @Size(max = 300) String excerpt,
    String status, // DRAFT, PUBLISHED, ARCHIVED
    Set<String> categories,
    Set<String> tags
) {}
