package com.campus.lostfound.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Table: claims (claim_id PK, item_id FK, claimant_id FK,
 * verification_status, timestamp) plus the claimant's proof note and the
 * one-time verification token whose QR code is shown at handover.
 */
@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    /** PENDING -> APPROVED -> VERIFIED (item returned). REJECTED ends a claim. */
    public enum VerificationStatus { PENDING, APPROVED, REJECTED, VERIFIED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    private Long claimId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claimant_id")
    private User claimant;

    /** How the claimant proves the item is theirs (bill, photo, unique marks...). */
    @Column(name = "proof_note", length = 1000)
    private String proofNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /** Unique token generated on approval; encoded into the handover QR code. */
    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
