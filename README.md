# 🌐 NexusBlog: Premium Multi-User Blogging Platform

[![Java Version](https://img.shields.95.svg?label=Java&message=21&color=orange&style=flat-square)](https://openjdk.org/)
[![Spring Boot](https://img.shields.95.svg?label=Spring+Boot&message=3.2.4&color=green&style=flat-square)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.95.svg?label=Database&message=PostgreSQL&color=blue&style=flat-square)](https://www.postgresql.org/)
[![Security](https://img.shields.95.svg?label=Security&message=JWT%20Stateless&color=red&style=flat-square)](https://spring.io/projects/spring-security)
[![Hosting](https://img.shields.95.svg?label=Hosting&message=Railway&color=black&style=flat-square)](https://railway.app/)

A secure, high-performance, production-grade RESTful API and interactive Single Page Application (SPA) for a multi-user blogging community. Built with **Java 21**, **Spring Boot 3.2.4**, **JPA / Hibernate**, **PostgreSQL**, and a styled **Glassmorphic Vanilla CSS/JS** front-end.

🚀 **Live Portal**: [https://blog-api-production-4f3a.up.railway.app/](https://blog-api-production-4f3a.up.railway.app/)

---

## 🎨 Interactive Glassmorphic Front-End Portal

The application serves a high-fidelity Single Page Application (SPA) directly from the root `/` URL, bundled natively inside the Spring Boot JAR resources.

### Highlights & Features:
* **Glassmorphic Dark Aesthetic**: A responsive dark-mode layout built with custom CSS variables, custom scrollbars, glowing button effects, and heart-pulsing transitions for likes.
* **Hash-based Client-Side Router**: Enables back/forward history navigation (`#/`, `#/post/:id`, `#/profile`, `#/admin`, `#/auth`) without triggering page refreshes.
* **Live Markdown Editor & Preview**: A split-screen pane that parses headers, bold, italics, code blocks, and lists instantly in JavaScript as you type.
* **Recursive Comment Trees**: Displays hierarchical, threaded comments with indentation. Authenticated users can write replies, edit, or delete comments inline.
* **Skeleton Screen Loading**: Smooth layout skeletons animate while data fetches to prevent layout shifts and enhance Largest Contentful Paint (LCP) scores.
* **Profile Dashboard**: Allows users to manage drafts vs published posts, see personal writing stats, and edit their bio/displayName through inline modal dialogues.
* **Admin Control Center**: Built-in tables for platform administrators (`admin` / `admin123`) to create, update, or delete categories and tags on the fly.

---

## 📂 Project Repository Directory Structure

```text
blog-api/
├── target/                      # Maven build outputs
├── docs/                        # Architectural documentation
├── verify.sh                    # End-to-end API lifecycle verification script
├── pom.xml                      # Dependency specifications & Java compiler rules
└── src/
    └── main/
        ├── java/com/blog/
        │   ├── BlogApplication.java   # App bootstrap entrypoint
        │   ├── config/                # Security Config, JWT filter, Data Seed
        │   ├── controller/            # REST API mappings
        │   ├── dto/                   # Data Transfer Objects (Requests & Responses)
        │   ├── exception/             # Global error handlers & Resource classes
        │   ├── model/                 # JPA database entities (User, Post, Like, Comment)
        │   ├── repository/            # DB specifications and JPA queries
        │   └── service/               # Transactional business logic operations
        └── resources/
            ├── application.yml        # Configuration profiles (ports, Postgres URL)
            └── static/                # Native front-end files
                ├── index.html         # HTML structural grid layouts
                ├── style.css          # Design variables, scrollbars & glassmorphism
                └── app.js             # API client, state, router, and MD parser
```

---

## 📈 Database Performance Optimizations

### 1. Resolving the N+1 Query Bottleneck
Fetching comments and their authors naively leads to $N+1$ database queries. To prevent this, an eager fetch graph is configured on the `CommentRepository` to load comment authors in a single query:
```java
@EntityGraph(attributePaths = {"author"})
Page<Comment> findByPostAndParentIsNull(Post post, Pageable pageable);
```

### 2. Eliminating Cartesian Product Duplication
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
| `POST` | `/api/v1/auth/register` | Register a new user account | Public |
| `POST` | `/api/v1/auth/login` | Login and acquire JWT bearer token | Public |
| `GET` | `/api/v1/auth/me` | Fetch active user profile details | Authenticated |

### 👤 User Management
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | List all users | Admin Only |
| `GET` | `/api/v1/users/{id}` | Fetch user profile by ID | Public |
| `PUT` | `/api/v1/users/{id}` | Update profile details (bio/displayName) | Owner / Admin |
| `DELETE` | `/api/v1/users/{id}` | Delete user account | Owner / Admin |

### 📝 Posts
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/posts` | Create a new blog post (Draft/Published) | Authenticated |
| `GET` | `/api/v1/posts` | Get all published posts (supports keyword/tag/category search) | Public |
| `GET` | `/api/v1/posts/my` | Get current authenticated user's posts | Authenticated |
| `GET` | `/api/v1/posts/{id}` | Get post details by ID | Public |
| `PUT` | `/api/v1/posts/{id}` | Update post content | Owner / Admin |
| `DELETE` | `/api/v1/posts/{id}` | Delete post | Owner / Admin |

### 💬 Comments
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/posts/{postId}/comments` | Post a comment or nested reply | Authenticated |
| `GET` | `/api/v1/posts/{postId}/comments` | Get paginated comments for a post | Public |
| `PUT` | `/api/v1/comments/{id}` | Edit comment content | Owner / Admin |
| `DELETE` | `/api/v1/comments/{id}` | Delete comment | Owner / Admin |

### ❤️ Likes
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/posts/{postId}/like` | Toggle like status on a post | Authenticated |
| `GET` | `/api/v1/posts/{postId}/likes` | Get list of users who liked the post | Public |

---

## 🛠️ Local Development & Running

### Prerequisites
* **Java 21** installed
* **Maven 3.8+**
* **PostgreSQL** running locally on port `5432` with a database named `blog_db`

### 1. Database Configuration
Ensure a local database named `blog_db` exists. Default local connection details inside `src/main/resources/application.yml`:
* Host: `localhost:5432`
* Username: `kavaykhurana` (or customize via environments)
* Password: *blank* (or custom password)

### 2. Build the Application
Compile the code and bundle resources:
```bash
mvn clean package -DskipTests
```

### 3. Run Locally
Start the Spring Boot server:
```bash
mvn spring-boot:run
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser to view the frontend application portal.

* **Seeded Accounts**: On startup, an admin account is automatically seeded:
  * Username: `admin`
  * Password: `admin123`

---

## 🧪 Integration Verification Testing

A custom shell script `verify.sh` is provided at the root of the project to test the entire API lifecycle. It validates status codes, parses tokens automatically via `jq`/`python3`, and cleans up all created test data (in reverse dependency order) upon exit.

### Run Verification locally:
```bash
chmod +x verify.sh
./verify.sh http://localhost:8080
```

### Run Verification against production:
```bash
./verify.sh https://blog-api-production-4f3a.up.railway.app
```

---

## ☁️ Deploying to Railway

The project is pre-configured for direct cloud deployment to **Railway** using their Nixpacks builder.

### Option A: Via Railway CLI (Recommended)
1. Install Railway CLI: `npm install -g @railway/cli`
2. Login to your account: `railway login`
3. Link your project directory: `railway link`
4. Upload and deploy:
   ```bash
   railway up
   ```

### Option B: Automatic GitHub Deploys
1. Push the code to your GitHub repository.
2. Link your GitHub repository to your Service block on the Railway Dashboard.
3. Railway will automatically compile the `pom.xml`, copy static resources, package the JAR, and spin up the server on every commit!