package com.blog.controller;

import com.blog.dto.response.UserResponse;
import com.blog.model.User;
import com.blog.service.AuthService;
import com.blog.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts/{postId}")
public class LikeController {

    private final LikeService likeService;
    private final AuthService authService;

    public LikeController(LikeService likeService, AuthService authService) {
        this.likeService = likeService;
        this.authService = authService;
    }

    @PostMapping("/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(@PathVariable("postId") Long postId) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        boolean liked = likeService.toggleLike(postId, currentUser);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @GetMapping("/likes")
    public ResponseEntity<List<UserResponse>> getUsersWhoLiked(@PathVariable("postId") Long postId) {
        List<UserResponse> response = likeService.getUsersWhoLiked(postId);
        return ResponseEntity.ok(response);
    }
}
