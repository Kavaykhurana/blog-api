package com.blog.controller;

import com.blog.dto.request.UserUpdateRequest;
import com.blog.dto.response.PagedResponse;
import com.blog.dto.response.UserResponse;
import com.blog.model.User;
import com.blog.service.AuthService;
import com.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
