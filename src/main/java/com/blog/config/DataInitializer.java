package com.blog.config;

import com.blog.model.Category;
import com.blog.model.Tag;
import com.blog.model.User;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           TagRepository tagRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize admin user
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                "admin",
                "admin@blog.com",
                passwordEncoder.encode("admin123"),
                "Admin User",
                "Default administrator user",
                "ROLE_ADMIN"
            );
            userRepository.save(admin);
        }

        // Initialize default categories
        initializeCategory("Technology", "Technology category");
        initializeCategory("Lifestyle", "Lifestyle category");

        // Initialize default tags
        initializeTag("java");
        initializeTag("springboot");
    }

    private void initializeCategory(String name, String description) {
        if (!categoryRepository.existsByNameIgnoreCase(name)) {
            Category category = new Category(name, description);
            categoryRepository.save(category);
        }
    }

    private void initializeTag(String name) {
        if (!tagRepository.existsByNameIgnoreCase(name)) {
            Tag tag = new Tag(name);
            tagRepository.save(tag);
        }
    }
}
