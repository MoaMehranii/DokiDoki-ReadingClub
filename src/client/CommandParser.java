package client;

import shared.util.HashUtil;
import shared.network.Request;

import java.util.Arrays;

public class CommandParser {
    private final ClientSession session;

    public CommandParser(ClientSession session) {
        this.session = session;
    }

    public void parseAndExecute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        try {
            if (session.isInClubMode()) {
                if (command.equals("exit")) {
                    session.leaveClub();
                    System.out.println("Exited club mode. Returned to general commands.");
                    return;
                }
                handleClubModeCommands(command, args);
            } else {
                handleGeneralCommands(command, args);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error parsing command: " + e.getMessage());
        }
    }

    private void handleGeneralCommands(String command, String[] args) {
        Request request = new Request(command);
        switch (command) {
            case "register":
            case "login":
                requireArgs(args, 2, command + " <username> <password>");
                request.addArgument("username", args[0]);
                request.addArgument("password", HashUtil.hashPassword(args[1]));
                break;

            case "logout":
                requireArgs(args, 0, "logout");
                session.sendRequest(request);
                session.logout();
                return;

            case "account_charge":
                requireArgs(args, 1, "account_charge <amount>");
                request.addArgument("amount", args[0]);
                break;

            case "balance_show":
            case "books_market_list":
            case "library_read_not_list":
            case "library_reading_list":
            case "library_read_list":
            case "clubs_list":
            case "clubs_list_total":
                requireArgs(args, 0, command);
                break;

            case "book_buy":
                requireArgs(args, 1, "book_buy <bookId>");
                request.addArgument("bookId", args[0]);
                break;

            case "progress_submit":
                requireArgs(args, 2, "progress_submit <bookId> <page>");
                request.addArgument("bookId", args[0]);
                request.addArgument("page", args[1]);
                break;

            case "club_create":
                requireArgs(args, 1, "club_create <name>");
                request.addArgument("clubName", args[0]);
                break;

            case "join":
                requireArgs(args, 1, "join <clubId>");
                request.addArgument("clubId", args[0]);
                break;

            case "accept_join_request":
            case "deny_join_request":
                requireArgs(args, 1, command + " <userId>");
                request.addArgument("userId", args[0]);
                break;

            case "club_view":
                requireArgs(args, 1, "club_view <clubId>");
                request.addArgument("clubId", args[0]);
                break;

            default:
                System.out.println("Unknown command: " + command);
                return;
        }

        session.sendRequest(request);
    }

    private void handleClubModeCommands(String command, String[] args) {
        Request request = new Request(command);

        switch (command) {
            case "members_club_list":
            case "progress_fundraiser_view":
                requireArgs(args, 0, command);
                break;

            case "member_remove":
                requireArgs(args, 1, "member_remove <userId>");
                request.addArgument("userId", args[0]);
                break;

            case "fundraiser_create":
                requireArgs(args, 1, "fundraiser_create <bookId>");
                request.addArgument("bookId", args[0]);
                break;

            case "donate":
                requireArgs(args, 1, "donate <amount>");
                request.addArgument("amount", args[0]);
                break;

            default:
                System.out.println("Unknown command or command not allowed inside club mode. Type 'exit' to leave.");
                return;
        }

        session.sendRequest(request);
    }

    private void requireArgs(String[] args, int expected, String usage) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }
}