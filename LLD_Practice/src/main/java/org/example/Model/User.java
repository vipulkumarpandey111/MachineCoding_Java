package org.example.Model;

import lombok.*;
import org.example.Statics.Helper;

import java.util.*;

@Getter
public class User {
    int id;
    String name;

    public User(String name){
        this.id = Helper.generateId();
        this.name = name;
    }
}
