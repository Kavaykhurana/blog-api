package com.blog.repository;

import com.blog.model.Post;
import com.blog.model.PostLike;
import com.blog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    long countByPost(Post post);
    boolean existsByPostAndUser(Post post, User user);
}
