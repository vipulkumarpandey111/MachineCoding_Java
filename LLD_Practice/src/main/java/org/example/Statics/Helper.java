package org.example.Statics;

public class Helper {
    private static int id = 0;

    public static int generateId(){
        synchronized (Helper.class){
            id++;
            return id;
        }
    }
}
