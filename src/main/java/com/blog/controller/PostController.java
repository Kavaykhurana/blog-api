package com.blog.controller;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PagedResponse;
import com.blog.dto.response.PostResponse;
import com.blog.model.User;
import com.blog.service.AuthService;
import com.blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final AuthService authService;

    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        PostResponse response = postService.createPost(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PostResponse>> getAllPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tag", required = false) String tag) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        PagedResponse<PostResponse> response = postService.getPosts(page, size, sortBy, sortDir, search, category, tag, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<PagedResponse<PostResponse>> getMyPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        PagedResponse<PostResponse> response = postService.getMyPosts(page, size, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable("id") Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        PostResponse response = postService.getPostById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable("id") Long id,
            @Valid @RequestBody PostRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        PostResponse response = postService.updatePost(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable("id") Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        postService.deletePost(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
