package com.hijozaty.smsservice.socketioservice;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;


import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.hijozaty.smsservice.Constants;
import com.hijozaty.smsservice.MainActivity;
import com.hijozaty.smsservice.PreferenceStorage;
import com.hijozaty.smsservice.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import okhttp3.OkHttpClient;

public class SocketIOService extends Service {
    private  SocketListener socketListener;
    private Boolean appConnectedToService;
    private  Socket mSocket;
    private boolean serviceBinded = false;
    private final LocalBinder mBinder = new LocalBinder();

    public void setAppConnectedToService(Boolean appConnectedToService) {
        this.appConnectedToService = appConnectedToService;
    }

    public void setSocketListener(SocketListener socketListener) {
        this.socketListener = socketListener;
    }

    public class LocalBinder extends Binder{
       public SocketIOService getService(){
            return SocketIOService.this;
        }
    }

    public void setServiceBinded(boolean serviceBinded) {
        this.serviceBinded = serviceBinded;
    }

    @Override
    public IBinder onBind(Intent intent) {
       return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSocket();
        addSocketHandlers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeSocketSession();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return serviceBinded;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void initializeSocket() {

        SSLContext mySSLContext = null;
        try {
            mySSLContext = SSLContext.getInstance("TLS");
            try {
                mySSLContext.init(null, trustAllCerts, null);
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        OkHttpClient okHttpClient = new OkHttpClient.Builder().hostnameVerifier(myHostnameVerifier).sslSocketFactory(mySSLContext.getSocketFactory()).build();

// default settings for all sockets
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
        IO.setDefaultOkHttpCallFactory(okHttpClient);

// set as an option
        IO.Options opts = new IO.Options();
        opts.callFactory = okHttpClient;
        opts.webSocketFactory = okHttpClient;

            try {


                mSocket = IO.socket(Constants.SERVER_URL, opts);
            } catch (URISyntaxException e) {
                Log.e("Error", "Exception in socket creation" + e.toString());

                throw new RuntimeException(e);
            }


    }

    private void closeSocketSession(){
        mSocket.disconnect();
        mSocket.off();
    }
    private void addSocketHandlers(){

        mSocket.io().on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport) args[0];
                transport.on(Transport.EVENT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Exception e = (Exception) args[0];
                        Log.e("Socket_evevnt", "Transport error " + e);
                        e.printStackTrace();
                        e.getCause().printStackTrace();
                    }
                });
            }
        });
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("Socket_evevnt", "EVENT_CONNECT ["+args[0].toString()+"]");

                Intent intent = new Intent(SocketEventConstants.socketConnection);
                intent.putExtra(Constants.EXTR_CONNECTION_STATUS, true);
                broadcastEvent(intent);
            }
        });

        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
              //  Exception e = (Exception) args[0];
                Log.e("Socket_evevnt", "EVENT_DISCONNECT ["+args[0].toString()+"]");

                Intent intent = new Intent(SocketEventConstants.socketConnection);
                intent.putExtra(Constants.EXTR_CONNECTION_STATUS, false);
                broadcastEvent(intent);
            }
        });


        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("Socket_evevnt", "EVENT_CONNECT_ERROR ["+args[0].toString()+"]");

                Intent intent = new Intent(SocketEventConstants.connectionFailure);
                broadcastEvent(intent);
            }
        });

        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("Socket_evevnt", "EVENT_CONNECT_TIMEOUT ["+args[0].toString()+"]");

                Intent intent = new Intent(SocketEventConstants.connectionFailure);
                broadcastEvent(intent);
            }
        });
        if (PreferenceStorage.shouldDoAutoLogin()) {
            addNewMessageHandler();
        }
        mSocket.connect();
    }

    public void addNewMessageHandler(){
        mSocket.off(SocketEventConstants.messageReceived);
        mSocket.on(SocketEventConstants.messageReceived, new Emitter.Listener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String username;
                String message;
                try {
                    username = data.getString("username");
                    message = data.getString("message");
                } catch (JSONException e) {
                    return;
                }

                if (isForeground("com.hijozaty.smsservice.socketiochat")) {
                    Intent intent = new Intent(SocketEventConstants.messageReceived);
                    intent.putExtra("username", username);
                    intent.putExtra("message", message);
                    broadcastEvent(intent);
                } else {
                    showNotificaitons(username, message);
                }
            }
        });
    }

    public void removeMessageHandler() {
        mSocket.off(SocketEventConstants.messageReceived);
    }

    public void emit(String event,Object[] args,Ack ack){
        mSocket.emit(event, args, ack);
    }
    public void emit (String event,Object... args) {
        try {
            mSocket.emit(event, args,null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addOnHandler(String event,Emitter.Listener listener){
            mSocket.on(event, listener);
    }

    public void connect(){
        mSocket.connect();
    }

    public void disconnect(){
        mSocket.disconnect();
    }

    public void restartSocket(){
        mSocket.off();
        mSocket.disconnect();
        addSocketHandlers();
    }

    public void off(String event){
        mSocket.off(event);
    }

    private void broadcastEvent(Intent intent){
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public boolean isSocketConnected(){
        if (mSocket == null){
            return false;
        }
        return mSocket.connected();
    }

    public void showNotificaitons(String username, String message){
        Intent toLaunch = new Intent(getApplicationContext(), MainActivity.class);
        toLaunch.putExtra("username",message);
        toLaunch.putExtra("message",message);
        toLaunch.setAction("android.intent.action.MAIN");
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0,toLaunch,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n  = new NotificationCompat.Builder(this)
                .setContentTitle("You have pending new messages")
                .setContentText("New Message")
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher_hdp)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        notificationManager.notify(0, n);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean isForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }



    HostnameVerifier myHostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    TrustManager[] trustAllCerts= new TrustManager[] { new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }};

}
