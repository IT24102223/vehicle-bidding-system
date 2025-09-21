package com.sliit.vehiclebiddingsystem.controller;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;
import com.sliit.vehiclebiddingsystem.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for auction views and actions.
 * Handles user and admin interfaces with security.
 */
@Controller
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private VehicleListingRepository vehicleListingRepository;

    // User View: List active auctions
    @GetMapping("/auctions")
    public String userAuctions(Model model, @RequestParam(required = false) String search) {
        List<Auction> auctions = (search == null) ? auctionService.getActiveAuctions() :
                auctionService.getActiveAuctions().stream()
                        .filter(a -> a.getListing().getMake().toLowerCase().contains(search.toLowerCase()) ||
                                a.getListing().getModel().toLowerCase().contains(search.toLowerCase()))
                        .toList();
        model.addAttribute("auctions", auctions);
        return "user-auctions";
    }

    // Admin View: Manage all auctions
    @GetMapping("/admin/auctions")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAuctions(Model model) {
        model.addAttribute("auctions", auctionService.getAllAuctions());
        model.addAttribute("auction", new Auction());
        return "admin-auctions";
    }

    // Admin: Create new auction
    @PostMapping("/admin/auctions")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAuction(@Valid @ModelAttribute Auction auction,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("auctions", auctionService.getAllAuctions());
            return "admin-auctions";
        }
        Long listingId = auction.getListing().getListingId();
        VehicleListing listing = vehicleListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle Listing not found"));
        auction.setListing(listing);
        auctionService.createAuction(auction);
        return "redirect:/admin/auctions";
    }

    // Admin: Start pending auction
    @PostMapping("/admin/auctions/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public String startAuction(@PathVariable Long id) {
        auctionService.startAuction(id);
        return "redirect:/admin/auctions";
    }

    // Admin: End auction manually
    @PostMapping("/admin/auctions/{id}/end")
    @PreAuthorize("hasRole('ADMIN')")
    public String endAuction(@PathVariable Long id) {
        auctionService.endAuction(id);
        return "redirect:/admin/auctions";
    }
}
