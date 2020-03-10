package com.hijozaty.smsservice;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hijozaty.smsservice.socketioservice.SocketIOService;


/**
 * Created by hijozaty team 9/3/2020.
 */
public class BootReceiver extends BroadcastReceiver {


    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
       // if (intent != null) {
            Intent serviceIntent = new Intent(SocketIOService.class.getName());
            context.startService(serviceIntent);
      //  }


    }
}
