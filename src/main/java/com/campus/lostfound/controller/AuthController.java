package com.campus.lostfound.controller;

import com.campus.lostfound.controller.Dtos.RegisterRequest;
import com.campus.lostfound.controller.Dtos.UserView;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Registration and "who am I" - the Registration and Login module. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUserService currentUser;

    public AuthController(UserRepository users, PasswordEncoder encoder, CurrentUserService currentUser) {
        this.users = users;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with this email already exists"));
        }
        User user = new User(req.name(), req.email(),
                encoder.encode(req.password()), User.Role.USER);
        users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserView.of(user));
    }

    @GetMapping("/me")
    public UserView me() {
        return UserView.of(currentUser.currentUser());
    }
}
