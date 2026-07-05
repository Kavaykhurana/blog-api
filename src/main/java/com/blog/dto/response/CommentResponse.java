package com.blog.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
    Long id,
    String content,
    UserResponse author,
    LocalDateTime createdAt,
    List<CommentResponse> replies
) {}
