import java.util.*;

public class Organiser implements User {
    public String name;
    public String password;

    public void AddUserCredentials(String name, String password){
        this.name = name;
        this.password = password;
        User.organisers.put(name, password);
    }

    public void GetUserCredential(){
        System.out.println(this.name);
        System.out.println(this.password);
    }
}
