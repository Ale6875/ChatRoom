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

    private final BroadcastReceiver moveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "moveReceiver received message: " + message);

            if (message != null) {
                if (message.startsWith("MOVE:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 5 && parts[1].equals(gameId)) {
                        try {
                            int row = Integer.parseInt(parts[2]);
                            int col = Integer.parseInt(parts[3]);
                            int player = Integer.parseInt(parts[4]);

                            handler.post(() -> {
                                if (ticTacToeView.markCell(row, col, player)) {
                                    checkGameEndConditions(player);
                                    isMyTurn = (player != myPlayerNumber);
                                    updateTurnDisplay();
                                }
                            });
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing move", e);
                        }
                    }
                } else if (message.startsWith("RESET_GAME_CONFIRMED:"))  {
                    handleResetConfirmed(intent);
                } else if (message.startsWith("EXIT_TICTACTOE:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 2 && parts[1].equals(gameId)) {
                        Toast.makeText(TicTacToeActivity.this, "The opponent has left the game.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        }
    };

    private void checkGameEndConditions(int lastPlayer) {
        int[][] board = ticTacToeView.getBoardState();

        if (checkWin(board, lastPlayer)) {
            onGameWon(lastPlayer);
        } else if (isBoardFull(board)) {
            onGameDraw();
        }
    }

    private boolean checkWin(int[][] board, int player) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) return true;
        }
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) return true;
        }
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) return true;
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) return true;

        return false;
    }

    private boolean isBoardFull(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tic_tac_toe);

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

        if (username != null) {
            myPlayerNumber = username.equals(player1Username) ? 1 : 2;
            initialPlayer = 1;
            isMyTurn = (myPlayerNumber == initialPlayer);
            updateTurnDisplay();
        } else {
            finish();
            return;
        }

        connectionManager = ServerConnectionManager.getInstance(username);

        IntentFilter moveFilter = new IntentFilter();
        moveFilter.addAction("tictactoe_move");
        moveFilter.addAction("chat_message");

        LocalBroadcastManager.getInstance(this).registerReceiver(moveReceiver, moveFilter);

        resetButton.setOnClickListener(v -> {
            if (opponentRequestedReset) {
                sendMessage("RESET_GAME_REQUEST:" + gameId);
                opponentRequestedReset = false;
            } else {
                sendMessage("RESET_GAME_REQUEST:" + gameId);
                Toast.makeText(this, "Reset requested. Waiting for opponent to confirm.", Toast.LENGTH_SHORT).show();
            }
        });

        exitGameButton.setOnClickListener(v -> {
            sendMessage("EXIT_TICTACTOE:" + gameId);
            finish();
        });
    }

    @Override
    public void onCellClicked(int row, int col) {
        if (isMyTurn && TextUtils.isEmpty(gameResultTextView.getText())) {
            boolean marked = ticTacToeView.markCell(row, col, myPlayerNumber);
            if (marked) {
                String moveMessage = "MOVE:" + gameId + ":" + row + ":" + col + ":" + myPlayerNumber;
                sendMessage(moveMessage);
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

    private void sendMessage(String message) {
        if (connectionManager != null) {
            connectionManager.sendMessage(message);
        }
    }

    private void updateTurnDisplay() {
        handler.post(() -> {
            if (TextUtils.isEmpty(gameResultTextView.getText())) {
                if (isMyTurn) {
                    playerTurnTextView.setText("Your Turn");
                } else {
                    String opponent = (myPlayerNumber == 1) ? player2Username : player1Username;
                    playerTurnTextView.setText(opponent + "'s Turn");
                }
            }
        });
    }

    private void handleResetConfirmed(Intent intent) {
        String message = intent.getStringExtra("message");
        if (message != null && message.startsWith("RESET_GAME_CONFIRMED:")) {
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                gameId = parts[1];
                ticTacToeView.resetBoard();
                gameResultTextView.setText("");
                opponentRequestedReset = false;
                initialPlayer = (initialPlayer == 1) ? 2 : 1;
                isMyTurn = (myPlayerNumber == initialPlayer);
                updateTurnDisplay();
                Toast.makeText(this, "Game reset! " + (isMyTurn ? "Your turn" : "Opponent's turn"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(moveReceiver);
    }
}
