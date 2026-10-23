package com.campus.lostfound.config;

import com.campus.lostfound.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security measures from the report (Chapter 5.3):
 *  - passwords stored as BCrypt hashes,
 *  - login required for reporting / claiming / notifications,
 *  - role-based access: /api/admin/** is ADMIN only.
 *
 * The portal pages themselves are static HTML; the REST APIs enforce the
 * rules. (CSRF is disabled because this is a same-origin prototype whose
 * forms are submitted with fetch(); a production version should enable it.)
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    // public pages and assets
                    .requestMatchers("/", "/index.html", "/login.html", "/register.html",
                            "/browse.html", "/item.html", "/map.html",
                            "/css/**", "/js/**", "/favicon.ico", "/uploads/**").permitAll()
                    // public API endpoints
                    .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/*",
                            "/api/items/*/matches", "/api/stats").permitAll()
                    // admin only
                    .requestMatchers("/api/admin/**", "/admin.html").hasRole("ADMIN")
                    // everything else needs a logged-in user
                    .anyRequest().authenticated())
            .formLogin(form -> form
                    .loginPage("/login.html")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .defaultSuccessUrl("/index.html", true)
                    .failureUrl("/login.html?error=1")
                    .permitAll())
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login.html?logout=1")
                    .permitAll())
            // APIs answer 401 JSON-style instead of redirecting to the login page
            .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                if (req.getRequestURI().startsWith("/api/")) {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Please login first\"}");
                } else {
                    res.sendRedirect("/login.html");
                }
            }));
        return http.build();
    }
}
