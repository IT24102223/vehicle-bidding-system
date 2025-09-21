package com.sliit.vehiclebiddingsystem.service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing auctions, including real-time updates and timer logic.
 * Broadcasts changes via WebSocket for live updates.
 */
@Service
public class AuctionService {

    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;  // For WebSocket broadcasts

    public List<Auction> getAllAuctions() {
        List<Auction> auctions = auctionRepository.findAll();
        for (Auction auction : auctions) {
            Double highest = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
            auction.setHighestBid(highest != null ? highest : 0.0);
        }
        return auctions;
    }

    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = auctionRepository.findByStatus("Active");
        for (Auction auction : auctions) {
            Double highest = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
            auction.setHighestBid(highest != null ? highest : 0.0);
        }
        return auctions;
    }

    public Auction getAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
        Double highest = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
        auction.setHighestBid(highest != null ? highest : 0.0);
        return auction;
    }

    public Auction createAuction(Auction auction) {
        if (auction.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("End time must be in the future");
        }
        if (!"Approved".equals(auction.getListing().getStatus())) {
            throw new IllegalArgumentException("Vehicle listing must be approved before creating auction");
        }
        auction.setStartTime(LocalDateTime.now());
        auction.setCurrentEndTime(auction.getEndTime());
        auction.setStatus("Active");
        Auction saved = auctionRepository.save(auction);
        broadcastAuctionUpdate(saved);
        return saved;
    }

    public Auction startAuction(Long id) {
        Auction auction = getAuctionById(id);
        if ("Pending".equals(auction.getStatus())) {
            auction.setStartTime(LocalDateTime.now());
            auction.setCurrentEndTime(auction.getEndTime());
            auction.setStatus("Active");
            Auction updated = auctionRepository.save(auction);
            broadcastAuctionUpdate(updated);
            return updated;
        }
        return auction;
    }

    public void endAuction(Long id) {
        Auction auction = getAuctionById(id);
        if ("Active".equals(auction.getStatus())) {
            auction.setStatus("Closed");
            // Integrate with Reporting: Determine winner based on highest bid
            auction.setWinner(bidRepository.findHighestBidderByAuctionId(id));
            Auction updated = auctionRepository.save(auction);
            broadcastAuctionUpdate(updated);
        }
    }

    // Call this from BidService when a bid is placed
    public void extendAuctionIfNeeded(Long id, LocalDateTime bidTime) {
        Auction auction = getAuctionById(id);
        if ("Active".equals(auction.getStatus())) {
            LocalDateTime threshold = auction.getCurrentEndTime().minusSeconds(30);
            if (bidTime.isAfter(threshold)) {
                auction.setCurrentEndTime(
                        auction.getCurrentEndTime().plusSeconds(auction.getExtensionDuration()));
                Auction updated = auctionRepository.save(auction);
                broadcastAuctionUpdate(updated);
                logger.info("Auction {} extended to {}", id, auction.getCurrentEndTime());
            }
        }
    }

    // Scheduled task to auto-end expired auctions (runs every minute)
    @Scheduled(fixedRate = 60000)
    public void checkAndEndExpiredAuctions() {
        List<Auction> activeAuctions = getActiveAuctions();
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions) {
            if (auction.getCurrentEndTime().isBefore(now)) {
                endAuction(auction.getAuctionId());
            }
        }
    }

    // Broadcast update to all clients subscribed to /topic/auctions/{id}
    private void broadcastAuctionUpdate(Auction auction) {
        messagingTemplate.convertAndSend("/topic/auctions/" + auction.getAuctionId(), auction);
    }
}
