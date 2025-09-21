package com.sliit.vehiclebiddingsystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents an auction for a vehicle listing.
 * Includes timer extension for last-second bids.
 */
@Entity
@Data
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auctionId;

    @OneToOne
    @JoinColumn(name = "listing_id", nullable = false)
    private VehicleListing listing;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime currentEndTime;  // Dynamic end time for extensions
    private Integer extensionDuration = 30;  // Seconds
    private String status = "Pending";  // Pending, Active, Closed

    @Transient
    private Double highestBid;  // Computed, not persisted
}
