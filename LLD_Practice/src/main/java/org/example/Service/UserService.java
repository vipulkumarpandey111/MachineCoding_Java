package org.example.Service;

import org.example.Model.User;
import org.example.Repos.UserRepo;

public class UserService {
    private UserRepo userRepo;
    private WalletService walletService;

    public UserService(UserRepo userRepo, WalletService walletService) {
        this.userRepo = userRepo;
        this.walletService = walletService;
    }

    public void RegisterUser(String name){
        User user = new User(name);
        userRepo.regsterUser(name, user);

        System.out.println("user " + name + " is created!");
    }
}
