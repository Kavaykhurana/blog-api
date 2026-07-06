package com.blog.controller;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.dto.response.PagedResponse;
import com.blog.model.User;
import com.blog.service.AuthService;
import com.blog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, AuthService authService) {
        this.commentService = commentService;
        this.authService = authService;
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody CommentRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        CommentResponse response = commentService.addComment(postId, request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<PagedResponse<CommentResponse>> getCommentsByPost(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PagedResponse<CommentResponse> response = commentService.getCommentsByPost(postId, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> editComment(
            @PathVariable("id") Long id,
            @Valid @RequestBody CommentRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        CommentResponse response = commentService.editComment(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable("id") Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        commentService.deleteComment(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
