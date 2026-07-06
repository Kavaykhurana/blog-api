package com.blog.service;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.dto.response.PagedResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.model.Comment;
import com.blog.model.Post;
import com.blog.model.User;
import com.blog.repository.CommentRepository;
import com.blog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public static CommentResponse mapToResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            UserService.mapToResponse(comment.getAuthor()),
            comment.getCreatedAt(),
            comment.getReplies().stream().map(CommentService::mapToResponse).toList()
        );
    }

    public CommentResponse addComment(Long postId, CommentRequest request, User user) {
        if (user == null) {
            throw new UnauthorizedException("User is not authenticated or not found");
        }
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setContent(request.content());

        if (request.parentId() != null) {
            Comment parent = commentRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.parentId()));
            comment.setParent(parent);
        }

        Comment saved = commentRepository.save(comment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> getCommentsByPost(Long postId, int page, int size) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Comment> comments = commentRepository.findByPostAndParentIsNull(post, pageable);

        return new PagedResponse<>(
            comments.map(CommentService::mapToResponse).getContent(),
            comments.getNumber(),
            comments.getSize(),
            comments.getTotalElements(),
            comments.getTotalPages(),
            comments.isLast()
        );
    }

    public CommentResponse editComment(Long id, CommentRequest request, User currentUser) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        if (currentUser == null || (!comment.getAuthor().getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedException("You are not authorized to edit this comment");
        }

        comment.setContent(request.content());
        Comment updated = commentRepository.save(comment);
        return mapToResponse(updated);
    }

    public void deleteComment(Long id, User currentUser) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        if (currentUser == null || (!comment.getAuthor().getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedException("You are not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }
}
