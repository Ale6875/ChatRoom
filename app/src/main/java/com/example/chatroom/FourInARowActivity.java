package com.example.chatroom;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FourInARowActivity extends AppCompatActivity implements ServerConnectionManager.MessageListener {

    private static final String TAG = "FourInARowActivity";
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final int PLAYER1 = 1;
    private static final int PLAYER2 = 2;

    private LinearLayout gameBoardLayout;
    private TextView turnTextView;
    private String player1, player2, gameId, username;
    private int currentPlayer = PLAYER1;
    private int[][] gameBoard = new int[ROWS][COLS];
    private ServerConnectionManager connectionManager;
    private boolean isMyTurn = false;
    private boolean gameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourinarow);

        gameBoardLayout = findViewById(R.id.gameBoardLayout);
        turnTextView = findViewById(R.id.turnTextView);
        Button exitButton = findViewById(R.id.exitButton);

        // Get game data from intent
        Intent intent = getIntent();
        player1 = intent.getStringExtra("PLAYER1");
        player2 = intent.getStringExtra("PLAYER2");
        gameId = intent.getStringExtra("GAME_ID");
        username = intent.getStringExtra("USERNAME");

        connectionManager = ServerConnectionManager.getInstance(username);
        connectionManager.addMessageListener(this);

        // Initialize game board
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                gameBoard[i][j] = 0;
            }
        }

        // Determine if it's my turn (player1 goes first)
        isMyTurn = username.equals(player1);

        setupGameBoard();
        updateTurnDisplay();

        exitButton.setOnClickListener(v -> {
            String exitMessage = "EXIT_FOURINAROW:" + gameId;
            connectionManager.sendMessage(exitMessage);
            finish();
        });
    }

    private void setupGameBoard() {
        gameBoardLayout.removeAllViews();

        // Get display metrics
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        // Calculate padding and available width more precisely
        int horizontalPadding = (int) (16 * displayMetrics.density); // Reduced padding
        int availableWidth = screenWidth - 2 * horizontalPadding; // Account for both sides

        // Calculate cell size with minimal spacing
        int spacing = (int) (1 * displayMetrics.density); // Slightly increased spacing
        int totalSpacing = (COLS - 1) * spacing;
        int cellSize = (availableWidth - totalSpacing) / COLS;

        // Create column headers
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                availableWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (int col = 0; col < COLS; col++) {
            Button columnButton = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    cellSize, (int) (30 * displayMetrics.density));
            if (col < COLS - 1) {
                params.setMargins(0, 0, spacing, 0);
            }
            columnButton.setLayoutParams(params);
            columnButton.setText("↓");
            columnButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            columnButton.setAllCaps(false);
            columnButton.setTag(col);
            columnButton.setOnClickListener(this::onColumnClick);
            columnButton.setEnabled(isMyTurn && !gameOver);
            columnButton.setBackgroundColor(Color.TRANSPARENT);
            columnButton.setTextColor(Color.WHITE);
            headerLayout.addView(columnButton);
        }
        gameBoardLayout.addView(headerLayout);

        // Create game board rows
        for (int row = 0; row < ROWS; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    availableWidth, cellSize));

            for (int col = 0; col < COLS; col++) {
                ImageView cell = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        cellSize, cellSize);
                if (col < COLS - 1) {
                    params.setMargins(0, 0, spacing, 0);
                }
                cell.setLayoutParams(params);
                cell.setImageResource(R.drawable.cell_empty);
                cell.setBackgroundResource(R.drawable.cell_border);
                cell.setTag(row + "," + col);
                rowLayout.addView(cell);
            }
            gameBoardLayout.addView(rowLayout);
        }
    }

    private void onColumnClick(View view) {
        if (!isMyTurn || gameOver) return;

        int col = (int) view.getTag();
        int row = findEmptyRow(col);

        if (row != -1) {
            // Make move locally
            gameBoard[row][col] = currentPlayer;
            updateBoard();

            // Send move to server
            String moveMessage = "MOVE:FOURINAROW:" + gameId + ":" + row + ":" + col + ":" + currentPlayer;
            connectionManager.sendMessage(moveMessage);
            Log.d(TAG, "Sent move: " + moveMessage);

            // Check for win or draw
            if (checkWin(row, col)) {
                String winMessage = "GAME_OVER:FOURINAROW:" + gameId + ":" + currentPlayer;
                connectionManager.sendMessage(winMessage);
                gameOver = true;
                showGameOver(currentPlayer);
                return;
            } else if (isBoardFull()) {
                String drawMessage = "GAME_OVER:FOURINAROW:" + gameId + ":0";
                connectionManager.sendMessage(drawMessage);
                gameOver = true;
                showGameOver(0);
                return;
            }

            // Switch turns
            currentPlayer = (currentPlayer == PLAYER1) ? PLAYER2 : PLAYER1;
            isMyTurn = false;
            updateTurnDisplay();
            updateColumnButtons();
        }
    }

    private void updateColumnButtons() {
        LinearLayout headerLayout = (LinearLayout) gameBoardLayout.getChildAt(0);
        for (int i = 0; i < headerLayout.getChildCount(); i++) {
            View child = headerLayout.getChildAt(i);
            if (child instanceof Button) {
                child.setEnabled(isMyTurn && !gameOver);
            }
        }
    }

    private int findEmptyRow(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (gameBoard[row][col] == 0) {
                return row;
            }
        }
        return -1;
    }

    private void updateBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView cell = gameBoardLayout.findViewWithTag(row + "," + col);
                if (cell != null) {
                    switch (gameBoard[row][col]) {
                        case PLAYER1:
                            cell.setImageResource(R.drawable.cell_red);
                            break;
                        case PLAYER2:
                            cell.setImageResource(R.drawable.cell_yellow);
                            break;
                        default:
                            cell.setImageResource(R.drawable.cell_empty);
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(String message) {
        Log.d(TAG, "Received: " + message);

        if (message.startsWith("MOVE:FOURINAROW:")) {
            handleMoveNotification(message);
        } else if (message.startsWith("GAME_OVER:FOURINAROW:")) {
            handleGameOver(message);
        } else if (message.startsWith("ERROR:")) {
            handleErrorMessage(message);
        }
    }

    private void handleMoveNotification(String message) {
        runOnUiThread(() -> {
            String[] parts = message.split(":");
            if (parts.length == 7 && parts[2].equals(gameId)) {
                int row = Integer.parseInt(parts[3]);
                int col = Integer.parseInt(parts[4]);
                int player = Integer.parseInt(parts[5]);

                boolean isOpponentMove = (player == PLAYER1 && username.equals(player2)) ||
                        (player == PLAYER2 && username.equals(player1));

                if (gameBoard[row][col] == 0 && isOpponentMove) {
                    gameBoard[row][col] = player;
                    updateBoard();

                    if (checkWin(row, col)) {
                        gameOver = true;
                        showGameOver(player);
                    } else if (isBoardFull()) {
                        gameOver = true;
                        showGameOver(0);
                    } else {
                        currentPlayer = (currentPlayer == PLAYER1) ? PLAYER2 : PLAYER1;
                        isMyTurn = true;
                        updateTurnDisplay();
                        updateColumnButtons();
                    }
                }
            }
        });
    }

    private boolean checkWin(int row, int col) {
        int player = gameBoard[row][col];
        return checkDirection(row, col, 1, 0, player) || // Vertical
                checkDirection(row, col, 0, 1, player) || // Horizontal
                checkDirection(row, col, 1, 1, player) || // Diagonal down-right
                checkDirection(row, col, 1, -1, player);  // Diagonal down-left
    }

    private boolean checkDirection(int row, int col, int rowDir, int colDir, int player) {
        int count = 1;
        count += countInDirection(row, col, rowDir, colDir, player);
        count += countInDirection(row, col, -rowDir, -colDir, player);
        return count >= 4;
    }

    private int countInDirection(int row, int col, int rowDir, int colDir, int player) {
        int count = 0;
        int r = row + rowDir;
        int c = col + colDir;

        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && gameBoard[r][c] == player) {
            count++;
            r += rowDir;
            c += colDir;
        }
        return count;
    }

    private boolean isBoardFull() {
        for (int col = 0; col < COLS; col++) {
            if (gameBoard[0][col] == 0) {
                return false;
            }
        }
        return true;
    }

    private void updateTurnDisplay() {
        String playerName = (currentPlayer == PLAYER1) ? player1 : player2;
        String turnText = playerName + "'s Turn" + (isMyTurn ? " (Your Turn)" : "");
        turnTextView.setText(turnText);
    }

    private void showGameOver(int winner) {
        String message = winner == 0 ?
                "Game Over - It's a Draw!" :
                "Game Over - " + (winner == PLAYER1 ? player1 : player2) + " Wins!";
        turnTextView.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    private void handleGameOver(String message) {
        String[] parts = message.split(":");
        if (parts.length == 4 && parts[2].equals(gameId)) {
            int winner = Integer.parseInt(parts[3]);
            runOnUiThread(() -> {
                gameOver = true;
                showGameOver(winner);
                updateColumnButtons();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionManager.removeMessageListener(this);
    }
}