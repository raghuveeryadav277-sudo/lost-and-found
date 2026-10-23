package com.campus.lostfound.controller;

import com.campus.lostfound.controller.Dtos.ClaimView;
import com.campus.lostfound.controller.Dtos.ItemView;
import com.campus.lostfound.controller.Dtos.UserView;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.repository.ClaimRepository;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin module: one place to see every case (items + claims), moderate a
 * report by closing it, and audit what happened. ADMIN role only - the
 * security config restricts /api/admin/** to admins.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ItemRepository items;
    private final ClaimRepository claims;
    private final UserRepository users;

    public AdminController(ItemRepository items, ClaimRepository claims, UserRepository users) {
        this.items = items;
        this.claims = claims;
        this.users = users;
    }

    @GetMapping("/items")
    public List<ItemView> allItems() {
        return items.findAllByOrderByCreatedAtDesc().stream().map(ItemView::of).toList();
    }

    @GetMapping("/claims")
    public List<ClaimView> allClaims() {
        return claims.findAllByOrderByTimestampDesc().stream().map(ClaimView::of).toList();
    }

    @GetMapping("/users")
    public List<UserView> allUsers() {
        return users.findAll().stream().map(UserView::of).toList();
    }

    @PostMapping("/items/{id}/close")
    public ResponseEntity<?> closeItem(@PathVariable Long id) {
        var found = items.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Item not found"));
        }
        Item item = found.get();
        item.setStatus(Item.Status.CLOSED);
        items.save(item);
        return ResponseEntity.ok(ItemView.of(item));
    }
}
