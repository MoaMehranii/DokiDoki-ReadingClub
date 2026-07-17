package shared.model;

import shared.model.enums.FundraiserStatus;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class Fundraiser implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String clubId;
    private String bookId;
    private long targetAmount;
    private long currentAmount;
    private FundraiserStatus status;
    private final List<Donation> donationHistory;

    public Fundraiser(String clubId, String bookId, long targetAmount) {
        if (targetAmount <= 0) {
            throw new IllegalArgumentException("Target amount must be positive.");
        }
        this.id = UUID.randomUUID().toString();
        this.clubId = clubId;
        this.bookId = bookId;
        this.targetAmount = targetAmount;
        this.currentAmount = 0;
        this.status = FundraiserStatus.ACTIVE;
        this.donationHistory = new ArrayList<>();
    }


    public synchronized long addDonation(String userId, String username, long offeredAmount) {
        if (offeredAmount <= 0) {
            throw new IllegalArgumentException("Donation amount must be positive.");
        }
        if (status != FundraiserStatus.ACTIVE) {
            throw new IllegalStateException("Fundraiser is completed.");
        }

        long remaining = getRemainingAmount();
        long actualDeduction = offeredAmount;


        if (offeredAmount > remaining) {
            actualDeduction = remaining;
        }

        this.currentAmount += actualDeduction;
        this.donationHistory.add(new Donation(userId, username, actualDeduction));

        if (this.currentAmount >= this.targetAmount) {
            this.status = FundraiserStatus.COMPLETED;
        }

        return actualDeduction;
    }


    public synchronized long reduceTarget(long bookPrice) {
        if (status != FundraiserStatus.ACTIVE) {
            return 0;
        }

        targetAmount = Math.max(0, targetAmount - bookPrice);

        if (currentAmount >= targetAmount) {
            status = FundraiserStatus.COMPLETED;
        }

        return getExcessAmount();
    }
    public long getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }
    public long getExcessAmount() {
        return Math.max(0, currentAmount - targetAmount);
    }
    public Donation getLastDonation() {
        if(donationHistory.isEmpty())
            return null;

        return donationHistory.get(donationHistory.size()-1);
    }

    public boolean isActive() {
        return status == FundraiserStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == FundraiserStatus.COMPLETED;
    }
    public boolean needsDonation() {
        return currentAmount < targetAmount;
    }

    public synchronized void refundExcess(long amount) {
        if (amount <= 0) {
            return;
        }

        currentAmount -= amount;

        if (currentAmount < targetAmount) {
            status = FundraiserStatus.ACTIVE;
        }
    }



    public String getId() { return id; }
    public String getClubId() { return clubId; }
    public String getBookId() { return bookId; }
    public long getTargetAmount() { return targetAmount; }
    public long getCurrentAmount() { return currentAmount; }
    public FundraiserStatus getStatus() { return status; }
    public List<Donation> getDonationHistory() {
        return Collections.unmodifiableList(donationHistory);
    }
}