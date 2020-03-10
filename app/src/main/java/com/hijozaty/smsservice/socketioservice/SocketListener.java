package com.hijozaty.smsservice.socketioservice;

/**
 * Created by hijozaty team 9/3/2020.
 */
public interface SocketListener {
    void onSocketConnected();
    void onSocketDisconnected();
    //void onNewMessageSend(String room_id,String senderId, String message);
    void onNewMessageReceived(String username, String message);
}
