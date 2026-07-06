package com.blog.repository;

import com.blog.model.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class PostSpecification {

    public static Specification<Post> hasStatus(PostStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Post> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.trim().isEmpty()) {
                return null;
            }
            query.distinct(true);
            Join<Post, Category> categoryJoin = root.join("categories");
            return cb.equal(cb.lower(categoryJoin.get("name")), category.trim().toLowerCase());
        };
    }

    public static Specification<Post> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.trim().isEmpty()) {
                return null;
            }
            query.distinct(true);
            Join<Post, Tag> tagJoin = root.join("tags");
            return cb.equal(cb.lower(tagJoin.get("name")), tag.trim().toLowerCase());
        };
    }

    public static Specification<Post> searchByKeyword(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Predicate titlePredicate = cb.like(cb.lower(root.get("title")), pattern);
            Predicate contentPredicate = cb.like(cb.lower(root.get("content")), pattern);
            return cb.or(titlePredicate, contentPredicate);
        };
    }

    public static Specification<Post> hasAuthor(User author) {
        return (root, query, cb) -> author == null ? null : cb.equal(root.get("author"), author);
    }
}
