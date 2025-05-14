package org.example.Service;

import org.example.Model.Wallet;
import org.example.Repos.WalletRepo;

import java.math.BigDecimal;

public class WalletService {
    private WalletRepo walletRepo;

    public WalletService(WalletRepo walletRepo) {
        this.walletRepo = walletRepo;
    }

    public synchronized  void topUpWallet(String user, BigDecimal amt){
        walletRepo.addTowallet(user, amt);
    }

    public synchronized  void decementWallet(String user, BigDecimal amt){
        walletRepo.removeFromWallet(user, amt);
    }

    public void fetchAmount(String user){
        walletRepo.getAmount(user);
    }

    public void createWallet(String key){
        Wallet wallet = new Wallet(key);
        walletRepo.createWallet(key, wallet);
    }
}
