package com.campus.lostfound.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/**
 * Perceptual image hashing (average hash / aHash).
 *
 * The PPT lists "AI Image Matching" as an innovation highlight. For this
 * prototype we implement the honest, classic version of it: every uploaded
 * photo is reduced to a 64-bit fingerprint. Two photos of the same object
 * produce fingerprints that differ in only a few bits, so the Hamming
 * distance between hashes gives a simple "image match score" without any
 * external AI service.
 */
@Service
public class ImageHashService {

    /** Computes the 64-bit average hash of an image, or null if unreadable. */
    public Long averageHash(InputStream in) {
        try {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            return averageHash(img);
        } catch (Exception e) {
            return null;
        }
    }

    public Long averageHash(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return null;
            return averageHash(img);
        } catch (Exception e) {
            return null;
        }
    }

    public Long averageHash(BufferedImage src) {
        // 1) shrink to 8x8 and convert to grayscale
        BufferedImage small = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, 8, 8, null);
        g.dispose();

        // 2) average brightness of the 64 pixels
        int[] px = new int[64];
        long sum = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int v = small.getRaster().getSample(x, y, 0);
                px[y * 8 + x] = v;
                sum += v;
            }
        }
        double avg = sum / 64.0;

        // 3) one bit per pixel: 1 if brighter than average
        long hash = 0;
        for (int i = 0; i < 64; i++) {
            if (px[i] >= avg) hash |= (1L << i);
        }
        return hash;
    }

    /** Similarity between two hashes as a percentage (100 = identical). */
    public int similarityPercent(long a, long b) {
        int distance = Long.bitCount(a ^ b); // Hamming distance, 0..64
        return (int) Math.round((64 - distance) / 64.0 * 100);
    }
}
