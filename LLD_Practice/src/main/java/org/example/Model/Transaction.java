package org.example.Model;

import lombok.*;
import org.example.Enums.PaymentMode;
import org.example.Enums.TransactionType;
import org.example.Statics.Helper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
public class Transaction {
    int id;
    BigDecimal amount;
    TransactionType type;
    PaymentMode mode;
    LocalDateTime date;
    String userFrom;
    String userTo;

    public Transaction(BigDecimal amount, TransactionType type, PaymentMode mode, LocalDateTime date, String userFrom, String userTo) {
        this.id = Helper.generateId();
        this.amount = amount;
        this.type = type;
        this.mode = mode;
        this.date = date;
        this.userFrom = userFrom;
        this.userTo = userTo;
    }
}
