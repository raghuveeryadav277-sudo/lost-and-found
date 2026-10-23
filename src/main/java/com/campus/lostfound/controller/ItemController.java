package com.campus.lostfound.controller;

import com.campus.lostfound.controller.Dtos.ItemView;
import com.campus.lostfound.controller.Dtos.MatchView;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.service.CurrentUserService;
import com.campus.lostfound.service.ImageHashService;
import com.campus.lostfound.service.MatchService;
import com.campus.lostfound.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Report + Search modules: report an item (with photo), browse and search
 * all reports, see match suggestions, and close your own case.
 */
@RestController
@RequestMapping("/api")
public class ItemController {

    private final ItemRepository items;
    private final CurrentUserService currentUser;
    private final MatchService matchService;
    private final ImageHashService imageHashService;
    private final NotificationService notificationService;
    private final Path uploadDir;

    public ItemController(ItemRepository items, CurrentUserService currentUser,
                          MatchService matchService, ImageHashService imageHashService,
                          NotificationService notificationService,
                          @Value("${app.upload.dir}") String uploadDir) {
        this.items = items;
        this.currentUser = currentUser;
        this.matchService = matchService;
        this.imageHashService = imageHashService;
        this.notificationService = notificationService;
        this.uploadDir = Path.of(uploadDir);
    }

    // ------------------------------------------------------------ search
    @GetMapping("/items")
    public List<ItemView> search(@RequestParam(required = false) String type,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String location,
                                 @RequestParam(required = false) String keyword) {
        return items.search(parseType(type), parseStatus(status),
                        blank(category), blank(location), blank(keyword))
                .stream().map(ItemView::of).toList();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        var found = items.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Item not found"));
        }
        return ResponseEntity.ok(ItemView.of(found.get()));
    }

    /** Match suggestions for one item (opposite type, scored 0-100). */
    @GetMapping("/items/{id}/matches")
    public ResponseEntity<?> matches(@PathVariable Long id) {
        var found = items.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Item not found"));
        }
        List<MatchView> result = matchService.findMatches(found.get(), 6).stream()
                .map(m -> new MatchView(ItemView.of(m.item()), m.matchScore(), m.imageMatchScore()))
                .toList();
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------ report
    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> report(@RequestParam String title,
                                    @RequestParam String type,
                                    @RequestParam String category,
                                    @RequestParam String description,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(required = false) MultipartFile image) throws IOException {
        User reporter = currentUser.currentUser();

        Item item = new Item();
        item.setTitle(title.trim());
        item.setType(Item.Type.valueOf(type.toUpperCase()));
        item.setCategory(category.trim());
        item.setDescription(description.trim());
        item.setLocation(location == null ? "" : location.trim());
        item.setReportedBy(reporter);

        if (image != null && !image.isEmpty()) {
            Files.createDirectories(uploadDir);
            String ext = image.getOriginalFilename() != null
                    && image.getOriginalFilename().contains(".")
                    ? image.getOriginalFilename().substring(image.getOriginalFilename().lastIndexOf('.'))
                    : ".png";
            String fileName = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(fileName);
            image.transferTo(target);
            item.setImage("/uploads/" + fileName);
            item.setImageHash(imageHashService.averageHash(target.toFile()));
        }

        Item saved = items.save(item);
        notifyAboutMatches(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemView.of(saved));
    }

    /** When a strong match exists, alert both reporters right away. */
    private void notifyAboutMatches(Item item) {
        List<MatchService.ScoredMatch> matches = matchService.findMatches(item, 3);
        for (MatchService.ScoredMatch m : matches) {
            if (m.matchScore() < 45) continue;
            Item other = m.item();
            notificationService.notify(other.getReportedBy(),
                    "Possible match (" + m.matchScore() + "%) for your " + other.getType()
                            + " item '" + other.getTitle() + "': a new " + item.getType()
                            + " report '" + item.getTitle() + "' was just added.");
            notificationService.notify(item.getReportedBy(),
                    "Possible match (" + m.matchScore() + "%) found for your " + item.getType()
                            + " item '" + item.getTitle() + "': see '" + other.getTitle()
                            + "' in Browse.");
        }
    }

    // ------------------------------------------------------------ mine
    @GetMapping("/my/items")
    public List<ItemView> myItems() {
        return items.findByReportedByOrderByCreatedAtDesc(currentUser.currentUser())
                .stream().map(ItemView::of).toList();
    }

    /** The reporter (or an admin) can close a case without a return. */
    @PostMapping("/items/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id) {
        User me = currentUser.currentUser();
        var found = items.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Item not found"));
        }
        Item item = found.get();
        if (!item.getReportedBy().getUserId().equals(me.getUserId())
                && me.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the reporter or an admin can close this case"));
        }
        item.setStatus(Item.Status.CLOSED);
        items.save(item);
        return ResponseEntity.ok(ItemView.of(item));
    }

    // ------------------------------------------------------------ stats (home page)
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "total", items.count(),
                "lost", items.countByType(Item.Type.LOST),
                "found", items.countByType(Item.Type.FOUND),
                "open", items.countByStatus(Item.Status.OPEN),
                "returned", items.countByStatus(Item.Status.RETURNED));
    }

    private Item.Type parseType(String s) {
        try { return s == null ? null : Item.Type.valueOf(s.toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private Item.Status parseStatus(String s) {
        try { return s == null ? null : Item.Status.valueOf(s.toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
