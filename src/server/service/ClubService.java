package server.service;

import server.storage.StateManager;
import server.notification.NotificationManager;
import shared.model.*;
import shared.network.Response;
import java.util.*;

public class ClubService {
    private final StateManager state = StateManager.getInstance();

    public Response createClub(User owner, String clubName) {
        if (clubName == null || clubName.trim().isEmpty()) {
            return new Response(false, "Club name cannot be blank.", null);
        }

        for (BookClub club : state.getClubs().values()) {
            if (club.getName().equalsIgnoreCase(clubName)) {
                return new Response(false, "Club name is already occupied.", null);
            }
        }

        BookClub newClub = new BookClub(clubName, owner.getId());
        state.getClubs().put(newClub.getId(), newClub);

        return new Response(true, "Club created successfully.", Map.of("clubId", newClub.getId()));
    }

    public Response listMyClubs(User user) {
        List<BookClub> myClubs = new ArrayList<>();
        for (BookClub club : state.getClubs().values()) {
            if (club.isMember(user.getId())) {
                myClubs.add(club);
            }
        }
        return new Response(true, "Personal clubs list loaded.", Map.of("clubs", myClubs));
    }

    public Response listAllClubs() {
        return new Response(true, "Total clubs list loaded.", Map.of("clubs", new ArrayList<>(state.getClubs().values())));
    }

    public Response joinRequest(User user, String clubId) {
        BookClub club = state.getClubs().get(clubId);
        if (club == null) {
            return new Response(false, "Club target not found.", null);
        }

        if (club.isMember(user.getId())) {
            return new Response(false, "You are already a member of this club.", null);
        }

        for (JoinRequest req : state.getJoinRequests().values()) {
            if (req.getClubId().equals(clubId) && req.getUserId().equals(user.getId())) {
                return new Response(false, "A duplicate join request is already pending.", null);
            }
        }

        JoinRequest request = new JoinRequest(clubId, user.getId(), user.getUsername());
        state.getJoinRequests().put(request.getId(), request);


        NotificationManager.getInstance().sendNotification(
                club.getOwnerId(), "JOIN_REQUEST", "User '" + user.getUsername() + "' requested to join club '" + club.getName() + "'."
        );

        return new Response(true, "Membership request submitted.", null);
    }

    //TODO:String userId(I'll probably forget about it)
    public Response acceptJoinRequest(User owner, String requestId) {
        JoinRequest req = state.getJoinRequests().get(requestId);
        if (req == null) {
            return new Response(false, "Request target not found.", null);
        }

        BookClub club = state.getClubs().get(req.getClubId());
        if (club == null) {
            return new Response(false, "Associated club not found.", null);
        }

        if (!club.isOwner(owner.getId())) {
            return new Response(false, "Only the owner is authorized to accept requests.", null);
        }

        synchronized (club) {
            club.addMember(req.getUserId());
        }
        state.getJoinRequests().remove(requestId);


        NotificationManager.getInstance().sendNotification(
                req.getUserId(), "JOIN_ACCEPTED", "Congratulations! Your join request for '" + club.getName() + "' was approved."
        );

        return new Response(true, "Join request approved.", null);
    }

    public Response denyJoinRequest(User owner, String requestId) {
        JoinRequest req = state.getJoinRequests().get(requestId);
        if (req == null) {
            return new Response(false, "Request target not found.", null);
        }

        BookClub club = state.getClubs().get(req.getClubId());
        if (club == null) {
            return new Response(false, "Associated club not found.", null);
        }

        if (!club.isOwner(owner.getId())) {
            return new Response(false, "Only the owner is authorized to reject requests.", null);
        }

        state.getJoinRequests().remove(requestId);


        NotificationManager.getInstance().sendNotification(
                req.getUserId(), "JOIN_DENIED", "Your request to join '" + club.getName() + "' has been rejected."
        );

        return new Response(true, "Join request rejected.", null);
    }

    public Response getClubView(User user, String clubId) {
        BookClub club = state.getClubs().get(clubId);
        if (club == null) {
            return new Response(false, "Club target not found.", null);
        }

        if (!club.isMember(user.getId())) {
            return new Response(false, "Club views are restricted to members only.", null);
        }

        return new Response(true, "Club view retrieved.", Map.of(
                "clubId", club.getId(),
                "clubName", club.getName()
        ));
    }

    public Response listMembers(User user, String clubId) {
        BookClub club = state.getClubs().get(clubId);
        if (club == null) {
            return new Response(false, "Club target not found.", null);
        }

        if (!club.isMember(user.getId())) {
            return new Response(false, "Club members list is restricted to members only.", null);
        }

        List<Map<String, String>> membersList = new ArrayList<>();
        for (String mId : club.getMemberIds()) {
            User m = state.getUsers().get(mId);
            if (m != null) {
                membersList.add(Map.of("id", m.getId(), "username", m.getUsername()));
            }
        }
        return new Response(true, "Club members retrieved.", Map.of("members", membersList));
    }



    public Response removeMember(User owner, String clubId, String memberId) {
        BookClub club = state.getClubs().get(clubId);
        if (club == null) {
            return new Response(false, "Club target not found.", null);
        }

        if (!club.isOwner(owner.getId())) {
            return new Response(false, "Only the owner is authorized to remove members.", null);
        }

        if (owner.getId().equals(memberId)) {
            return new Response(false, "You cannot remove yourself (the owner) from the club.", null);
        }

        boolean removed;
        synchronized (club) {
            removed = club.removeMember(memberId);
        }


        if (removed) {
            NotificationManager.getInstance().sendNotification(
                    memberId, "MEMBER_REMOVED", "You have been removed from club '" + club.getName() + "'."
            );
            return new Response(true, "Member removed successfully.", null);
        }
        return new Response(false, "User is not a member of this club.", null);
    }
}