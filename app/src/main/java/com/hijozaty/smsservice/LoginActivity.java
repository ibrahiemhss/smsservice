package com.hijozaty.smsservice;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hijozaty.smsservice.socketioservice.SocketEventConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


/**
 * A login screen that offers login via username.
 */
public class LoginActivity extends Activity {

    private EditText mUsernameView;

    private String mUsername;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up te1he login form.
        mUsernameView = (EditText) findViewById(R.id.username_input);
        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    try {
                        attemptLogin();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    attemptLogin();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() throws RemoteException {
        // Reset errors.
        mUsernameView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            return;
        }

        mUsername = username;
        PreferenceStorage.storeUsername(username);
        // perform the user login attempt.
        ArrayList<String> stringArrayList = new ArrayList<String>();
        stringArrayList.add(username);
        //AppSocketListener.getInstance().emit("add user",stringArrayList,null);
        //AppSocketListener.getInstance().emit("add user",stringArrayList,null);
        String room_id= "598_623";
        String user_id="598";
        String other_id="598";
        String[] openRom = {room_id, user_id};
        // _socket.emit("subscribe", [_roomId, userId]);
        // _socket.emit('getAllMessages', [_roomId, 100, 1]);




        AppSocketListener.getInstance().activeSocketListener.onSocketConnected();
      //  AppSocketListener.getInstance().emit(SocketEventConstants.subscribe, openRom);
      //  AppSocketListener.getInstance().emit(SocketEventConstants.getAllMessages, user_id);
    }

    private Emitter.Listener subscribe = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            JSONObject data = (JSONObject) args[0];

            int numMessages;
            try {
                numMessages = data.getInt("fetchMessages");
            } catch (JSONException e) {
                return;
            }

            Intent intent = new Intent();
            intent.putExtra("username", mUsername);
            intent.putExtra("numMessages", numMessages);
            setResult(RESULT_OK, intent);
            finish();
        }
    };
}



