package shared.model;

import shared.model.enums.BookStatus;
import java.io.Serializable;

public class LibraryBook implements Serializable {
    private static final long serialVersionUID = 1L;



    public static final String FAUST_BOOK_ID = "FAUST_001";

    private String bookId;
    private String title;
    private int totalPages;
    private int currentPage;
    private BookStatus status;

    public LibraryBook(String bookId, String title, int totalPages) {
        this.bookId = bookId;
        this.title = title;
        this.totalPages = totalPages;
        this.currentPage = 0;
        this.status = BookStatus.UNREAD;
    }

    public void updateProgress(int page) throws IllegalArgumentException {
        if (page < 0 || page > totalPages) {
            throw new IllegalArgumentException("Invalid page number.");
        }


        if (bookId.equals(FAUST_BOOK_ID) && page == totalPages) {
            throw new IllegalArgumentException("Faust cannot be fully read! It remains a mystery.");
        }

        this.currentPage = page;


        if (this.currentPage == 0) {
            this.status = BookStatus.UNREAD;
        } else if (this.currentPage == this.totalPages) {
            this.status = BookStatus.READ;
        } else {
            this.status = BookStatus.CURRENTLY_READING;
        }
    }


    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public BookStatus getStatus() { return status; }
}