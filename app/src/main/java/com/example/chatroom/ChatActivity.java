package com.example.chatroom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String STATS_URL = "http://51.21.214.199/get_stats.php";
    private static final String SCOREBOARD_URL = "http://51.21.214.199/get_scoreboard.php";

    private Map<String, View[]> activeGameInvites = new HashMap<>();
    private EditText messageEditText;
    private Button sendButton;
    private TextView chatTextView;
    private String username;
    private LinearLayout chatLinearLayout;
    private Button openGameMenuButton;
    private LinearLayout gameSelectionLayout;
    private ServerConnectionManager connectionManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ScrollView chatScrollView;
    private Button statsButton;
    private Button scoreboardButton;
    private RequestQueue requestQueue;

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
                } else if (message.startsWith("EXIT_TICTACTOE:") || (message.startsWith("EXIT_FOURINAROW"))) {
                    handleGameExit(message);
                } if (message.startsWith("NEW_FOURINAROW:")) {
                    showFourInARowInvite(message);
                } else if (message.startsWith("JOIN_FOURINAROW:")) {
                    handleJoinFourInARow(message);
                } else if (message.startsWith("START_FOURINAROW:")) {
                    handleStartFourInARow(message);
                } else if (!message.startsWith("EXIT_TICTACTOE:") &&
                        !message.startsWith("EXIT_FOURINAROW:") &&
                        !message.startsWith("RESET_GAME_REQUEST:") &&
                        !message.startsWith("RESET_GAME_CONFIRMED:") &&
                        !message.startsWith("MOVE:") &&
                        !message.startsWith("GAME_OVER:") &&
                        !message.startsWith("MOVE4:") &&
                        !message.startsWith("GAME_OVER4:") &&
                        !message.startsWith("NEW_TICTACTOE:") &&
                        !message.startsWith("JOIN_TICTACTOE:") &&
                        !message.startsWith("START_TICTACTOE:") &&
                        !message.startsWith("NEW_FOURINAROW:") &&
                        !message.startsWith("JOIN_FOURINAROW:") &&
                        !message.startsWith("START_FOURINAROW:")) {

                    addChatMessage(message);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ChatActivity onCreate() started on this device.");
        setContentView(R.layout.activity_chat);

        // Initialize views
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatTextView = findViewById(R.id.chatTextView);
        chatLinearLayout = findViewById(R.id.chatLinearLayout);
        openGameMenuButton = findViewById(R.id.openGameMenuButton);
        gameSelectionLayout = findViewById(R.id.gameSelectionLayout);
        chatScrollView = findViewById(R.id.chatScrollView);
        statsButton = findViewById(R.id.statsButton);
        scoreboardButton = findViewById(R.id.scoreboardButton);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Get username from intent
        username = getIntent().getStringExtra("USERNAME");
        Log.d(TAG, "Intent extra USERNAME: " + getIntent().getStringExtra("USERNAME"));
        Log.d(TAG, "Username on this device: " + username);

        // Initialize connection manager
        connectionManager = ServerConnectionManager.getInstance(username);

        // Set up button click listeners
        setupButtonListeners();

    }

    private void setupButtonListeners() {
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                connectionManager.sendMessage(message);
                messageEditText.setText("");
            }
        });

        openGameMenuButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            GameOptionsBottomSheet gameOptionsBottomSheet = new GameOptionsBottomSheet();
            gameOptionsBottomSheet.show(fragmentManager, gameOptionsBottomSheet.getTag());
        });

        statsButton.setOnClickListener(v -> fetchUserStats());
        scoreboardButton.setOnClickListener(v -> fetchScoreboard());

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            connectionManager.sendMessage("DISCONNECT:" + username);
            Intent serviceIntent = new Intent(ChatActivity.this, MyChatService.class);
            stopService(serviceIntent);
            connectionManager.disconnect();
            Intent intent = new Intent(ChatActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void fetchUserStats() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, STATS_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject stats = jsonResponse.getJSONObject("stats");
                            showStatsDialog(stats);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing stats response", e);
                        Toast.makeText(ChatActivity.this, "Error loading stats", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching stats", error);
                    Toast.makeText(ChatActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void fetchScoreboard() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, SCOREBOARD_URL,
                response -> {
                    Log.d(TAG, "Scoreboard response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            if (jsonResponse.has("scoreboard")) {
                                JSONArray scoreboard = jsonResponse.getJSONArray("scoreboard");
                                showScoreboardDialog(scoreboard);
                            } else {
                                Toast.makeText(ChatActivity.this, "No scoreboard data available", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            String error = jsonResponse.optString("message", "Unknown error");
                            Toast.makeText(ChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(ChatActivity.this, "Error parsing scoreboard data", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error: " + error.getMessage());
                    Toast.makeText(ChatActivity.this,
                            "Network error: " + (error.getMessage() != null ? error.getMessage() : "Check connection"),
                            Toast.LENGTH_LONG).show();
                });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);
    }

    private void showStatsDialog(JSONObject stats) {
        try {
            StringBuilder statsText = new StringBuilder();
            statsText.append("Tic Tac Toe:\n");
            statsText.append("Wins: ").append(stats.getInt("tictactoe_wins")).append("\n");
            statsText.append("Losses: ").append(stats.getInt("tictactoe_losses")).append("\n");
            statsText.append("Draws: ").append(stats.getInt("tictactoe_draws")).append("\n\n");
            statsText.append("Four in a Row:\n");
            statsText.append("Wins: ").append(stats.getInt("fourinarow_wins")).append("\n");
            statsText.append("Losses: ").append(stats.getInt("fourinarow_losses")).append("\n");
            statsText.append("Draws: ").append(stats.getInt("fourinarow_draws"));

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Your Game Stats")
                    .setMessage(statsText.toString())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (JSONException e) {
            Log.e(TAG, "Error processing stats data", e);
        }
    }

    private void showScoreboardDialog(JSONArray scoreboard) {
        try {
            StringBuilder scoreboardText = new StringBuilder();
            for (int i = 0; i < Math.min(scoreboard.length(), 5); i++) {
                JSONObject player = scoreboard.getJSONObject(i);
                String username = player.optString("up_ime", "Unknown");
                int wins = player.optInt("total_wins", 0);

                scoreboardText.append(i + 1).append(". ")
                        .append(username).append(" - ")
                        .append("Wins: ").append(wins).append("\n");
            }

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Top Players")
                    .setMessage(scoreboardText.toString())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (JSONException e) {
            Log.e(TAG, "Error processing scoreboard data", e);
            Toast.makeText(this, "Error displaying scoreboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void addChatMessage(String message) {
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextSize(16);
        TypedValue typedValue = new TypedValue();
        TypedArray a = obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary});
        int color = a.getColor(0, 0xFF000000);
        a.recycle();

        messageView.setTextColor(color);
        messageView.setPadding(0, 4, 0, 4);

        chatLinearLayout.addView(messageView);
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
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
            removeExistingInvite(gameId);

            TextView inviteText = new TextView(this);
            inviteText.setText(initiatorUsername + " wants to play Tic Tac Toe!");
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
                    connectionManager.sendMessage("JOIN_REQUEST:" + gameId + ":" + initiatorUsername + ":" + username);
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
            String gameId = parts[1];

            handler.post(() -> {
                View[] views = activeGameInvites.get(gameId);
                if (views != null && views.length == 2 && views[1] instanceof Button) {
                    Button gameButton = (Button) views[1];
                    gameButton.setEnabled(false);
                    gameButton.setText("This game was closed.");
                }
            });
        }
    }

    private void handleStartTicTacToe(String message) {
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
            startActivity(intent);

            removeExistingInvite(gameId);

            handler.post(() -> {
                TextView infoText = new TextView(this);
                infoText.setText("Tic Tac Toe: " + player1 + " vs " + player2);
                infoText.setTextSize(16);
                infoText.setPadding(0, 16, 0, 8);

                Button gameStatusButton = new Button(this);
                gameStatusButton.setText("Game in Progress");
                gameStatusButton.setAllCaps(false);
                gameStatusButton.setEnabled(false);

                chatLinearLayout.addView(infoText);
                chatLinearLayout.addView(gameStatusButton);

                activeGameInvites.put(gameId, new View[]{infoText, gameStatusButton});
                chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
            });
        } else {
            Log.e(TAG, "Invalid START_TICTACTOE message format: " + message);
        }
    }

    // FOUR IN A ROW
    public void initiateFourInARowGame() {
        String gameId = generateGameId();
        String inviteMessage = "NEW_FOURINAROW:" + gameId + ":" + username;
        connectionManager.sendMessage(inviteMessage);
    }

    private void showFourInARowInvite(String message) {
        String[] parts = message.split(":");
        if (parts.length != 3) return;

        final String gameId = parts[1];
        final String initiatorUsername = parts[2];

        handler.post(() -> {
            removeExistingInvite(gameId);

            TextView inviteText = new TextView(this);
            inviteText.setText(initiatorUsername + " wants to play Four in a Row!");
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
                    connectionManager.sendMessage("JOIN_REQUEST4:" + gameId + ":" + initiatorUsername + ":" + username);
                    removeExistingInvite(gameId);
                });
            }

            chatLinearLayout.addView(inviteText);
            chatLinearLayout.addView(joinButton);
            activeGameInvites.put(gameId, new View[]{inviteText, joinButton});
            chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void handleJoinFourInARow(String message) {
        String[] parts = message.split(":");
        if (parts.length == 4) {
            handler.post(() ->
                    Toast.makeText(ChatActivity.this, "You have joined the Four in a Row game!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void handleStartFourInARow(String message) {
        String[] parts = message.split(":");
        if (parts.length == 4) {
            String gameId = parts[1];
            String player1 = parts[2];
            String player2 = parts[3];

            Intent intent = new Intent(ChatActivity.this, FourInARowActivity.class);
            intent.putExtra("PLAYER1", player1);
            intent.putExtra("PLAYER2", player2);
            intent.putExtra("GAME_ID", gameId);
            intent.putExtra("USERNAME", username);
            startActivity(intent);

            removeExistingInvite(gameId);

            handler.post(() -> {
                TextView infoText = new TextView(this);
                infoText.setText("Four in a Row: " + player1 + " vs " + player2);
                infoText.setTextSize(16);
                infoText.setPadding(0, 16, 0, 8);

                Button gameStatusButton = new Button(this);
                gameStatusButton.setText("Game in Progress / Has already ended");
                gameStatusButton.setAllCaps(false);
                gameStatusButton.setEnabled(false);

                chatLinearLayout.addView(infoText);
                chatLinearLayout.addView(gameStatusButton);

                activeGameInvites.put(gameId, new View[]{infoText, gameStatusButton});
                chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
            });
        } else {
            Log.e(TAG, "Invalid START_FOURINAROW message format: " + message);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ChatActivity onDestroy() - Disconnecting.");
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}