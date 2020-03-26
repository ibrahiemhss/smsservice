package com.hijozaty.smsservice;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity {

    private Socket mSocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new MainThread()).start();

    }


    class MainThread implements Runnable {

        @Override
        public void run() {

            try {

                mSocket = new Socket("95.216.223.177", 3001);

                new Thread(new GetThread()).start();
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    class GetThread implements Runnable {

        @Override
        public void run() {

            try {
                InputStreamReader isR = new InputStreamReader(mSocket.getInputStream());
                BufferedReader bfr = new BufferedReader(isR);
                while (true) {
                    String textMessage = bfr.readLine();
                    Log.d("getSocketValue", textMessage);

                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }


}
