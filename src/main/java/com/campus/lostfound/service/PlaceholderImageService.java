package com.campus.lostfound.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Creates simple placeholder item photos for the seed data, so the demo
 * works instantly and the image-matching feature has pictures to compare.
 * A generated photo is a coloured card with the item's name drawn on it.
 */
@Service
public class PlaceholderImageService {

    private final Path uploadDir;

    public PlaceholderImageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
    }

    /** Draws a placeholder picture and returns its public path (/uploads/...). */
    public String createPlaceholder(String label, Color background) {
        try {
            Files.createDirectories(uploadDir);
            String fileName = "seed-" + UUID.randomUUID() + ".png";
            File file = uploadDir.resolve(fileName).toFile();

            BufferedImage img = new BufferedImage(480, 360, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(background);
            g.fillRect(0, 0, 480, 360);
            g.setColor(background.darker());
            g.fillRect(0, 260, 480, 100);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            FontMetrics fm = g.getFontMetrics();
            int x = (480 - fm.stringWidth(label)) / 2;
            g.drawString(label, x, 190);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            String sub = "Lost & Found - demo photo";
            g.drawString(sub, (480 - g.getFontMetrics().stringWidth(sub)) / 2, 315);
            g.dispose();

            ImageIO.write(img, "png", file);
            return "/uploads/" + fileName;
        } catch (Exception e) {
            return null;
        }
    }
}
