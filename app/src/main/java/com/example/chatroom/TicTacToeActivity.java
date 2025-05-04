package com.example.chatroom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TicTacToeActivity extends AppCompatActivity implements TicTacToeView.OnGameEventListener {

    private static final String TAG = "TicTacToeActivity";
    private TicTacToeView ticTacToeView;
    private TextView playerTurnTextView;
    private TextView gameResultTextView;
    private Button resetButton;
    private Button exitGameButton;
    private String player1Username;
    private String player2Username;
    private String gameId;
    private String username;
    private int myPlayerNumber;
    private boolean isMyTurn = false;
    private ServerConnectionManager connectionManager;
    private boolean opponentRequestedReset = false;
    private int initialPlayer = 1;

    private Handler handler = new Handler(Looper.getMainLooper());

    private BroadcastReceiver moveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "moveReceiver received message: " + message);

            if (message != null) {
                if (message.startsWith("MOVE:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 5) {
                        String receivedGameId = parts[1];
                        if (gameId.equals(receivedGameId)) {
                            try {
                                int row = Integer.parseInt(parts[2]);
                                int col = Integer.parseInt(parts[3]);
                                int player = Integer.parseInt(parts[4]);

                                handler.post(() -> {
                                    if (ticTacToeView.markCell(row, col, player)) {
                                        // Check for win/draw after opponent's move
                                        checkGameEndConditions(player);
                                        isMyTurn = (player != myPlayerNumber);
                                        updateTurnDisplay();
                                    }
                                });
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing move", e);
                            }
                        }
                    }
                } else if (message.startsWith("RESET_GAME_CONFIRMED:"))  {
                    // Pass the current intent to handleResetConfirmed
                    handleResetConfirmed(intent);
                }
            }
        }
    };

    // Add these new methods for win/draw checking
    private void checkGameEndConditions(int lastPlayer) {
        int[][] board = ticTacToeView.getBoardState();

        if (checkWin(board, lastPlayer)) {
            onGameWon(lastPlayer);
        } else if (isBoardFull(board)) {
            onGameDraw();
        }
    }

    private boolean checkWin(int[][] board, int player) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                return true;
            }
        }

        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) {
                return true;
            }
        }

        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return true;
        }
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return true;
        }

        return false;
    }

    private boolean isBoardFull(int[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) { // 0 means empty cell
                    return false;
                }
            }
        }
        return true;
    }

    // Rest of your existing code remains the same...
    private BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "tictactoe_reset_request":
                        handleResetRequest();
                        break;
                    case "tictactoe_reset_confirmed":
                        handleResetConfirmed(intent);
                        break;
                    case "tictactoe_reset_pending":
                        String message = intent.getStringExtra("message");
                        if (message != null && message.startsWith("RESET_GAME_PENDING:")) {
                            String[] parts = message.split(":");
                            if (parts.length >= 3) {
                                String requestingUser = parts[2];
                                handler.post(() -> {
                                    opponentRequestedReset = true;
                                    Toast.makeText(TicTacToeActivity.this,
                                            requestingUser + " wants to reset. Press RESET to confirm.",
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                        break;
                    case "tictactoe_reset_game":
                        handleResetGame();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tic_tac_toe);

        ticTacToeView = findViewById(R.id.ticTacToeView);
        playerTurnTextView = findViewById(R.id.playerTurnTextView);
        gameResultTextView = findViewById(R.id.gameResultTextView);
        resetButton = findViewById(R.id.resetButton);
        exitGameButton = findViewById(R.id.exitGameButton);

        ticTacToeView.setOnGameEventListener(this);

        Intent intent = getIntent();
        gameId = intent.getStringExtra("GAME_ID");
        player1Username = intent.getStringExtra("PLAYER1");
        player2Username = intent.getStringExtra("PLAYER2");
        username = intent.getStringExtra("USERNAME");

        Log.d(TAG, "Game ID: " + gameId + ", Player 1: " + player1Username +
                ", Player 2: " + player2Username + ", My Username: " + username);

        if (username != null) {
            myPlayerNumber = username.equals(player1Username) ? 1 : 2;
            initialPlayer = 1;
            isMyTurn = (myPlayerNumber == initialPlayer);
            updateTurnDisplay();
        } else {
            Log.e(TAG, "My username is null!");
            finish();
            return;
        }

        connectionManager = ServerConnectionManager.getInstance(username);

        IntentFilter moveFilter = new IntentFilter();
        moveFilter.addAction("tictactoe_move");
        moveFilter.addAction("chat_message");
        LocalBroadcastManager.getInstance(this).registerReceiver(moveReceiver, moveFilter);

        LocalBroadcastManager.getInstance(this).registerReceiver(resetReceiver, createResetIntentFilter());

        resetButton.setOnClickListener(v -> {
            if (opponentRequestedReset) {
                sendMessage("RESET_GAME_REQUEST:" + gameId);
                opponentRequestedReset = false;
            } else {
                sendMessage("RESET_GAME_REQUEST:" + gameId);
                Toast.makeText(this,
                        "Reset requested. Waiting for opponent to confirm.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        exitGameButton.setOnClickListener(v -> {
            sendMessage("EXIT_TICTACTOE:" + gameId);
            finish();
        });
    }

    @Override
    public void onCellClicked(int row, int col) {
        Log.d(TAG, "onCellClicked(" + row + ", " + col + ") called");
        Log.d(TAG, "  isMyTurn = " + isMyTurn);
        Log.d(TAG, "  gameResultTextView.getText() = " + gameResultTextView.getText());

        if (isMyTurn && TextUtils.isEmpty(gameResultTextView.getText())) {
            boolean marked = ticTacToeView.markCell(row, col, myPlayerNumber);
            if (marked) {
                String moveMessage = "MOVE:" + gameId + ":" + row + ":" + col + ":" + myPlayerNumber;
                sendMessage(moveMessage);

                // Check for win/draw after our move
                checkGameEndConditions(myPlayerNumber);

                isMyTurn = false;
                updateTurnDisplay();
            }
        }
    }

    @Override
    public void onGameWon(int player) {
        handler.post(() -> {
            if (TextUtils.isEmpty(gameResultTextView.getText())) {
                String resultText = player == myPlayerNumber ? "You Win!" : "Opponent Wins!";
                gameResultTextView.setText(resultText);
                sendMessage("GAME_OVER:" + gameId + ":" + player);
                isMyTurn = false;
                updateTurnDisplay();
            }
        });
    }

    @Override
    public void onGameDraw() {
        handler.post(() -> {
            if (TextUtils.isEmpty(gameResultTextView.getText())) {
                gameResultTextView.setText("It's a Draw!");
                sendMessage("GAME_OVER:" + gameId + ":0");
                isMyTurn = false;
                updateTurnDisplay();
            }
        });
    }

    // Rest of your existing methods...
    private IntentFilter createResetIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("tictactoe_reset_request");
        filter.addAction("tictactoe_reset_confirmed");
        filter.addAction("tictactoe_reset_pending");
        filter.addAction("tictactoe_reset_game");
        filter.addAction("chat_message");
        return filter;
    }

    private void handleResetRequest() {
        handler.post(() -> {
            opponentRequestedReset = true;
            Toast.makeText(this, "Opponent has requested a reset. Press RESET again to confirm.", Toast.LENGTH_LONG).show();
        });
    }

    private void handleResetConfirmed(Intent intent) {
        String message = intent.getStringExtra("message");
        if (message != null && message.startsWith("RESET_GAME_CONFIRMED:")) {
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                // Update the game ID with the new one from server
                gameId = parts[1];

                handler.post(() -> {
                    ticTacToeView.resetBoard();
                    gameResultTextView.setText("");
                    opponentRequestedReset = false;

                    // Switch starting player
                    initialPlayer = (initialPlayer == 1) ? 2 : 1;
                    isMyTurn = (myPlayerNumber == initialPlayer);

                    updateTurnDisplay();

                    Toast.makeText(TicTacToeActivity.this,
                            "Game reset! " + (isMyTurn ? "Your turn" : "Opponent's turn"),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void handleResetPending(Intent intent) {
        handler.post(() -> {
            String requestingUser = intent.getStringExtra("requestingUser");
            opponentRequestedReset = true;
            Toast.makeText(this, "Reset requested by " + requestingUser + ". Press RESET again to confirm.", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleResetGame() {
        handler.post(() -> {
            resetBoard();
            opponentRequestedReset = false;
            Toast.makeText(this, "Game reset by server.", Toast.LENGTH_SHORT).show();
        });
    }

    private void resetBoard() {
        ticTacToeView.resetBoard();
        gameResultTextView.setText("");
        opponentRequestedReset = false;
        isMyTurn = (myPlayerNumber == initialPlayer);
        updateTurnDisplay();
    }

    private void updateTurnDisplay() {
        handler.post(() -> {
            if (TextUtils.isEmpty(gameResultTextView.getText())) {
                if (isMyTurn) {
                    playerTurnTextView.setText("Your Turn");
                } else {
                    String opponentName = (myPlayerNumber == 1) ? player2Username : player1Username;
                    playerTurnTextView.setText(opponentName + "'s Turn");
                }
            }
        });
    }

    private void sendMessage(String message) {
        if (connectionManager != null) {
            connectionManager.sendMessage(message);
        } else {
            Log.e(TAG, "Connection manager is null, cannot send message: " + message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(moveReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resetReceiver);
    }
}