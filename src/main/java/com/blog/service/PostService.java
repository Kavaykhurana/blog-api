package com.blog.service;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PagedResponse;
import com.blog.dto.response.PostResponse;
import com.blog.exception.BadRequestException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.model.*;
import com.blog.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostLikeRepository postLikeRepository;

    public PostService(PostRepository postRepository, CategoryRepository categoryRepository,
                       TagRepository tagRepository, PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.postLikeRepository = postLikeRepository;
    }

    public PostResponse mapToResponse(Post post, User currentUser) {
        long likesCount = postLikeRepository.countByPost(post);
        boolean liked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getExcerpt(),
            post.getStatus().name(),
            UserService.mapToResponse(post.getAuthor()),
            post.getCategories().stream().map(CategoryService::mapToResponse).toList(),
            post.getTags().stream().map(TagService::mapToResponse).toList(),
            likesCount,
            liked,
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }

    public PostResponse createPost(PostRequest request, User user) {
        Post post = new Post();
        post.setAuthor(user);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setExcerpt(request.excerpt());

        PostStatus status = PostStatus.DRAFT;
        if (request.status() != null) {
            try {
                status = PostStatus.valueOf(request.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid post status. Allowed values: DRAFT, PUBLISHED, ARCHIVED");
            }
        }
        post.setStatus(status);
        if (status == PostStatus.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
        }

        mapCategoriesAndTags(post, request.categories(), request.tags());
        Post saved = postRepository.save(post);
        return mapToResponse(saved, user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getPosts(int page, int size, String sortBy, String sortDir,
                                                String search, String category, String tag, User currentUser) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Post> spec = Specification.where(null);

        // Public viewers can only see published posts. Admin / author filters are applied in spec if customized,
        // but by default, public endpoint shows only published.
        spec = spec.and(PostSpecification.hasStatus(PostStatus.PUBLISHED));

        if (search != null && !search.isBlank()) {
            spec = spec.and(PostSpecification.searchByKeyword(search));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(PostSpecification.hasCategory(category));
        }
        if (tag != null && !tag.isBlank()) {
            spec = spec.and(PostSpecification.hasTag(tag));
        }

        Page<Post> posts = postRepository.findAll(spec, pageable);
        return new PagedResponse<>(
            posts.map(p -> mapToResponse(p, currentUser)).getContent(),
            posts.getNumber(),
            posts.getSize(),
            posts.getTotalElements(),
            posts.getTotalPages(),
            posts.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getMyPosts(int page, int size, User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Post> spec = PostSpecification.hasAuthor(user);
        Page<Post> posts = postRepository.findAll(spec, pageable);
        return new PagedResponse<>(
            posts.map(p -> mapToResponse(p, user)).getContent(),
            posts.getNumber(),
            posts.getSize(),
            posts.getTotalElements(),
            posts.getTotalPages(),
            posts.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id, User currentUser) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        return mapToResponse(post, currentUser);
    }

    public PostResponse updatePost(Long id, PostRequest request, User currentUser) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to edit this post");
        }

        post.setTitle(request.title());
        post.setContent(request.content());
        post.setExcerpt(request.excerpt());

        if (request.status() != null) {
            try {
                PostStatus newStatus = PostStatus.valueOf(request.status().toUpperCase());
                if (newStatus == PostStatus.PUBLISHED && post.getStatus() != PostStatus.PUBLISHED) {
                    post.setPublishedAt(LocalDateTime.now());
                }
                post.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status");
            }
        }

        mapCategoriesAndTags(post, request.categories(), request.tags());
        Post updated = postRepository.save(post);
        return mapToResponse(updated, currentUser);
    }

    public void deletePost(Long id, User currentUser) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (!post.getAuthor().getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ROLE_ADMIN")) {
            throw new UnauthorizedException("You are not authorized to delete this post");
        }

        postRepository.delete(post);
    }

    private void mapCategoriesAndTags(Post post, Set<String> catNames, Set<String> tagNames) {
        Set<Category> categories = new HashSet<>();
        if (catNames != null) {
            for (String name : catNames) {
                if (name == null || name.trim().isEmpty()) continue;
                Category cat = categoryRepository.findByNameIgnoreCase(name.trim())
                    .orElseGet(() -> categoryRepository.save(new Category(name.trim(), "Auto-created Category")));
                categories.add(cat);
            }
        }
        post.setCategories(categories);

        Set<Tag> tags = new HashSet<>();
        if (tagNames != null) {
            for (String name : tagNames) {
                if (name == null || name.trim().isEmpty()) continue;
                String cleanName = name.replaceAll("#", "").trim();
                if (cleanName.isEmpty()) continue;
                Tag tag = tagRepository.findByNameIgnoreCase(cleanName)
                    .orElseGet(() -> tagRepository.save(new Tag(cleanName)));
                tags.add(tag);
            }
        }
        post.setTags(tags);
    }
}
