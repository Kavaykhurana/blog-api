package com.blog.service;

import com.blog.dto.response.UserResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Post;
import com.blog.model.PostLike;
import com.blog.model.User;
import com.blog.repository.PostLikeRepository;
import com.blog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    public LikeService(PostLikeRepository postLikeRepository, PostRepository postRepository) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
    }

    public boolean toggleLike(Long postId, User user) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            return false; // un-liked
        } else {
            postLikeRepository.save(new PostLike(post, user));
            return true; // liked
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersWhoLiked(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        return post.getLikes().stream()
            .map(PostLike::getUser)
            .map(UserService::mapToResponse)
            .toList();
    }
}
