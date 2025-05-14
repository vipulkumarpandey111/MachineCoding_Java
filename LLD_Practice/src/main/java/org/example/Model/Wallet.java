package org.example.Model;

import lombok.*;
import org.example.Statics.Helper;

import java.math.BigDecimal;

@Getter
public class Wallet {
    int id;
    String user;
    BigDecimal amount;

    public Wallet(String user) {
        this.id = Helper.generateId();
        this.user = user;
        this.amount = new BigDecimal(0);
    }

    public synchronized void incrementAmt(BigDecimal amt){
        this.amount.add(amt);
    }

    public synchronized void decrementAmt(BigDecimal amt){
        this.amount.subtract(amt);
    }
}
