package com.blog.config;

import com.blog.model.User;
import com.blog.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return usernameOrEmail -> {
            User user = userRepository.findByUsername(usernameOrEmail)
                    .or(() -> userRepository.findByEmail(usernameOrEmail))
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with username or email: " + usernameOrEmail));

            String dbRole = user.getRole();
            if (dbRole == null || dbRole.trim().isEmpty()) {
                dbRole = "USER";
            }
            String authorityRole = dbRole.startsWith("ROLE_") ? dbRole : "ROLE_" + dbRole;

            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(new SimpleGrantedAuthority(authorityRole))
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"");
                    response.getWriter().write(authException.getMessage() != null ? authException.getMessage() : "Access Denied");
                    response.getWriter().write("\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Allow all CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public root welcome and static resources
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/assets/**", "/static/**", "/*.html", "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.jpeg", "/*.svg", "/*.gif", "/*.ico", "/api/v1/status").permitAll()
                
                // Authentication Endpoints
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/me").authenticated()

                // Users Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*").authenticated()

                // Posts Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/likes").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/posts").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/like").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/posts/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*").authenticated()

                // Comments Endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/comments/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/comments/*").authenticated()

                // Categories & Tags
                .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/tags").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/categories", "/api/v1/tags").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*", "/api/v1/tags/*").hasRole("ADMIN")

                // Catch-all
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
