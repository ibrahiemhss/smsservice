package com.hijozaty.smsservice;

import android.app.Application;
import android.content.Context;

/**
 * Created by hijozaty team 9/3/2020.
 */
public class AppContext extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        AppContext.context = getApplicationContext();
        initializeSocket();
    }

    public static Context getAppContext() {
        return AppContext.context;
    }

    public void initializeSocket(){
        AppSocketListener.getInstance().initialize();
    }

    public void destroySocketListener(){
        AppSocketListener.getInstance().destroy();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        destroySocketListener();
    }
}
