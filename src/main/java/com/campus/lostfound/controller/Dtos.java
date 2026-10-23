package com.campus.lostfound.controller;

import com.campus.lostfound.model.Claim;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.Notification;
import com.campus.lostfound.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response shapes used by the REST APIs (kept in one file for the team). */
public class Dtos {

    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Size(min = 6, message = "Password must be at least 6 characters") String password) {}

    public record UserView(Long userId, String name, String email, String role) {
        static UserView of(User u) {
            return new UserView(u.getUserId(), u.getName(), u.getEmail(), u.getRole().name());
        }
    }

    public record ItemView(Long itemId, String title, String type, String category,
                           String description, String image, String location, String status,
                           Long reportedById, String reportedByName, String createdAt) {
        static ItemView of(Item i) {
            return new ItemView(i.getItemId(), i.getTitle(), i.getType().name(), i.getCategory(),
                    i.getDescription(), i.getImage(), i.getLocation(), i.getStatus().name(),
                    i.getReportedBy().getUserId(), i.getReportedBy().getName(),
                    i.getCreatedAt().toString());
        }
    }

    /** An item plus its match score (and image-only score, when both have photos). */
    public record MatchView(ItemView item, int matchScore, Integer imageMatchScore) {}

    public record ClaimRequest(Long itemId, String proofNote) {}

    public record ClaimView(Long claimId, Long itemId, String itemTitle, String itemImage,
                            Long claimantId, String claimantName, String proofNote,
                            String verificationStatus, String verificationToken, String timestamp) {
        static ClaimView of(Claim c) {
            return new ClaimView(c.getClaimId(), c.getItem().getItemId(), c.getItem().getTitle(),
                    c.getItem().getImage(), c.getClaimant().getUserId(), c.getClaimant().getName(),
                    c.getProofNote(), c.getVerificationStatus().name(), c.getVerificationToken(),
                    c.getTimestamp().toString());
        }
    }

    public record VerifyRequest(@NotBlank String token) {}

    public record NotificationView(Long notificationId, String message, boolean read, String createdAt) {
        static NotificationView of(Notification n) {
            return new NotificationView(n.getNotificationId(), n.getMessage(), n.isRead(),
                    n.getCreatedAt().toString());
        }
    }
}
