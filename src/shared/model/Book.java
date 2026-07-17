package shared.model;

import java.io.Serializable;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String author;
    private int totalPages;
    private String genre;
    private int publishYear;
    private long price;

    public Book(String id, String title, String author, int totalPages, String genre, int publishYear, long price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.totalPages = totalPages;
        this.genre = genre;
        this.publishYear = publishYear;
        this.price = price;
    }


    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getTotalPages() { return totalPages; }
    public String getGenre() { return genre; }
    public int getPublishYear() { return publishYear; }
    public long getPrice() { return price; }
}