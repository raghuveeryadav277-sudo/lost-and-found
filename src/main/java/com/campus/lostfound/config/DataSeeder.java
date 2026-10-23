package com.campus.lostfound.config;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.service.ImageHashService;
import com.campus.lostfound.service.PlaceholderImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.nio.file.Path;

/**
 * First-run seed data so a demo works instantly:
 *  - admin@campus.edu / admin123 (ADMIN)
 *  - aarav@campus.edu / student123, meera@campus.edu / student123
 *  - 6 sample reports. Aarav's LOST black wallet and Meera's FOUND black
 *    wallet deliberately match, so the full claim flow can be demoed.
 * Runs only when the users table is empty.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final ItemRepository items;
    private final PasswordEncoder encoder;
    private final PlaceholderImageService placeholders;
    private final ImageHashService imageHash;
    private final Path uploadDir;

    public DataSeeder(UserRepository users, ItemRepository items, PasswordEncoder encoder,
                      PlaceholderImageService placeholders, ImageHashService imageHash,
                      @Value("${app.upload.dir}") String uploadDir) {
        this.users = users;
        this.items = items;
        this.encoder = encoder;
        this.placeholders = placeholders;
        this.imageHash = imageHash;
        this.uploadDir = Path.of(uploadDir);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;

        User admin = users.save(new User("Campus Admin", "admin@campus.edu",
                encoder.encode("admin123"), User.Role.ADMIN));
        User aarav = users.save(new User("Aarav Sharma", "aarav@campus.edu",
                encoder.encode("student123"), User.Role.USER));
        User meera = users.save(new User("Meera Iyer", "meera@campus.edu",
                encoder.encode("student123"), User.Role.USER));

        seedItem("Black Wallet", Item.Type.FOUND, "Wallet",
                "Black leather wallet with a college ID card and some cash inside. Found near the library entrance.",
                "Library", meera, new Color(45, 45, 48));
        seedItem("Black Leather Wallet", Item.Type.LOST, "Wallet",
                "Lost my black leather wallet near the library. It has my ID card and a bus pass inside.",
                "Library", aarav, new Color(58, 54, 50));
        seedItem("Blue Water Bottle", Item.Type.FOUND, "Bottle",
                "Steel blue water bottle left on a canteen table after lunch.",
                "Canteen", meera, new Color(52, 98, 160));
        seedItem("College ID Card", Item.Type.LOST, "ID Card",
                "Lost my college ID card near the main gate in the morning.",
                "Main Gate", aarav, new Color(170, 60, 55));
        seedItem("Data Structures Book", Item.Type.FOUND, "Book",
                "A data structures book found in Lab Block room 204, name written on the first page.",
                "Lab Block", meera, new Color(66, 133, 84));
        seedItem("Black Umbrella", Item.Type.LOST, "Umbrella",
                "Black umbrella left near the hostel mess entrance yesterday evening.",
                "Hostel", aarav, new Color(70, 70, 75));
    }

    private void seedItem(String title, Item.Type type, String category, String description,
                          String location, User reporter, Color color) {
        Item item = new Item();
        item.setTitle(title);
        item.setType(type);
        item.setCategory(category);
        item.setDescription(description);
        item.setLocation(location);
        item.setReportedBy(reporter);
        String imagePath = placeholders.createPlaceholder(title, color);
        item.setImage(imagePath);
        if (imagePath != null) {
            String fileName = imagePath.substring("/uploads/".length());
            item.setImageHash(imageHash.averageHash(uploadDir.resolve(fileName).toFile()));
        }
        items.save(item);
    }
}
