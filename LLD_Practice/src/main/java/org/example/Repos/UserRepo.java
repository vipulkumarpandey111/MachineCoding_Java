package org.example.Repos;

import org.example.Model.User;

import java.util.HashMap;

public class UserRepo {
    static HashMap<String, User> users;

    public UserRepo() {
        users = new HashMap<>();
    }

    public synchronized void regsterUser(String key, User user){
        users.put(key, user);
    }
}
