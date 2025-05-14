public class Customer implements User{
    public String name;
    public String password;

    //This class simply implements user interface as a parent interface for organizers and customers.

    public void AddUserCredentials(String name, String password){
        this.name = name;
        this.password = password;
        User.customers.put(name, password);
    }

    public void GetUserCredential(){
        System.out.println(this.name);
        System.out.println(this.password);
    }
}
