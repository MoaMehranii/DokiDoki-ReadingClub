package server;

import server.service.*;
import server.notification.NotificationManager;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;
import shared.util.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService = new AuthService();
    private final AccountService accountService = new AccountService();
    private final BookService bookService = new BookService();
    private final ClubService clubService = new ClubService();
    private final FundraiserService fundraiserService = new FundraiserService();

    private PrintWriter out;
    private User authenticatedUser = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = writer;
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                Response response;
                try {
                    Request request = JsonUtil.fromJson(inputLine, Request.class);
                    if (request == null) {
                        response = new Response(false, "Invalid payload context format.", null);
                    } else {
                        response = dispatchRequest(request);
                    }
                } catch (Exception e) {
                    response = new Response(false, "Processing fault context: " + e.getMessage(), null);
                }
                out.println(JsonUtil.toJson(response));
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected target: " + socket.getInetAddress());
        } finally {
            cleanUpSession();
        }
    }

    private Response dispatchRequest(Request request) {
        String command = request.getCommand();
        Map<String, Object> data = request.getData();

        if (command == null) {
            return new Response(false, "Command identification failed.", null);
        }


        if (command.equalsIgnoreCase("register")) {
            return authService.register((String) data.get("username"), (String) data.get("password"));
        } else if (command.equalsIgnoreCase("login")) {
            Response loginResponse = authService.login((String) data.get("username"), (String) data.get("password"));
            if (loginResponse.isSuccess()) {
                String token = (String) loginResponse.getData().get("token");
                String userId = (String) loginResponse.getData().get("userId");
                this.authenticatedUser = server.storage.StateManager.getInstance().getUserByToken(token);

            }
            return loginResponse;
        }


        String token = request.getToken();
        User user = server.storage.StateManager.getInstance().getUserByToken(token);
        if (user == null) {
            return new Response(false, "Unauthorized. Token verification failed.", null);
        }
        this.authenticatedUser = user;

        try {
            switch (command.toLowerCase()) {
                case "logout":
                    Response logoutRes = authService.logout(token);
                    cleanUpSession();
                    return logoutRes;

                case "register_udp":
                    int udpPort = Integer.parseInt(data.get("udpPort").toString());
                    String ipAddress = socket.getInetAddress().getHostAddress();
                    NotificationManager.getInstance().registerUdpClient(user.getId(), ipAddress, udpPort);
                    return new Response(true, "UDP Client registration succeeded.", null);

                case "account_charge":
                    long chargeAmt = Long.parseLong(data.get("amount").toString());
                    return accountService.charge(user, chargeAmt);

                case "balance_show":
                    return accountService.balanceShow(user);

                case "books_market_list":
                    return bookService.getMarketList();

                case "book_buy":
                    return bookService.buyBook(user, (String) data.get("bookId"));

                case "progress_submit":
                    int page = Integer.parseInt(data.get("page").toString());
                    return bookService.submitProgress(user, (String) data.get("bookId"), page);

                case "library_read_not_list":
                    return bookService.getLibraryByStatus(user, "UNREAD");

                case "library_reading_list":
                    return bookService.getLibraryByStatus(user, "CURRENTLY_READING");

                case "library_read_list":
                    return bookService.getLibraryByStatus(user, "READ");

                case "club_create":
                    return clubService.createClub(user, (String) data.get("clubName"));

                case "clubs_list":
                    return clubService.listMyClubs(user);

                case "clubs_list_total":
                    return clubService.listAllClubs();

                case "join":
                    return clubService.joinRequest(user, (String) data.get("clubId"));

                case "accept_join_request":
                    return clubService.acceptJoinRequest(user, (String) data.get("requestId"));

                case "deny_join_request":
                    return clubService.denyJoinRequest(user, (String) data.get("requestId"));

                case "club_view":
                    return clubService.getClubView(user, (String) data.get("clubId"));

                case "members_club_list":
                    return clubService.listMembers(user, (String) data.get("clubId"));

                case "member_remove":
                    return clubService.removeMember(user, (String) data.get("clubId"), (String) data.get("memberId"));

                case "fundraiser_create":
                    return fundraiserService.createFundraiser(user, (String) data.get("clubId"), (String) data.get("bookId"));

                case "progress_fundraiser_view":
                    return fundraiserService.getFundraiserProgress(user, (String) data.get("fundraiserId"));

                case "donate":
                    long donateAmt = Long.parseLong(data.get("amount").toString());
                    return fundraiserService.donate(user, (String) data.get("fundraiserId"), donateAmt);

                default:
                    return new Response(false, "Unknown command logic: " + command, null);
            }
        } catch (Exception e) {
            return new Response(false, "Internal processing fault: " + e.getMessage(), null);
        }
    }

    private void cleanUpSession() {
        if (authenticatedUser != null) {
            String userId = authenticatedUser.getId();
            NotificationManager.getInstance().unregisterUdpClient(userId);
            if (authenticatedUser.getSessionToken() != null) {
                authService.logout(authenticatedUser.getSessionToken());
            }
            authenticatedUser = null;
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}