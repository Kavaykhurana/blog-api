package com.blog.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
    Long id,
    String title,
    String content,
    String excerpt,
    String status,
    UserResponse author,
    List<CategoryResponse> categories,
    List<TagResponse> tags,
    long likesCount,
    boolean likedByCurrentUser,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
