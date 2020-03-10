package com.hijozaty.smsservice;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hijozaty team 9/3/2020.
 */
public class PreferenceStorage {
    static String preferencesIdentifier = "Socket.io.preferences";

    public static void storeUsername(String username){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("username", username);
        editor.apply();
    }

    public static String getUsername(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userName = prefs.getString("username", null);
        return userName;
    }

    public static Boolean shouldDoAutoLogin(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userName = prefs.getString("username", null);
        if (userName != null && ! userName.isEmpty()){
            return true;
        }
        return false;

    }
    public static void clearUserSession(){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).edit();
        editor.putString("username",null);
        editor.apply();
    }
}
