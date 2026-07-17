package server.service;

import server.storage.StateManager;
import shared.model.User;
import shared.network.Response;
import java.util.Map;

public class AccountService {
    private final StateManager state = StateManager.getInstance();

    public Response charge(User user, long amount) {
        if (amount <= 0) {
            return new Response(false, "Charge amount must be positive.", null);
        }




        synchronized (user) {
            user.chargeAccount(amount);
        }

        return new Response(true, "Wallet charged successfully.", Map.of("newBalance", user.getBalance()));
    }

    public Response balanceShow(User user) {
        return new Response(true, "Balance retrieved.", Map.of("balance", user.getBalance()));
    }
}