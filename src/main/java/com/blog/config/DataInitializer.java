package com.blog.config;

import com.blog.model.Category;
import com.blog.model.Tag;
import com.blog.model.User;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

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
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting default data bootstrapping...");

        // Initialize admin user safely checking both username and email
        if (!userRepository.existsByUsername("admin") && !userRepository.existsByEmail("admin@blog.com")) {
            User admin = new User(
                "admin",
                "admin@blog.com",
                passwordEncoder.encode("admin123"),
                "Admin User",
                "Default administrator user",
                "ROLE_ADMIN"
            );
            userRepository.save(admin);
            log.info("Bootstrapped default admin user successfully.");
        } else if (userRepository.existsByUsername("admin") != userRepository.existsByEmail("admin@blog.com")) {
            log.warn("Database inconsistent: admin username or admin@blog.com email exists, but not both. Skipping admin bootstrap.");
        } else {
            log.info("Admin user already exists. Skipping admin bootstrap.");
        }

        // Initialize default categories
        initializeCategory("Technology", "Technology category");
        initializeCategory("Lifestyle", "Lifestyle category");

        // Initialize default tags
        initializeTag("java");
        initializeTag("springboot");

        log.info("Data bootstrapping completed.");
    }

    private void initializeCategory(String name, String description) {
        if (!categoryRepository.existsByNameIgnoreCase(name)) {
            Category category = new Category(name, description);
            categoryRepository.save(category);
            log.info("Bootstrapped category: {}", name);
        }
    }

    private void initializeTag(String name) {
        if (!tagRepository.existsByNameIgnoreCase(name)) {
            Tag tag = new Tag(name);
            tagRepository.save(tag);
            log.info("Bootstrapped tag: {}", name);
        }
    }
}
