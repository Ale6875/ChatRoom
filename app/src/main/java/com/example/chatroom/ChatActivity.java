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

    private static final String TAG = "ChatActivity";
    private Map<String, View[]> activeGameInvites = new HashMap<>();
    private EditText messageEditText;
    private Button sendButton;
    private TextView chatTextView;
    private String username;
    private LinearLayout chatLinearLayout;
    private Button openGameMenuButton;
    private ScrollView chatScrollView;
    private ServerConnectionManager connectionManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "Received broadcast: " + intent.getAction() + ", message: " + message);

            if ("chat_message".equals(intent.getAction())) {
                if (message.startsWith("NEW_TICTACTOE:")) {
                    showGameInvite(message, "Tic Tac Toe");
                } else if (message.startsWith("NEW_FOURINAROW:")) {
                    showGameInvite(message, "Four in a Row");
                } else if (message.startsWith("JOIN_TICTACTOE:")) {
                    handleJoinGame(message, "Tic Tac Toe");
                } else if (message.startsWith("JOIN_FOURINAROW:")) {
                    handleJoinGame(message, "Four in a Row");
                } else if (message.startsWith("START_TICTACTOE:")) {
                    handleStartGame(message, TicTacToeActivity.class);
                } else if (message.startsWith("START_FOURINAROW:")) {
                    handleStartGame(message, FourInARowActivity.class);
                } else if (message.startsWith("EXIT_TICTACTOE:") || message.startsWith("EXIT_FOURINAROW:")) {
                    handleGameExit(message);
                } else {
                    chatTextView.append(message + "\n");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatTextView = findViewById(R.id.chatTextView);
        chatLinearLayout = findViewById(R.id.chatLinearLayout);
        Button exitButton = findViewById(R.id.exitButton);
        openGameMenuButton = findViewById(R.id.openGameMenuButton);
        chatScrollView = findViewById(R.id.chatScrollView);

        username = getIntent().getStringExtra("USERNAME");
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
    }

    public void initiateTicTacToeGame() {
        String gameId = generateGameId();
        String inviteMessage = "NEW_TICTACTOE:" + gameId + ":" + username;
        connectionManager.sendMessage(inviteMessage);
    }

    public void initiateFourInARowGame() {
        String gameId = generateGameId();
        String inviteMessage = "NEW_FOURINAROW:" + gameId + ":" + username;
        connectionManager.sendMessage(inviteMessage);
    }

    private String generateGameId() {
        return String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
    }

    private void showGameInvite(String message, String gameType) {
        String[] parts = message.split(":");
        if (parts.length != 3) return;

        final String gameId = parts[1];
        final String initiatorUsername = parts[2];

        handler.post(() -> {
            removeExistingInvite(gameId);

            TextView inviteText = new TextView(this);
            inviteText.setText(initiatorUsername + " wants to play " + gameType + "!");
            inviteText.setTextSize(16);
            inviteText.setPadding(0, 16, 0, 8);

            Button joinButton = new Button(this);
            joinButton.setText("Join Game");
            joinButton.setAllCaps(false);

            if (initiatorUsername.equals(username)) {
                joinButton.setText("Your Game - Waiting...");
                joinButton.setEnabled(false);
            } else {
                joinButton.setOnClickListener(v -> {
                    String joinMessage = "JOIN_REQUEST:" +
                            (gameType.equals("Tic Tac Toe") ? "TICTACTOE" : "FOURINAROW") +
                            ":" + gameId + ":" + initiatorUsername + ":" + username;
                    connectionManager.sendMessage(joinMessage);
                    removeExistingInvite(gameId);
                });
            }

            chatLinearLayout.addView(inviteText);
            chatLinearLayout.addView(joinButton);
            activeGameInvites.put(gameId, new View[]{inviteText, joinButton});
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

    private void handleJoinGame(String message, String gameType) {
        String[] parts = message.split(":");
        if (parts.length == 4) {
            handler.post(() -> {
                Toast.makeText(ChatActivity.this,
                        "You have joined the " + gameType + " game!",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void handleStartGame(String message, Class<?> gameActivityClass) {
        String[] parts = message.split(":");
        if (parts.length == 4) {
            String gameId = parts[1];
            String player1 = parts[2];
            String player2 = parts[3];

            Intent intent = new Intent(ChatActivity.this, gameActivityClass);
            intent.putExtra("PLAYER1", player1);
            intent.putExtra("PLAYER2", player2);
            intent.putExtra("GAME_ID", gameId);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        }
    }

    private void handleGameExit(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2) {
            removeExistingInvite(parts[1]);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("chat_message"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}