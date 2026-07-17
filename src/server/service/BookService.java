package server.service;

import server.storage.StateManager;
import server.notification.NotificationManager;
import shared.model.*;
import shared.network.Response;
import java.util.*;

public class BookService {
    private final StateManager state = StateManager.getInstance();

    public Response getMarketList() {
        return new Response(true, "Market list loaded.", Map.of("books", new ArrayList<>(state.getBooks().values())));
    }

    public Response buyBook(User user, String bookId) {
        Book book = state.getBooks().get(bookId);
        if (book == null) {
            return new Response(false, "Book not found.", null);
        }


        BookClub targetClub = null;
        Fundraiser targetFundraiser = null;

        for (BookClub club : state.getClubs().values()) {
            if (!club.isMember(user.getId())) {
                continue;
            }

            String fundraiserId = club.getActiveFundraiserId(bookId);
            if (fundraiserId == null) {
                continue;
            }

            Fundraiser fr = state.getFundraisers().get(fundraiserId);
            if (fr != null && fr.isActive()) {
                targetClub = club;
                targetFundraiser = fr;
                break;
            }
        }


        if (targetFundraiser == null) {
            synchronized (user) {
                if (user.ownsBook(bookId)) {
                    return new Response(false, "You already own this book.", null);
                }

                if (user.getBalance() < book.getPrice()) {
                    return new Response(false, "Insufficient balance.", null);
                }

                user.deductBalance(book.getPrice());
                user.addBook(new LibraryBook(bookId, book.getTitle(), book.getTotalPages()));
            }

            return new Response(true, "Book purchased successfully.", null);
        }



        synchronized (targetClub) {
            synchronized (targetFundraiser) {
                synchronized (user) {

                    if (user.ownsBook(bookId)) {
                        return new Response(false, "You already own this book.", null);
                    }

                    if (user.getBalance() < book.getPrice()) {
                        return new Response(false, "Insufficient balance.", null);
                    }

                    user.deductBalance(book.getPrice());
                    user.addBook(new LibraryBook(bookId, book.getTitle(), book.getTotalPages()));

                    targetFundraiser.reduceTarget(book.getPrice());

                    if (targetFundraiser.isCompleted()) {

                        long excess = targetFundraiser.getExcessAmount();
                        Donation lastDonation = targetFundraiser.getLastDonation();

                        if (excess > 0 && lastDonation != null) {
                            User lastDonor = state.getUsers().get(lastDonation.getUserId());

                            if (lastDonor != null) {
                                synchronized (lastDonor) {
                                    lastDonor.chargeAccount(excess);
                                    targetFundraiser.refundExcess(excess);
                                }
                            }
                        }

                        for (String memberId : targetClub.getMemberIds()) {
                            User member = state.getUsers().get(memberId);

                            if (member == null) {
                                continue;
                            }

                            synchronized (member) {
                                if (!member.ownsBook(bookId)) {
                                    member.addBook(
                                            new LibraryBook(
                                                    book.getId(),
                                                    book.getTitle(),
                                                    book.getTotalPages()
                                            )
                                    );

                                    NotificationManager.getInstance().sendNotification(
                                            memberId,
                                            "BOOK_ADDED",
                                            "Book '" + book.getTitle() + "' is now in your library via fundraiser completion!"
                                    );
                                }
                            }
                        }

                        targetClub.removeActiveFundraiser(bookId);

                        NotificationManager.getInstance().broadcastToClub(
                                targetClub.getMemberIds(),
                                "FUNDRAISER_COMPLETED",
                                "Fundraiser for '" + book.getTitle() + "' completed!"
                        );

                    } else {

                        NotificationManager.getInstance().broadcastToClub(
                                targetClub.getMemberIds(),
                                "FUNDRAISER_UPDATE",
                                "A club member purchased '" + book.getTitle() + "' personally. Fundraiser target decreased."
                        );
                    }
                }
            }
        }

        return new Response(true, "Book purchased successfully.", null);
    }

    public Response submitProgress(User user, String bookId, int page) {
        synchronized (user) {
            LibraryBook libBook = user.getLibrary().get(bookId);
            if (libBook == null) {
                return new Response(false, "Book is not registered in your library.", null);
            }

            if (page < 0 || page > libBook.getTotalPages()) {
                return new Response(false, "Invalid reading progression coordinate.", null);
            }


            libBook.updateProgress(page);

            Map<String, Object> data = new HashMap<>();
            data.put("status", libBook.getStatus().name());
            data.put("currentPage", libBook.getCurrentPage());
            return new Response(true, "Progress updated successfully.", data);
        }
    }

    public Response getLibraryByStatus(User user, String statusName) {
        synchronized (user) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (LibraryBook lb : user.getLibrary().values()) {
                if (lb.getStatus().name().equalsIgnoreCase(statusName)) {
                    result.add(Map.of(
                            "bookId", lb.getBookId(),
                            "title", lb.getTitle(),
                            "currentPage", lb.getCurrentPage(),
                            "totalPages", lb.getTotalPages(),
                            "status", lb.getStatus().name()
                    ));
                }
            }
            return new Response(true, "Library filtered successfully.", Map.of("books", result));
        }
    }
}