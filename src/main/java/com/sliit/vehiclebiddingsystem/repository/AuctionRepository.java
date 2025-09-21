package com.sliit.vehiclebiddingsystem.repository;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Auction entities with custom query for active auctions.
 */
@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Query("SELECT a FROM Auction a WHERE a.status = 'Active'")
    List<Auction> findActiveAuctions();

    List<Auction> findByStatus(String status);
}
