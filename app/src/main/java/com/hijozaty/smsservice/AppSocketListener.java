package com.hijozaty.smsservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.hijozaty.smsservice.socketioservice.SocketEventConstants;
import com.hijozaty.smsservice.socketioservice.SocketIOService;
import com.hijozaty.smsservice.socketioservice.SocketListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import io.socket.client.Ack;
import io.socket.emitter.Emitter;

/**
 * Created by hijozaty team 9/3/2020.
 */
public class AppSocketListener implements SocketListener {
    private static AppSocketListener sharedInstance;
    private SocketIOService socketServiceInterface;
    public SocketListener activeSocketListener;

    public void setActiveSocketListener(SocketListener activeSocketListener) {
        this.activeSocketListener = activeSocketListener;
        if (socketServiceInterface != null && socketServiceInterface.isSocketConnected()){
            onSocketConnected();
        }
    }


    public static AppSocketListener getInstance(){
        if (sharedInstance==null){
            sharedInstance = new AppSocketListener();
        }
        return sharedInstance;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try{
                socketServiceInterface = ((SocketIOService.LocalBinder)service).getService();
                socketServiceInterface.setServiceBinded(true);
                socketServiceInterface.setSocketListener(sharedInstance);
                if (socketServiceInterface.isSocketConnected()){
                    onSocketConnected();
                }
            }catch (Exception e){
                Log.i("Connection Exception",e.toString());

            }


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            socketServiceInterface.setServiceBinded(false);
            socketServiceInterface=null;
            onSocketDisconnected();
        }
    };


    public void initialize(){
        Intent intent = new Intent(AppContext.getAppContext(), SocketIOService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppContext.getAppContext().startForegroundService(intent);
        } else {
            AppContext.getAppContext().startService(intent);
        }
        AppContext.getAppContext().startService(intent);
        AppContext.getAppContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(socketConnectionReceiver, new IntentFilter(SocketEventConstants.
                        socketConnection));
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(connectionFailureReceiver, new IntentFilter(SocketEventConstants.
                        connectionFailure));
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newMessageReceiver, new IntentFilter(SocketEventConstants.
                        messageReceived));
    }

    private BroadcastReceiver socketConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = intent.getBooleanExtra(Constants.EXTR_CONNECTION_STATUS,false);
            if (connected){
                Log.i("BroadcastReceiver","Socket connected");
                onSocketConnected();
            }
            else{
                onSocketDisconnected();
            }
        }
    };

    private BroadcastReceiver connectionFailureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.
                    makeText(AppContext.getAppContext(), "Please check your network connection",
                            Toast.LENGTH_SHORT);
            toast.show();
        }
    };

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String message = intent.getStringExtra("message");
            onNewMessageReceived(userName,message);
        }
    };

    public void destroy(){
        socketServiceInterface.setServiceBinded(false);
        AppContext.getAppContext().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(socketConnectionReceiver);
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newMessageReceiver);
    }

    @Override
    public void onSocketConnected() {
        String room_id = "598_623";
        String user_id = "623";
        String other_id = "598";
        Object[] openRom = {room_id, user_id};


        if (activeSocketListener != null) {
           // socketServiceInterface.emit(SocketEventConstants.subscribe, openRom);
            activeSocketListener.onSocketConnected();
        }
    }

    @Override
    public void onSocketDisconnected() {
        if (activeSocketListener != null) {
            activeSocketListener.onSocketDisconnected();
        }
    }

    public void sendMessage(String room_id,String senderId, String message) {
        if (activeSocketListener != null) {
            String msg= "mesage from android ";
            String roomId= "598_623";
             senderId="598";
            Boolean isImage=false;
            String pendingMsgUniqueID = UUID.randomUUID().toString();
            Date date = new Date(); // This object contains the current date value
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String MsgDate= formatter.format(date);
            Object[] sendMessage = {msg, roomId,senderId,isImage,pendingMsgUniqueID,MsgDate};
            try{
                socketServiceInterface.emit(SocketEventConstants.sendMessage, sendMessage);

            }catch (Exception e){
                Log.d("Exception send",e.toString());
            }
        }
    }

    @Override
    public void onNewMessageReceived(String username, String message) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewMessageReceived(username, message);
        }
    }

    public void addOnHandler(String event,Emitter.Listener listener){
       // if (socketServiceInterface != null) {
            socketServiceInterface.addOnHandler(event, listener);

      //  }

    }
    public void emit(String event,Object[] args,Ack ack){
        if (socketServiceInterface != null) {
            socketServiceInterface.emit(event, args, ack);

        }
    }

    public void emit (String event,Object... args){
        if (socketServiceInterface != null) {
            socketServiceInterface.emit(event, args);

        }
    }

    void connect(){
        socketServiceInterface.connect();
    }

    public void disconnect(){
        socketServiceInterface.disconnect();
    }
    public void off(String event) {
        if (socketServiceInterface != null) {
            socketServiceInterface.off(event);
        }
    }

    public boolean isSocketConnected(){
        if (socketServiceInterface == null){
            return false;
        }
        return socketServiceInterface.isSocketConnected();
    }

    public void setAppConnectedToService(Boolean status){
        if ( socketServiceInterface != null){
            socketServiceInterface.setAppConnectedToService(status);
        }
    }

    public void restartSocket(){
        if (socketServiceInterface != null){
            socketServiceInterface.restartSocket();
        }
    }
    public void addNewMessageHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewMessageHandler();
        }
    }

    public void removeNewMessageHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeMessageHandler();
        }
    }

    public void signOutUser(){
        AppSocketListener.getInstance().disconnect();
        removeNewMessageHandler();
        AppSocketListener.getInstance().connect();
    }
}
