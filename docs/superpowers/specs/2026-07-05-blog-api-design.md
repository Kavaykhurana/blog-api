# Design Document: Blog Platform API
**Date:** 2026-07-05
**Status:** Approved

## 1. Overview
A RESTful, production-ready blog API built with Spring Boot 3.x, Java 25, PostgreSQL, and Spring Security. The project includes role-based access control, JWT-based stateless authentication, dynamic searching/filtering with JPA Specifications, hierarchical threaded comments, post likes, and tag management.

To keep the codebase clean and native, we do not use Lombok; instead, standard Java classes with custom builders/accessors are used for entities, and Java `record` types are used for immutable DTOs.

---

## 2. System Architecture & Tech Stack
- **Language:** Java 25
- **Framework:** Spring Boot 3.x (Spring Data JPA, Spring Security, Spring Validation)
- **Database:** PostgreSQL
- **Security:** Spring Security 6.x (Stateless JWT Authentication, BCrypt Password Hashing)
- **Build Tool:** Maven

---

## 3. Database Schema

### `users`
- `id` (BIGINT, PK, Generated Identity)
- `username` (VARCHAR(50), UNIQUE, NOT NULL)
- `email` (VARCHAR(100), UNIQUE, NOT NULL)
- `password` (VARCHAR(255), NOT NULL)
- `display_name` (VARCHAR(100))
- `bio` (TEXT)
- `role` (VARCHAR(20), DEFAULT 'ROLE_USER')
- `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)

### `posts`
- `id` (BIGINT, PK, Generated Identity)
- `user_id` (BIGINT, FK -> users.id, NOT NULL)
- `title` (VARCHAR(200), NOT NULL)
- `content` (TEXT, NOT NULL)
- `excerpt` (VARCHAR(300))
- `status` (VARCHAR(20), DEFAULT 'DRAFT') (DRAFT, PUBLISHED, ARCHIVED)
- `published_at` (TIMESTAMP)
- `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)

### `categories`
- `id` (BIGINT, PK, Generated Identity)
- `name` (VARCHAR(100), UNIQUE, NOT NULL)
- `description` (TEXT)

### `post_categories` (Join Table)
- `post_id` (BIGINT, FK -> posts.id, PK)
- `category_id` (BIGINT, FK -> categories.id, PK)

### `tags`
- `id` (BIGINT, PK, Generated Identity)
- `name` (VARCHAR(50), UNIQUE, NOT NULL)

### `post_tags` (Join Table)
- `post_id` (BIGINT, FK -> posts.id, PK)
- `tag_id` (BIGINT, FK -> tags.id, PK)

### `comments`
- `id` (BIGINT, PK, Generated Identity)
- `post_id` (BIGINT, FK -> posts.id, NOT NULL)
- `user_id` (BIGINT, FK -> users.id, NOT NULL)
- `parent_id` (BIGINT, FK -> comments.id, NULLABLE)
- `content` (TEXT, NOT NULL)
- `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)

### `post_likes`
- `id` (BIGINT, PK, Generated Identity)
- `post_id` (BIGINT, FK -> posts.id, NOT NULL)
- `user_id` (BIGINT, FK -> users.id, NOT NULL)
- *Unique Constraint:* `(post_id, user_id)`

---

## 4. API Endpoints

### Authentication
- `POST /api/v1/auth/register` (Public)
- `POST /api/v1/auth/login` (Public)
- `GET /api/v1/auth/me` (Authenticated)

### User Management
- `GET /api/v1/users` (Admin)
- `GET /api/v1/users/{id}` (Public)
- `PUT /api/v1/users/{id}` (Owner/Admin)
- `DELETE /api/v1/users/{id}` (Owner/Admin)

### Posts
- `GET /api/v1/posts` (Public - Paginated, Filterable by Search, Category, Tag, Status)
- `GET /api/v1/posts/{id}` (Public - Retrieves post details, comment thread, like count)
- `POST /api/v1/posts` (Authenticated)
- `PUT /api/v1/posts/{id}` (Owner)
- `DELETE /api/v1/posts/{id}` (Owner/Admin)
- `GET /api/v1/posts/my` (Authenticated - Retrieves current user's posts)

### Comments
- `GET /api/v1/posts/{postId}/comments` (Public - Paginated, Threaded)
- `POST /api/v1/posts/{postId}/comments` (Authenticated)
- `PUT /api/v1/comments/{id}` (Owner)
- `DELETE /api/v1/comments/{id}` (Owner/Admin)

### Likes
- `POST /api/v1/posts/{postId}/like` (Authenticated - Toggles Like)
- `GET /api/v1/posts/{postId}/likes` (Public - Retrieves like list & count)

### Categories & Tags (Admin Managed)
- `GET /api/v1/categories` (Public)
- `POST /api/v1/categories` (Admin)
- `PUT /api/v1/categories/{id}` (Admin)
- `DELETE /api/v1/categories/{id}` (Admin)
- `GET /api/v1/tags` (Public)
- `POST /api/v1/tags` (Admin)
- `DELETE /api/v1/tags/{id}` (Admin)

---

## 5. Security Architecture
- Custom `JwtAuthenticationFilter` reads `Authorization` header bearer token.
- `SecurityFilterChain` restricts HTTP methods and paths based on Authentication and Roles (`ROLE_USER`, `ROLE_ADMIN`).
- Password validation and encoding via `BCryptPasswordEncoder`.

---

## 6. Implementation Strategy (Approach 1)
- Write domain entities with custom builder patterns (without Lombok).
- Define service layer mapping DTO records $\leftrightarrow$ Domain Entities.
- Write custom JPA Specifications for dynamic filtering (`PostSpecification`).
- Expose endpoints via cleanly written Spring RestControllers.
- Include a complete postman-like Curl test script to verify endpoint correctness.
