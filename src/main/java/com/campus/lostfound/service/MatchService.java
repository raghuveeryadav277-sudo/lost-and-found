package com.campus.lostfound.service;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Match suggestions - the "Search and Match" module.
 *
 * For a given item we look at items of the OPPOSITE type (a LOST report is
 * matched with FOUND items and vice versa) that are still open, and score
 * them:
 *   - same category ............ 25 points
 *   - title/description words .. up to 35 points (Jaccard overlap)
 *   - image similarity ......... up to 30 points (perceptual hash)
 *   - same location ............ 10 points
 * The total is a 0-100 "match score" shown in the UI.
 */
@Service
public class MatchService {

    /** A candidate item together with its scores. */
    public record ScoredMatch(Item item, int matchScore, Integer imageMatchScore) {}

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "in", "on", "at", "to", "my", "i",
            "lost", "found", "with", "near", "please", "help", "it", "is", "was");

    private final ItemRepository items;
    private final ImageHashService imageHash;

    public MatchService(ItemRepository items, ImageHashService imageHash) {
        this.items = items;
        this.imageHash = imageHash;
    }

    public List<ScoredMatch> findMatches(Item item, int limit) {
        Item.Type opposite = item.getType() == Item.Type.LOST ? Item.Type.FOUND : Item.Type.LOST;
        List<Item> candidates = items.findByTypeAndStatus(opposite, Item.Status.OPEN);

        List<ScoredMatch> scored = new ArrayList<>();
        for (Item candidate : candidates) {
            if (candidate.getItemId().equals(item.getItemId())) continue;
            ScoredMatch s = score(item, candidate);
            if (s.matchScore() >= 25) scored.add(s);
        }
        scored.sort(Comparator.comparingInt(ScoredMatch::matchScore).reversed());
        return scored.stream().limit(limit).collect(Collectors.toList());
    }

    public ScoredMatch score(Item a, Item b) {
        int score = 0;

        if (a.getCategory() != null && a.getCategory().equalsIgnoreCase(b.getCategory())) {
            score += 25;
        }

        Set<String> wordsA = words(a.getTitle() + " " + a.getDescription());
        Set<String> wordsB = words(b.getTitle() + " " + b.getDescription());
        if (!wordsA.isEmpty() && !wordsB.isEmpty()) {
            Set<String> both = new HashSet<>(wordsA);
            both.retainAll(wordsB);
            Set<String> either = new HashSet<>(wordsA);
            either.addAll(wordsB);
            double jaccard = (double) both.size() / either.size();
            score += (int) Math.round(jaccard * 35);
        }

        Integer imageScore = null;
        if (a.getImageHash() != null && b.getImageHash() != null) {
            imageScore = imageHash.similarityPercent(a.getImageHash(), b.getImageHash());
            score += (int) Math.round(imageScore / 100.0 * 30);
        }

        if (a.getLocation() != null && a.getLocation().equalsIgnoreCase(b.getLocation())) {
            score += 10;
        }

        return new ScoredMatch(b, Math.min(score, 100), imageScore);
    }

    private Set<String> words(String text) {
        if (text == null) return Set.of();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() > 2 && !STOPWORDS.contains(w))
                .collect(Collectors.toSet());
    }
}
