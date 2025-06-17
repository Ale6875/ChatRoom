package com.example.chatroom;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ServerConnectionManager {

    private static final String SERVER_IP = "51.21.214.199";
    private static final int SERVER_PORT = 12345;
    private static final String TAG = "ServerConnectionManager";

    private static ServerConnectionManager instance;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;
    private Handler handler;
    private boolean isConnected = false;
    private Thread messageListenerThread;
    List<MessageListener> messageListeners = new ArrayList<>();

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    private ServerConnectionManager(String username) {
        this.username = username;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static ServerConnectionManager getInstance(String username) {
        if (instance == null || !instance.username.equals(username)) {
            instance = new ServerConnectionManager(username);
        }
        return instance;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connectAndListen() {
        if (isConnected) {
            return;
        }
        new Thread(() -> {
            try {
                Log.d(TAG, "Connecting to server...");
                socket = new Socket(SERVER_IP, SERVER_PORT);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                if (socket.isConnected() && writer != null && reader != null) {
                    writer.println(username);
                    Log.d(TAG, "Sent username: " + username);
                    isConnected = true;
                    Log.d(TAG, "Connected to server!");
                    startListeningForMessages();
                } else {
                    Log.e(TAG, "Failed to establish connection to server.");
                    isConnected = false;
                    closeConnection();
                }

            } catch (IOException e) {
                Log.e(TAG, "Error connecting to server: " + e.getMessage(), e);
                isConnected = false;
                closeConnection();
            }
        }).start();
    }

    private void startListeningForMessages() {
        messageListenerThread = new Thread(() -> {
            try {
                String message;
                while (isConnected && (message = reader.readLine()) != null) {
                    Log.d(TAG, "Received message: " + message);
                    notifyMessageListeners(message);
                }
                Log.d(TAG, "Disconnected from server.  Stopping message listener.");
            } catch (SocketException e) {
                Log.e(TAG, "SocketException while listening for messages: " + e.getMessage(), e);
                handleDisconnect();
            } catch (IOException e) {
                if (isConnected) {
                    Log.e(TAG, "Error reading from server: " + e.getMessage(), e);
                    handleDisconnect();
                }

            } finally {
                disconnect();
            }
        });
        messageListenerThread.start();
    }

    private void handleDisconnect() {
        if (isConnected) {
            isConnected = false;
            Log.d(TAG, "Disconnected from server.");
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (isConnected && writer != null) {
            new Thread(() -> {
                writer.println(message);
                Log.d(TAG, "Sent message: " + message);
            }).start();
        } else {
            Log.w(TAG, "Not connected.  Message not sent: " + message);
        }
    }

    public void disconnect() {
        if (isConnected) {
            isConnected = false;
            try {
                if (writer != null) {
                    writer.println("DISCONNECT:" + username);
                    writer.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending disconnect message", e);
            }
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing connection: " + e.getMessage(), e);
        } finally {
            writer = null;
            reader = null;
            socket = null;
        }
    }

    public void addMessageListener(MessageListener listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
            Log.d(TAG, "Added MessageListener: " + listener.getClass().getSimpleName() +
                    ". Total listeners: " + messageListeners.size());
        } else {
            Log.w(TAG, "Attempted to add duplicate MessageListener: " +
                    listener.getClass().getSimpleName());
        }
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
        Log.d(TAG, "Removed MessageListener: " + listener.getClass().getSimpleName() +
                ". Total listeners: " + messageListeners.size());
    }

    private void notifyMessageListeners(String message) {
        Log.d(TAG, "notifyMessageListeners: Notifying " + messageListeners.size() + " listeners for message: " + message);
        for (MessageListener listener : messageListeners) {
            listener.onMessageReceived(message);
        }
    }
}