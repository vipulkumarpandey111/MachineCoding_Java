package org.example.Repos;

import org.example.Enums.TransactionSort;
import org.example.Enums.TransactionType;
import org.example.Model.Transaction;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.HashMap;

public class TransactionRepo {
    static HashMap<String, Transaction> transactions;

    public TransactionRepo() {
        transactions = new HashMap<>();
    }

    public synchronized void AddTransaction(String user, Transaction trn){
        transactions.put(user, trn);
    }

    public void getTransactions(String user, TransactionType type, TransactionSort sort) {
        List<Transaction> filteredSortedList = transactions.values().stream()
                .filter(t -> user == null || t.getUserFrom().equals(user))
                .filter(t -> type == null || t.getType() == type)
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());

        filteredSortedList.forEach(t ->
                System.out.println(t.getId() + " | " + t.getDate() + " | " + t.getAmount())
        );
    }

    public void getTransactions() {
        getTransactions(null, null, null);
    }

    public void getTransactions(String user) {
        getTransactions(user, null, null);
    }

    public void getTransactions(String user, TransactionType type) {
        getTransactions(user, type, null);
    }

}
