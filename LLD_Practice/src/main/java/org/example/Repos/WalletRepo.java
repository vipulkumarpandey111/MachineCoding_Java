package org.example.Repos;

import org.example.Model.User;
import org.example.Model.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;

public class WalletRepo {
    static HashMap<String, Wallet> wallets;

    public WalletRepo() {
        wallets = new HashMap<>();
    }

    public synchronized void createWallet(String key, Wallet wallet){
        wallets.put(key, wallet);
    }

    public synchronized void addTowallet(String user, BigDecimal amt){

        Wallet wallet = wallets.get(user);
        wallet.incrementAmt(amt);
        wallets.remove(user);
        wallets.put(user, wallet);
    }

    public synchronized void removeFromWallet(String user, BigDecimal amt){
        Wallet wallet = wallets.get(user);
        wallet.decrementAmt(amt);
        wallets.remove(user);
        wallets.put(user, wallet);
    }

    public void getAmount(String user){
        if(wallets.containsKey((user))){
            Wallet wallet = wallets.get(user);
            System.out.println(wallet.getAmount());
        }
    }
}
