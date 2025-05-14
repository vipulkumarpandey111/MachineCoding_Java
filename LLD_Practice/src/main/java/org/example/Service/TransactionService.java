package org.example.Service;

import org.example.Enums.PaymentMode;
import org.example.Enums.TransactionType;
import org.example.Model.Transaction;
import org.example.Repos.TransactionRepo;
import org.example.Statics.Helper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

public class TransactionService {
    private TransactionRepo transactionRepo;
    private WalletService walletService;

    public TransactionService(TransactionRepo transactionRepo, WalletService walletService) {
        this.transactionRepo = transactionRepo;
        this.walletService = walletService;
    }

    public synchronized void CreateTransaction(BigDecimal amount, PaymentMode mode, Date date, String userFrom, String userTo){
        LocalDateTime currentDateTime = LocalDateTime.now();
        Transaction transactionFrom = new Transaction(amount, TransactionType.Debit, mode, currentDateTime, userFrom, userTo);
        Transaction transactionTo = new Transaction(amount, TransactionType.Credit, mode, currentDateTime, userFrom, userTo);
        transactionRepo.AddTransaction(userFrom, transactionFrom);
        transactionRepo.AddTransaction(userTo, transactionTo);
        walletService.topUpWallet(userFrom, amount);
        walletService.decementWallet(userTo, amount);
    }
}
