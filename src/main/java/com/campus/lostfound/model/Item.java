package com.campus.lostfound.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Table: items (item_id PK, category, description, image, reported_by FK)
 * plus the fields the flow needs: type (LOST/FOUND), title, location,
 * status and the perceptual image hash used for image matching.
 */
@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    public enum Type { LOST, FOUND }

    /** OPEN -> CLAIMED -> RETURNED (case closed). CLOSED = closed by admin. */
    public enum Status { OPEN, CLAIMED, RETURNED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Path of the uploaded image, e.g. /uploads/abc123.png (nullable). */
    @Column(name = "image")
    private String image;

    /** 64-bit perceptual average-hash of the image, used for image matching. */
    @Column(name = "image_hash")
    private Long imageHash;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
