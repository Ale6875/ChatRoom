package com.example.chatroom;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MyChatService extends Service implements ServerConnectionManager.MessageListener {

    private static final String TAG = "MyChatService";
    private ServerConnectionManager connectionManager;
    private String username;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        if (intent != null) {
            username = intent.getStringExtra("USERNAME");
            Log.d(TAG, "Service received username: " + username);
            if (username != null) {
                connectionManager = ServerConnectionManager.getInstance(username);
                if (connectionManager.messageListeners.isEmpty()) {
                    connectionManager.addMessageListener(this);
                    Log.d(TAG, "onStartCommand: Added listener (MyChatService). Total Listeners: " + connectionManager.messageListeners.size());
                } else {
                    Log.w(TAG, "onStartCommand: Listener (MyChatService) already registered. Count: " + connectionManager.messageListeners.size());
                }
                if (!connectionManager.isConnected()) {
                    connectionManager.connectAndListen();
                }
            } else {
                Log.w(TAG, "No username provided to the service.");
                stopSelf();
            }
        } else {
            Log.w(TAG, "Service started with a null intent.");
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        if (connectionManager != null) {
            connectionManager.disconnect();
            connectionManager.removeMessageListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onMessageReceived(String message) {
        Log.d(TAG, "MyChatService - Received message: " + message);

        if (message.startsWith("MOVE:") || message.startsWith("GAME_OVER:")) {
            Intent gameIntent;
            gameIntent = new Intent("tictactoe_move");
            gameIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(gameIntent);
        }
        else if (message.startsWith("CHAT:")) {
            Log.d(TAG, "MyChatService - Handling CHAT message: " + message);
            Intent chatIntent = new Intent("chat_message");
            chatIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(chatIntent);
        }
        else if (message.startsWith("JOINED:")) {
            Log.d(TAG, "MyChatService - Handling JOINED message: " + message);
            Intent joinedIntent = new Intent("chat_message");
            joinedIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(joinedIntent);
        }
        else if (message.startsWith("NEW_TICTACTOE:") || message.startsWith("NEW_FOURINAROW:")) {
            Log.d(TAG, "MyChatService - Handling game invite message: " + message);
            Intent gameInviteIntent = new Intent("chat_message");
            gameInviteIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(gameInviteIntent);
        }
        else if (message.startsWith("JOIN_TICTACTOE:") || message.startsWith("JOIN_FOURINAROW:")) {
            Log.d(TAG, "MyChatService - Handling game join message: " + message);
            Intent gameJoinIntent = new Intent("chat_message");
            gameJoinIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(gameJoinIntent);
        }
        else if (message.startsWith("START_TICTACTOE:") || message.startsWith("START_FOURINAROW:")) {
            Log.d(TAG, "MyChatService - Handling game start message: " + message);
            Intent gameStartIntent = new Intent("chat_message");
            gameStartIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(gameStartIntent);
        }
        else if (message.startsWith("EXIT_TICTACTOE:") || message.startsWith("EXIT_FOURINAROW:")) {
            Log.d(TAG, "MyChatService - Handling game exit message: " + message);
            Intent gameExitIntent = new Intent("chat_message");
            gameExitIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(gameExitIntent);
        }
        else {
            Log.d(TAG, "MyChatService - Sending chat message to activity: " + message);
            Intent chatIntent = new Intent("chat_message");
            chatIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(chatIntent);
        }
    }
}