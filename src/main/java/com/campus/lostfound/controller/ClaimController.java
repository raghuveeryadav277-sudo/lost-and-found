package com.campus.lostfound.controller;

import com.campus.lostfound.controller.Dtos.ClaimRequest;
import com.campus.lostfound.controller.Dtos.ClaimView;
import com.campus.lostfound.controller.Dtos.VerifyRequest;
import com.campus.lostfound.model.Claim;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.ClaimRepository;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.service.CurrentUserService;
import com.campus.lostfound.service.NotificationService;
import com.campus.lostfound.service.QrService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Claim and Verification module - the heart of the flow:
 * Claim -> Approve (QR token is generated) -> Verify at handover -> Returned.
 */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimRepository claims;
    private final ItemRepository items;
    private final CurrentUserService currentUser;
    private final NotificationService notificationService;
    private final QrService qrService;

    public ClaimController(ClaimRepository claims, ItemRepository items,
                           CurrentUserService currentUser,
                           NotificationService notificationService, QrService qrService) {
        this.claims = claims;
        this.items = items;
        this.currentUser = currentUser;
        this.notificationService = notificationService;
        this.qrService = qrService;
    }

    // ------------------------------------------------------------ raise a claim
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ClaimRequest req) {
        User me = currentUser.currentUser();
        Item item = items.findById(req.itemId()).orElse(null);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Item not found"));
        }
        if (item.getReportedBy().getUserId().equals(me.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot claim your own report"));
        }
        if (item.getStatus() != Item.Status.OPEN) {
            return ResponseEntity.badRequest().body(Map.of("error", "This item is not open for claims"));
        }
        boolean already = claims.existsByItemAndClaimantAndVerificationStatusIn(
                item, me, List.of(Claim.VerificationStatus.PENDING, Claim.VerificationStatus.APPROVED));
        if (already) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "You already have an active claim on this item"));
        }

        Claim claim = new Claim();
        claim.setItem(item);
        claim.setClaimant(me);
        claim.setProofNote(req.proofNote());
        Claim saved = claims.save(claim);

        notificationService.notify(item.getReportedBy(),
                me.getName() + " claimed your " + item.getType() + " item '" + item.getTitle()
                        + "'. Review the claim in My Claims.");
        return ResponseEntity.status(HttpStatus.CREATED).body(ClaimView.of(saved));
    }

    // ------------------------------------------------------------ lists
    @GetMapping("/mine")
    public List<ClaimView> mine() {
        return claims.findByClaimantOrderByTimestampDesc(currentUser.currentUser())
                .stream().map(ClaimView::of).toList();
    }

    /** Claims raised on items that I reported - these wait for my decision. */
    @GetMapping("/received")
    public List<ClaimView> received() {
        return claims.findByItemReportedByOrderByTimestampDesc(currentUser.currentUser())
                .stream().map(ClaimView::of).toList();
    }

    // ------------------------------------------------------------ approve / reject
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        User me = currentUser.currentUser();
        Claim claim = claims.findById(id).orElse(null);
        if (claim == null) return notFound();
        if (!canDecide(me, claim)) return forbidden();
        if (claim.getVerificationStatus() != Claim.VerificationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Claim is not pending"));
        }
        Item item = claim.getItem();
        if (item.getStatus() != Item.Status.OPEN) {
            return ResponseEntity.badRequest().body(Map.of("error", "Item already has an approved claim"));
        }

        // Approval generates the one-time verification token + QR for handover.
        claim.setVerificationStatus(Claim.VerificationStatus.APPROVED);
        claim.setVerificationToken(UUID.randomUUID().toString());
        claims.save(claim);

        item.setStatus(Item.Status.CLAIMED);
        items.save(item);

        notificationService.notify(claim.getClaimant(),
                "Your claim on '" + item.getTitle() + "' was APPROVED. Show the QR code in "
                        + "My Claims at handover so the return can be verified.");
        return ResponseEntity.ok(ClaimView.of(claim));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        User me = currentUser.currentUser();
        Claim claim = claims.findById(id).orElse(null);
        if (claim == null) return notFound();
        if (!canDecide(me, claim)) return forbidden();
        if (claim.getVerificationStatus() != Claim.VerificationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Claim is not pending"));
        }
        claim.setVerificationStatus(Claim.VerificationStatus.REJECTED);
        claims.save(claim);
        notificationService.notify(claim.getClaimant(),
                "Your claim on '" + claim.getItem().getTitle() + "' was rejected by the reporter.");
        return ResponseEntity.ok(ClaimView.of(claim));
    }

    // ------------------------------------------------------------ QR code image
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable Long id) {
        User me = currentUser.currentUser();
        Claim claim = claims.findById(id).orElse(null);
        if (claim == null) return ResponseEntity.notFound().build();
        boolean allowed = claim.getClaimant().getUserId().equals(me.getUserId())
                || claim.getItem().getReportedBy().getUserId().equals(me.getUserId())
                || me.getRole() == User.Role.ADMIN;
        if (!allowed || claim.getVerificationToken() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String content = "LOSTFOUND-VERIFY:" + claim.getVerificationToken();
        return ResponseEntity.ok(qrService.qrPng(content, 320));
    }

    // ------------------------------------------------------------ verify at handover
    /** The finder (item reporter) or an admin confirms the handover with the token. */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest req) {
        User me = currentUser.currentUser();
        String token = req.token().trim();
        if (token.startsWith("LOSTFOUND-VERIFY:")) {
            token = token.substring("LOSTFOUND-VERIFY:".length());
        }
        Claim claim = claims.findByVerificationToken(token).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Invalid verification code"));
        }
        if (claim.getVerificationStatus() != Claim.VerificationStatus.APPROVED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "This claim is not awaiting verification"));
        }
        boolean allowed = claim.getItem().getReportedBy().getUserId().equals(me.getUserId())
                || me.getRole() == User.Role.ADMIN;
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the item reporter or an admin can verify a handover"));
        }

        claim.setVerificationStatus(Claim.VerificationStatus.VERIFIED);
        claims.save(claim);

        Item item = claim.getItem();
        item.setStatus(Item.Status.RETURNED);
        items.save(item);

        notificationService.notify(claim.getClaimant(),
                "Verified! '" + item.getTitle() + "' has been marked RETURNED. Case closed.");
        notificationService.notify(item.getReportedBy(),
                "Handover verified for '" + item.getTitle() + "'. The item is RETURNED and the case is closed.");
        return ResponseEntity.ok(ClaimView.of(claim));
    }

    private boolean canDecide(User me, Claim claim) {
        return claim.getItem().getReportedBy().getUserId().equals(me.getUserId())
                || me.getRole() == User.Role.ADMIN;
    }

    private ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Claim not found"));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only the item reporter or an admin can decide this claim"));
    }
}
