package org.example;

import org.example.Repos.TransactionRepo;
import org.example.Repos.UserRepo;
import org.example.Repos.WalletRepo;
import org.example.Service.TransactionService;
import org.example.Service.UserService;
import org.example.Service.WalletService;

import java.math.BigDecimal;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class WalletDriver {
    public static void main(String[] args) {
        TransactionRepo transactionRepo = new TransactionRepo();
        UserRepo userRepo = new UserRepo();
        WalletRepo walletRepo = new WalletRepo();

        WalletService walletService = new WalletService(walletRepo);
        TransactionService transactionService = new TransactionService(transactionRepo, walletService);
        UserService userService = new UserService(userRepo, walletService);

        userService.RegisterUser("Vipul");
        walletService.topUpWallet("Vipul", new BigDecimal(100));
        //walletService.fetchAmount("Vipul");
    }
}