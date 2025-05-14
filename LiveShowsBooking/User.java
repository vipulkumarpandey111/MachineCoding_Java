import java.util.*;

interface User {
    public static HashMap<String, String> organisers = new HashMap<String,String>();
    public static HashMap<String, String> customers = new HashMap<String,String>();;
    public void AddUserCredentials(String name, String password);
    public void GetUserCredential();
}
