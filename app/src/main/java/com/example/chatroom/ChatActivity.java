package com.example.chatroom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ChatActivity extends AppCompatActivity {

    private static final String SERVER_IP = "56.228.15.83";
    private static final int SERVER_PORT = 12345;
    private static final String TAG = "ChatActivity";
    private Map<String, View[]> activeGameInvites = new HashMap<>();
    private EditText messageEditText;
    private Button sendButton;
    private TextView chatTextView;
    private String username;
    private LinearLayout chatLinearLayout;
    private Button openGameMenuButton;
    private LinearLayout gameSelectionLayout;
    private Button ticTacToeButton;
    private Button fourInARowButton;
    private boolean isGameMenuVisible = false;
    private ServerConnectionManager connectionManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ScrollView chatScrollView;

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "Received broadcast: " + intent.getAction() + ", message: " + message);

            if ("chat_message".equals(intent.getAction())) {
                if (message.startsWith("NEW_TICTACTOE:")) {
                    showGameInvite(message);
                } else if (message.startsWith("JOIN_TICTACTOE:")) {
                    handleJoinTicTacToe(message);
                } else if (message.startsWith("START_TICTACTOE:")) {
                    handleStartTicTacToe(message);
                } else if (message.startsWith("EXIT_TICTACTOE:")) {
                    handleGameExit(message);  // Handle exit messages
                } else {
                    chatTextView.append(message + "\n");
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ChatActivity onCreate() started on this device.");
        setContentView(R.layout.activity_chat);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatTextView = findViewById(R.id.chatTextView);
        chatLinearLayout = findViewById(R.id.chatLinearLayout);
        Button exitButton = findViewById(R.id.exitButton);
        openGameMenuButton = findViewById(R.id.openGameMenuButton);
        gameSelectionLayout = findViewById(R.id.gameSelectionLayout);
        ticTacToeButton = findViewById(R.id.ticTacToeButton);
        fourInARowButton = findViewById(R.id.fourInARowButton);
        chatScrollView = findViewById(R.id.chatScrollView);

        username = getIntent().getStringExtra("USERNAME");
        Log.d(TAG, "Intent extra USERNAME: " + getIntent().getStringExtra("USERNAME"));
        Log.d(TAG, "Username on this device: " + username);

        connectionManager = ServerConnectionManager.getInstance(username);

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                connectionManager.sendMessage(message);
                messageEditText.setText("");
            }
        });

        exitButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        openGameMenuButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            GameOptionsBottomSheet gameOptionsBottomSheet = new GameOptionsBottomSheet();
            gameOptionsBottomSheet.show(fragmentManager, gameOptionsBottomSheet.getTag());
        });

        ticTacToeButton.setOnClickListener(v -> {
            initiateTicTacToeGame();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("chat_message"));
        Log.d(TAG, "ChatActivity - BroadcastReceiver registered in onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        Log.d(TAG, "ChatActivity - BroadcastReceiver unregistered in onPause()");
    }

    public void initiateTicTacToeGame() {
        String gameId = generateGameId();
        String inviteMessage = "NEW_TICTACTOE:" + gameId + ":" + username;
        connectionManager.sendMessage(inviteMessage);
    }

    private String generateGameId() {
        return String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
    }

    private void showGameInvite(String message) {
        String[] parts = message.split(":");
        if (parts.length != 3) return;

        final String gameId = parts[1];
        final String initiatorUsername = parts[2];

        handler.post(() -> {
            // Remove previous invite if exists
            removeExistingInvite(gameId);

            // Create new invite UI
            TextView inviteText = new TextView(this);
            inviteText.setText(initiatorUsername + " wants to play Tic Tac Toe!");
            inviteText.setTextSize(16);
            inviteText.setPadding(0, 16, 0, 8);

            Button joinButton = new Button(this);
            joinButton.setText("Join Game");
            joinButton.setAllCaps(false);

            // Different button states
            if (initiatorUsername.equals(username)) {
                joinButton.setText("Your Game - Waiting...");
                joinButton.setEnabled(false);
            } else {
                joinButton.setOnClickListener(v -> {
                    connectionManager.sendMessage("JOIN_REQUEST:" + gameId + ":" + initiatorUsername + ":" + username);
                    removeExistingInvite(gameId);
                });
            }

            // Add to layout
            chatLinearLayout.addView(inviteText);
            chatLinearLayout.addView(joinButton);

            // Store for later removal
            activeGameInvites.put(gameId, new View[]{inviteText, joinButton});

            // Scroll to show the invite
            chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void removeExistingInvite(String gameId) {
        View[] views = activeGameInvites.remove(gameId);
        if (views != null) {
            for (View view : views) {
                chatLinearLayout.removeView(view);
            }
        }
    }

    private void handleJoinTicTacToe(String message) {
        String[] parts = message.split(":");
        if (parts.length == 4 && parts[0].equals("JOIN_TICTACTOE")) {
            Log.d(TAG, "Received JOIN_TICTACTOE confirmation: " + message);
            handler.post(() -> {
                Toast.makeText(ChatActivity.this, "You have joined the Tic Tac Toe game!", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.e(TAG, "Invalid JOIN_TICTACTOE message format received: " + message);
        }
    }

    private void handleGameExit(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2) {
            removeExistingInvite(parts[1]);
        }
    }

    private void handleStartTicTacToe(String message) {
        // Format: START_TICTACTOE:gameId:player1Username:player2Username
        String[] parts = message.split(":");
        if (parts.length == 4 && parts[0].equals("START_TICTACTOE")) {
            String gameId = parts[1];
            String player1 = parts[2];
            String player2 = parts[3];

            Intent intent = new Intent(ChatActivity.this, TicTacToeActivity.class);
            intent.putExtra("PLAYER1", player1);
            intent.putExtra("PLAYER2", player2);
            intent.putExtra("GAME_ID", gameId);
            intent.putExtra("USERNAME", username);
            Log.d(TAG, "Starting TicTacToeActivity with intent: " + intent.toString());  // ADD THIS LOG
            startActivity(intent);
        } else {
            Log.e(TAG, "Invalid START_TICTACTOE message format: " + message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ChatActivity onDestroy() - Disconnecting.");
    }
}