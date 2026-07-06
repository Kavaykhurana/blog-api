package com.blog.service;

import com.blog.dto.request.UserUpdateRequest;
import com.blog.dto.response.PagedResponse;
import com.blog.dto.response.UserResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.model.User;
import com.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static UserResponse mapToResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getBio(),
            user.getRole(),
            user.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);
        return new PagedResponse<>(
            users.map(UserService::mapToResponse).getContent(),
            users.getNumber(),
            users.getSize(),
            users.getTotalElements(),
            users.getTotalPages(),
            users.isLast()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request, User currentUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (currentUser == null || (!user.getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedException("You are not authorized to update this profile");
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    public void deleteUser(Long id, User currentUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (currentUser == null || (!user.getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedException("You are not authorized to delete this profile");
        }

        userRepository.delete(user);
    }
}
