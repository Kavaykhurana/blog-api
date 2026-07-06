# Blog API

A secure, high-performance, production-grade RESTful API for a multi-user blogging platform built using **Java 25**, **Spring Boot 3.2.4**, **JPA / Hibernate**, and **PostgreSQL**. 

The application is deployed live on Railway at: **[https://blog-api-production-4f3a.up.railway.app](https://blog-api-production-4f3a.up.railway.app)**

---

## 🎨 Interactive Premium Front-End Portal

The application now serves a high-fidelity, single-page application (SPA) front-end directly from the root `/` URL. Built using **modern semantic HTML5**, **custom responsive Vanilla CSS**, and **pure native JavaScript**, it features:

* **Sleek Glassmorphic Design**: A modern dark-mode-first aesthetic with glowing borders, translucent backdrops (`backdrop-filter: blur`), responsive layout grids, and clean visual structures.
* **Hash-based Client-Side Router**: Enables natural back/forward navigation history (`#/`, `#/post/123`, `#/profile`, `#/admin`) without reloading the page.
* **Stateless Auth Integration**: Login/Registration UI forms that persist JWT bearer tokens inside local storage and dynamically update navigation menus.
* **Interactive Post Feed**: View posts with dynamic text-match keyword searching and tag/category filtering chips. Supports client-side pagination.
* **Threaded Comment Tree**: Recursively renders nested, hierarchical comment threads. Authenticated users can post replies, edit, or delete comments inline.
* **Pulsing Like Button**: Custom micro-interactions with pulsing SVG heart animations that show live like states and counters.
* **Dashboard Profile**: User portal showing written posts (both drafts and published) with edit/delete shortcuts and user stats.
* **Admin Control Center**: Built-in tables for administrators (`admin` / `admin123`) to perform CRUD operations on categories and tags directly.

---

## 🚀 Key Features

* **Stateless Security**: Secure endpoints using Spring Security 6.x and JWT (JSON Web Tokens via JJWT 0.12.5) with BCrypt password hashing and Role-Based Access Control (RBAC).
* **Dynamic Search & Filtering**: Implements Spring Data JPA Specifications with Criteria Builder to search posts by keyword and filter by category/tags dynamically.
* **Hierarchical Comments**: Supports nested comment replies (parent-child self-referential structure) with automatic cascade deletes.
* **Likes System**: Toggles post likes and tracks liked counts with database unique constraint collision protection.
* **Idempotent Data Seeding**: Automatically bootstraps default categories, tags, and a default admin user on application startup safely without duplicate records.
* **Robust Exception Handling**: Global error controller advice that catches data integrity violations, binding failures, and auth errors to return clean API messages while masking internal database structures.
* **Performance Tuning**: Addressed Hibernate lazy-loading bottlenecks to prevent N+1 query patterns using EntityGraphs.

---

## 🛠️ Technology Stack

* **Language**: Java 25 (OpenJDK 25.0.1)
* **Framework**: Spring Boot 3.2.4
* **ORM & Database**: JPA / Hibernate, PostgreSQL (Driver 42.6.2)
* **Security**: Spring Security 6.2.3, JSON Web Tokens (io.jsonwebtoken:jjwt-api:0.12.5)
* **Build System**: Apache Maven 3.9.16
* **Hosting / Cloud**: Railway

---

## 📈 Database Performance Optimizations

### 1. Resolving the N+1 Query Problem
Fetching comments and their authors naively leads to $N+1$ database queries. To prevent this, an eager fetch graph is configured on the `CommentRepository` to load comment authors in a single query:
```java
@EntityGraph(attributePaths = {"author"})
Page<Comment> findByPostAndParentIsNull(Post post, Pageable pageable);
```

### 2. Eliminating Pagination Cartesian Product Duplication
Performing joins on tag and category collections causes cartesian product expansion in Hibernate. In `PostSpecification.java`, this is optimized by explicitly calling `distinct(true)` on the criteria query builder:
```java
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
```

---

## 📖 API Endpoints

### 🔐 Authentication
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a new user | Public |
| `POST` | `/api/v1/auth/login` | Login and acquire JWT bearer token | Public |
| `GET` | `/api/v1/auth/me` | Fetch active user profile | Authenticated |

### 👤 User Management
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | List all users | Admin Only |
| `GET` | `/api/v1/users/{id}` | Fetch user profile by ID | Public |
| `PUT` | `/api/v1/users/{id}` | Update profile details | Owner / Admin |
| `DELETE` | `/api/v1/users/{id}` | Delete user account | Owner / Admin |

### 📝 Posts
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/posts` | Create a new blog post | Authenticated |
| `GET` | `/api/v1/posts` | Get all published posts (with search/filter query params) | Public |
| `GET` | `/api/v1/posts/my` | Get current authenticated user's posts | Authenticated |
| `GET` | `/api/v1/posts/{id}` | Get post details by ID | Public |
| `PUT` | `/api/v1/posts/{id}` | Update post content | Owner / Admin |
| `DELETE` | `/api/v1/posts/{id}` | Delete post | Owner / Admin |

### 💬 Comments
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/posts/{postId}/comments` | Post a comment or nested reply | Authenticated |
| `GET` | `/api/v1/posts/{postId}/comments` | Get paginated comments for a post | Public |
| `PUT` | `/api/v1/comments/{id}` | Edit comment | Owner / Admin |
| `DELETE` | `/api/v1/comments/{id}` | Delete comment | Owner / Admin |

### ❤️ Likes
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/posts/{postId}/like` | Toggle like on a post | Authenticated |
| `GET` | `/api/v1/posts/{postId}/likes` | Get list of users who liked the post | Public |

### 📁 Categories & Tags
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/categories` | Get all categories | Public |
| `POST` | `/api/v1/categories` | Create a category | Admin Only |
| `PUT` | `/api/v1/categories/{id}` | Update a category | Admin Only |
| `DELETE` | `/api/v1/categories/{id}` | Delete a category | Admin Only |
| `GET` | `/api/v1/tags` | Get all tags | Public |
| `POST` | `/api/v1/tags` | Create a tag | Admin Only |
| `DELETE` | `/api/v1/tags/{id}` | Delete a tag | Admin Only |

---

## 🛠️ Local Development Setup

### Prerequisites
* Java 21 or Java 25 installed
* Apache Maven 3.8+
* PostgreSQL running locally on port `5432` with a database named `blog_db`

### 1. Database Configuration
Ensure a local database named `blog_db` exists. You can customize connections using environment variables or application configuration profiles. Default local connection details inside `src/main/resources/application.yml`:
* Host: `localhost:5432`
* Username: `kavaykhurana` (your local system user)
* Password: *blank*

### 2. Build the Application
Compile the code and packages:
```bash
mvn clean package -DskipTests
```

### 3. Run Locally
Run using Maven Spring Boot plugin:
```bash
mvn spring-boot:run
```

---

## 🧪 Integration Verification Script

A custom shell script `verify.sh` is provided at the root of the project to test the entire API lifecycle. 

### Features of the Verification Script
* **Robust Status Code Checking**: Inspects the exact HTTP status code returned for each endpoint to fail-fast on errors.
* **Auto-parsing token**: Uses `jq` or `python3` to automatically parse and extract JWT tokens (`USER_TOKEN`, `ADMIN_TOKEN`).
* **Automated Database Cleanup**: Uses a trap handler (`trap cleanup EXIT`) to automatically delete all created test data (test user, category, post, parent comments, nested reply comments) in reverse relational order upon termination.

### Run Verification:
To run it against your local application server:
```bash
./verify.sh http://localhost:8080
```

To run it against the live production deployment:
```bash
./verify.sh https://blog-api-production-4f3a.up.railway.app
```