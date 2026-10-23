package com.campus.lostfound.service;

import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Returns the currently logged-in user (from the Spring Security session). */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found: " + email));
    }

    public boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }
}
