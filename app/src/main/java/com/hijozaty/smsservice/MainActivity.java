package com.hijozaty.smsservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hijozaty.smsservice.socketioservice.SocketEventConstants;
import com.hijozaty.smsservice.socketioservice.SocketIOService;
import com.hijozaty.smsservice.socketioservice.SocketListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends Activity implements SocketListener {

    private static final int TYPING_TIMER_LENGTH = 600;

    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<Message> mMessages = new ArrayList<Message>();
    private MessageAdapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;
    SocketIOService socketServiceInterface;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceStorage.storeUsername("ibrahim");

      //  AppSocketListener.getInstance().initialize();
     //   AppSocketListener.getInstance().setActiveSocketListener(this);
       // AppSocketListener.getInstance().activeSocketListener.onSocketConnected();
        AppSocketListener.getInstance().setActiveSocketListener(this);
        String room_id = "598_623";
        String user_id = "598";
        String other_id = "598";
        Object[] openRom = {room_id, user_id};

        AppSocketListener.getInstance().restartSocket();
//        AppSocketListener.getInstance().addOnHandler(SocketEventConstants.subscribe,subscribe);
        AppSocketListener.getInstance().emit(SocketEventConstants.subscribe, openRom);

        // Restart Socket.io to avoid weird stuff ;-)
     //   AppSocketListener.getInstance().restartSocket();




        mMessagesView = (RecyclerView) findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MessageAdapter(this, mMessages);
        mMessagesView.setAdapter(mAdapter);
        mInputMessageView = (EditText) findViewById(R.id.message_input);

        mUsername = "ibrahim";
        int numUsers = 1;
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == 0 || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
                // if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                  //  AppSocketListener.getInstance().emit("typing");
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
            }
        });
    }

//===============================================================================================
    private Emitter.Listener subscribe = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });

            int room_id = 598_623;
            int user_id = 598;
            int other_id = 598;
            int[] openRom = {room_id, user_id};
            Log.i("sucess", "connected");

            addLog(getResources().getString(R.string.message_welcome));
            addParticipantsLog(openRom);
        }
    };

//===============================================================================================

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("Failed", "Failed to connect");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

//===============================================================================================

    private Emitter.Listener messageReceived = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Log.d("messageReceved=", "[" + data + "]");
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(username);
                    addMessage(username, message);
                }
            });
        }
    };

//===============================================================================================

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            finish();
            return;
        }
        mUsername = data.getStringExtra("username");
        int numUsers = data.getIntExtra("numUsers", 1);
        addLog(getResources().getString(R.string.message_welcome));
        addParticipantsLog(numUsers);
    }

//===============================================================================================

    private void addLog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessages.add(new Message.Builder(Message.TYPE_LOG)
                        .message(message).build());
                mAdapter.notifyItemInserted(mMessages.size() - 1);

                scrollToBottom();
            }
        });
    }

//===============================================================================================

    private void addParticipantsLog(int... data) {
        addLog(getResources().getQuantityString(R.plurals.message_participants, data[0], data[1]));
    }

//===============================================================================================

    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

//===============================================================================================

    private void addTyping(String username) {
        mMessages.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

//===============================================================================================

    private void removeTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

//===============================================================================================

    private void attemptSend() {
        if (null == mUsername) return;
        mTyping = false;
        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }
        mInputMessageView.setText("");
        addMessage(mUsername, message);
        // perform the sending message attempt.

        String msg = "mesage from android ";
        String roomId = "598_623";
        String senderId = "598";
        Boolean isImage = false;
        String pendingMsgUniqueID = UUID.randomUUID().toString();
        Date date = new Date(); // This object contains the current date value
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String MsgDate = formatter.format(date);
        Object[] sendMessage = {msg, roomId, senderId, isImage, pendingMsgUniqueID, MsgDate};
        AppSocketListener.getInstance().emit(SocketEventConstants.sendMessage, sendMessage);
    }

//===============================================================================================

    private void leave() {
        removeHandlers();
        mUsername = null;
        PreferenceStorage.clearUserSession();
        AppSocketListener.getInstance().signOutUser();
    }

//===============================================================================================

    public void askForLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to logout")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        leave();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


//===============================================================================================

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    public void removeHandlers() {
        AppSocketListener.getInstance().off(Socket.EVENT_CONNECT_ERROR);
        AppSocketListener.getInstance().off(Socket.EVENT_CONNECT_TIMEOUT);
        AppSocketListener.getInstance().off(SocketEventConstants.onnewMessage_not);
        AppSocketListener.getInstance().off(SocketEventConstants.subscribe);
        AppSocketListener.getInstance().off(SocketEventConstants.typing);
    }

//===============================================================================================


    @Override
    public void onResume() {
        super.onResume();
        AppSocketListener.getInstance().setActiveSocketListener(this);

        scrollToBottom();
    }

//===============================================================================================


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

//===============================================================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeHandlers();
        super.onDestroy();
        AppSocketListener.getInstance().setAppConnectedToService(false);
    }

//===============================================================================================

    @Override
    protected void onStart() {
        super.onStart();
        mUsername = "ibrahim";

    }

//===============================================================================================

    @Override
    public void onSocketConnected() {
         String room_id = "598_623";
        String user_id = "623";
        String other_id = "598";
        Object[] openRom = {room_id, user_id};
       //  AppSocketListener.getInstance().emit(SocketEventConstants.addUser, mUsername);

      AppSocketListener.getInstance().addOnHandler(SocketEventConstants.subscribe, subscribe);
      AppSocketListener.getInstance().emit(SocketEventConstants.subscribe, openRom);
      //  AppSocketListener.getInstance().addOnHandler(SocketEventConstants.subscribe, subscribe);
     //  AppSocketListener.getInstance().addOnHandler(SocketEventConstants.messageReceived, messageReceived);
       // AppSocketListener.getInstance().addOnHandler(Socket.EVENT_CONNECT_ERROR, onConnectError);
       // AppSocketListener.getInstance().addOnHandler(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

    }


//===============================================================================================


    @Override
    public void onSocketDisconnected() {

    }
//===============================================================================================

    @Override
    public void onNewMessageReceived(String username, String message) {
        Log.d("message Received","success");

       // removeTyping(username);
        addMessage(username, message);
    }

}
