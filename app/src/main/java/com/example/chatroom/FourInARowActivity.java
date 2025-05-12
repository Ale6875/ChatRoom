package com.example.chatroom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class FourInARowActivity extends AppCompatActivity implements FourInARowView.OnMoveListener {

    private static final String TAG = "FourInARowActivity";
    private FourInARowView gameView;
    private TextView statusTextView;
    private Button resetButton, exitButton;

    private String gameId, player1, player2, username;
    private int myPlayerNumber;
    private boolean isMyTurn = false;
    private ServerConnectionManager connectionManager;
    private Handler handler = new Handler();
    private boolean opponentRequestedReset = false;
    private int initialPlayer = 1; // Alternate who starts after reset


    private final BroadcastReceiver moveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("message");
            Log.d(TAG, "Received: " + msg);

            if (msg.startsWith("MOVE4:")) {
                String[] parts = msg.split(":");
                if (parts.length == 5 && parts[1].equals(gameId)) {
                    int row = Integer.parseInt(parts[2]);
                    int col = Integer.parseInt(parts[3]);
                    int player = Integer.parseInt(parts[4]);

                    handler.post(() -> {
                        gameView.placeChip(row, col, player);
                        checkWin(row, col, player);
                        isMyTurn = (player != myPlayerNumber);
                        updateStatus();
                    });
                }
            } else if (msg.startsWith("GAME_OVER4:")) {
                String[] parts = msg.split(":");
                if (parts.length == 3 && parts[1].equals(gameId)) {
                    int winner = Integer.parseInt(parts[2]);
                    handler.post(() -> {
                        String result = (winner == 0) ? "It's a draw!" :
                                (winner == myPlayerNumber) ? "You won!" : "Opponent won!";
                        statusTextView.setText(result);
                        isMyTurn = false;
                    });
                }
            } else if (msg.startsWith("EXIT_FOURINAROW:")) {
                String[] parts = msg.split(":");
                if (parts.length == 2 && parts[1].equals(gameId)) {
                    Toast.makeText(FourInARowActivity.this, "The opponent has left the game.", Toast.LENGTH_LONG).show();
                    finish(); // Close the activity
                }
            }

        }
    };

    private final BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message == null) return;

            if (message.startsWith("RESET_GAME_PENDING:")) {
                String[] parts = message.split(":");
                if (parts.length >= 3) {
                    String requestingUser = parts[2];
                    opponentRequestedReset = true;
                    Toast.makeText(FourInARowActivity.this,
                            requestingUser + " wants to reset. Press RESET to confirm.",
                            Toast.LENGTH_LONG).show();
                }
            } else if (message.startsWith("RESET_GAME_CONFIRMED:")) {
                String[] parts = message.split(":");
                if (parts.length >= 2) {
                    gameId = parts[1];

                    // Alternate who starts
                    initialPlayer = (initialPlayer == 1) ? 2 : 1;
                    isMyTurn = (myPlayerNumber == initialPlayer);

                    gameView.resetBoard();
                    opponentRequestedReset = false;

                    statusTextView.setText("Game reset! " + (isMyTurn ? "Your turn" : "Opponent's turn"));
                    Toast.makeText(FourInARowActivity.this, "Game reset!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_four_in_a_row);

        gameView = findViewById(R.id.fourInARowView);
        statusTextView = findViewById(R.id.statusTextView);
        resetButton = findViewById(R.id.resetButton);
        exitButton = findViewById(R.id.exitButton);

        gameView.setOnMoveListener(this);

        Intent intent = getIntent();
        gameId = intent.getStringExtra("GAME_ID");
        player1 = intent.getStringExtra("PLAYER1");
        player2 = intent.getStringExtra("PLAYER2");
        username = intent.getStringExtra("USERNAME");

        myPlayerNumber = username.equals(player1) ? 1 : 2;
        isMyTurn = (myPlayerNumber == 1);
        updateStatus();

        connectionManager = ServerConnectionManager.getInstance(username);

        LocalBroadcastManager.getInstance(this).registerReceiver(moveReceiver, new IntentFilter("chat_message"));

        IntentFilter resetFilter = new IntentFilter("chat_message");
        LocalBroadcastManager.getInstance(this).registerReceiver(resetReceiver, resetFilter);

        resetButton.setOnClickListener(v -> {
            if (opponentRequestedReset) {
                connectionManager.sendMessage("RESET_GAME_REQUEST:" + gameId);
                opponentRequestedReset = false;
            } else {
                connectionManager.sendMessage("RESET_GAME_REQUEST:" + gameId);
                Toast.makeText(this,
                        "Reset requested. Waiting for opponent to confirm.",
                        Toast.LENGTH_SHORT).show();
            }
        });


        exitButton.setOnClickListener(v -> {
            connectionManager.sendMessage("EXIT_FOURINAROW:" + gameId);
            finish();
        });
    }

    private void updateStatus() {
        statusTextView.setText(isMyTurn ? "Your turn" : "Opponent's turn");
    }

    @Override
    public void onMoveMade(int row, int col, int player) {
        if (!isMyTurn) return;

        boolean placed = gameView.placeChip(row, col, myPlayerNumber);
        if (placed) {
            String move = "MOVE4:" + gameId + ":" + row + ":" + col + ":" + myPlayerNumber;
            connectionManager.sendMessage(move);
            checkWin(row, col, myPlayerNumber);
            isMyTurn = false;
            updateStatus();
        }
    }

    private void checkWin(int lastRow, int lastCol, int player) {
        int[][] board = gameView.getBoard();
        if (hasFourConnected(board, lastRow, lastCol, player)) {
            connectionManager.sendMessage("GAME_OVER4:" + gameId + ":" + player);
            statusTextView.setText((player == myPlayerNumber) ? "You won!" : "Opponent won!");
            isMyTurn = false;
        } else if (isDraw(board)) {
            connectionManager.sendMessage("GAME_OVER4:" + gameId + ":0");
            statusTextView.setText("It's a draw!");
        }
    }

    private boolean hasFourConnected(int[][] board, int row, int col, int player) {
        return countDirection(board, row, col, -1, 0, player) + countDirection(board, row, col, 1, 0, player) > 2 ||
                countDirection(board, row, col, 0, -1, player) + countDirection(board, row, col, 0, 1, player) > 2 ||
                countDirection(board, row, col, -1, -1, player) + countDirection(board, row, col, 1, 1, player) > 2 ||
                countDirection(board, row, col, -1, 1, player) + countDirection(board, row, col, 1, -1, player) > 2;
    }

    private int countDirection(int[][] board, int row, int col, int dRow, int dCol, int player) {
        int count = 0;
        for (int r = row + dRow, c = col + dCol;
             r >= 0 && r < 6 && c >= 0 && c < 7 && board[r][c] == player;
             r += dRow, c += dCol) {
            count++;
        }
        return count;
    }

    private boolean isDraw(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(moveReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resetReceiver);
    }
}
