package server.service;

import server.storage.StateManager;
import server.notification.NotificationManager;
import shared.model.*;
import shared.network.Response;
import java.util.*;

public class FundraiserService {
    private final StateManager state = StateManager.getInstance();

    public Response createFundraiser(User creator, String clubId, String bookId) {
        BookClub club = state.getClubs().get(clubId);
        if (club == null) {
            return new Response(false, "Club not found.", null);
        }

        if (!club.isMember(creator.getId())) {
            return new Response(false, "Only club members can create fundraisers.", null);
        }

        Book book = state.getBooks().get(bookId);
        if (book == null) {
            return new Response(false, "Book target not found.", null);
        }


        synchronized (club) {
            if (club.getActiveFundraiserId(bookId) != null) {
                return new Response(false, "An active fundraiser already exists for this book in this club.", null);
            }


            long count = 0;
            for (String mId : club.getMemberIds()) {
                User m = state.getUsers().get(mId);
                if (m != null && !m.getLibrary().containsKey(bookId)) {
                    count++;
                }
            }

            if (count == 0) {
                return new Response(false, "All members already own this book.", null);
            }

            long targetAmount = count * book.getPrice();
            Fundraiser fundraiser = new Fundraiser(clubId, bookId, targetAmount);
            state.getFundraisers().put(fundraiser.getId(), fundraiser);
            club.setActiveFundraiserId(bookId, fundraiser.getId());

            NotificationManager.getInstance().broadcastToClub(
                    club.getMemberIds(), "FUNDRAISER_CREATED", "Fundraiser started for '" + book.getTitle() + "' in club '" + club.getName() + "'. Target: " + targetAmount
            );

            return new Response(true, "Fundraiser created successfully.", Map.of("fundraiserId", fundraiser.getId(), "targetAmount", targetAmount));
        }
    }

    public Response getFundraiserProgress(User user, String fundraiserId) {
        Fundraiser fr = state.getFundraisers().get(fundraiserId);
        if (fr == null) {
            return new Response(false, "Fundraiser target not found.", null);
        }

        BookClub club = state.getClubs().get(fr.getClubId());
        if (club == null || !club.isMember(user.getId())) {
            return new Response(false, "Club permissions required to view this fundraiser.", null);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("id", fr.getId());
        details.put("clubId", fr.getClubId());
        details.put("bookId", fr.getBookId());
        details.put("targetAmount", fr.getTargetAmount());
        details.put("currentAmount", fr.getCurrentAmount());
        details.put("remainingAmount", fr.getRemainingAmount());
        details.put("status", fr.getStatus().name());
        details.put("donations", fr.getDonationHistory());

        return new Response(true, "Fundraiser progress loaded.", details);
    }
    public Response donate(User user, String fundraiserId, long offeredAmount) {
        if (offeredAmount <= 0) {
            return new Response(false, "Donation amount must be positive.", null);
        }

        Fundraiser fr = state.getFundraisers().get(fundraiserId);
        if (fr == null) {
            return new Response(false, "Fundraiser not found.", null);
        }

        if (!fr.isActive()) {
            return new Response(false, "Fundraiser is no longer active.", null);
        }

        BookClub club = state.getClubs().get(fr.getClubId());
        if (club == null || !club.isMember(user.getId())) {
            return new Response(false, "Only club members can donate to this fundraiser.", null);
        }


        synchronized (club){
            synchronized (fr) {
            if (!fr.isActive()) {
                return new Response(false, "This fundraiser completed while processing.", null);
            }

            synchronized (user) {
                if (user.getBalance() <= 0) {
                    return new Response(false, "Insufficient balance.", null);
                }


                long actualOffered = Math.min(
                        offeredAmount,
                        Math.min(user.getBalance(), fr.getRemainingAmount())
                );
                long acceptedAmount = fr.addDonation(user.getId(), user.getUsername(), actualOffered);

                if (acceptedAmount <= 0) {
                    return new Response(false, "No donation accepted.", null);
                }

                user.deductBalance(acceptedAmount);

                NotificationManager.getInstance().broadcastToClub(
                        club.getMemberIds(), "DONATION_RECEIVED", "User " + user.getUsername() + " donated " + acceptedAmount + " to fundraiser " + fr.getId()
                );

                if (fr.isCompleted()) {
                    Book book = state.getBooks().get(fr.getBookId());
                    if (book != null) {
                        for (String memberId : club.getMemberIds()) {
                            User member = state.getUsers().get(memberId);
                            if (member != null) {
                                synchronized (member) {
                                    if (!member.ownsBook(book.getId())) {
                                        LibraryBook giftBook = new LibraryBook(book.getId(), book.getTitle(), book.getTotalPages());
                                        member.addBook(giftBook);
                                        NotificationManager.getInstance().sendNotification(
                                                memberId, "BOOK_ADDED", "The book '" + book.getTitle() + "' has been added to your library from the completed fundraiser."
                                        );
                                    }
                                }
                            }
                        }
                    }

                    club.removeActiveFundraiser(fr.getBookId());
                    NotificationManager.getInstance().broadcastToClub(
                            club.getMemberIds(), "FUNDRAISER_COMPLETED", "Fundraiser for '" + (book != null ? book.getTitle() : fr.getBookId()) + "' has completed!"
                    );
                }

                Map<String, Object> result = new HashMap<>();
                result.put("donatedAmount", acceptedAmount);
                result.put("remaining", fr.getRemainingAmount());
                result.put("isCompleted", fr.isCompleted());
                return new Response(true, "Donation accepted.", result);
            }
        }
    }
    }
}