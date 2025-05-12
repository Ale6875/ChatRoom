package com.example.chatroom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TicTacToeView extends View {

    private static final int GRID_LINE_WIDTH = 6;
    private static final float MARK_SIZE_PERCENTAGE = 0.25f;

    private int boardSize = 3;
    private int[][] board;
    private Paint gridPaint;
    private Paint oPaint;
    private Paint xPaint;
    private OnGameEventListener gameEventListener;

    public interface OnGameEventListener {
        void onCellClicked(int row, int col);

        void onGameWon(int player);

        void onGameDraw();
    }

    public void setOnGameEventListener(OnGameEventListener listener) {
        this.gameEventListener = listener;
    }

    public TicTacToeView(Context context) {
        super(context);
        init();
    }

    public TicTacToeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TicTacToeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        board = new int[boardSize][boardSize];
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(GRID_LINE_WIDTH);

        oPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        oPaint.setColor(Color.BLUE);
        oPaint.setStyle(Paint.Style.STROKE);
        oPaint.setStrokeWidth(GRID_LINE_WIDTH);

        xPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xPaint.setColor(Color.RED);
        xPaint.setStyle(Paint.Style.STROKE);
        xPaint.setStrokeWidth(GRID_LINE_WIDTH);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("TicTacToeView", "onDraw() called");
        int width = getWidth();
        int height = getHeight();
        int cellSize = width / boardSize;

        for (int i = 1; i < boardSize; i++) {
            canvas.drawLine(i * cellSize, 0, i * cellSize, height, gridPaint);
            canvas.drawLine(0, i * cellSize, width, i * cellSize, gridPaint);
        }

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == 1) {
                    float centerX = j * cellSize + cellSize / 2f;
                    float centerY = i * cellSize + cellSize / 2f;
                    float offset = cellSize * MARK_SIZE_PERCENTAGE;
                    canvas.drawLine(centerX - offset, centerY - offset, centerX + offset, centerY + offset, xPaint);
                    canvas.drawLine(centerX + offset, centerY - offset, centerX - offset, centerY + offset, xPaint);
                } else if (board[i][j] == 2) {
                    float centerX = j * cellSize + cellSize / 2f;
                    float centerY = i * cellSize + cellSize / 2f;
                    float radius = cellSize / 2f - cellSize * MARK_SIZE_PERCENTAGE;
                    canvas.drawCircle(centerX, centerY, radius, oPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            int cellSize = getWidth() / boardSize;
            int col = x / cellSize;
            int row = y / cellSize;

            if (row < boardSize && col < boardSize) {
                if (gameEventListener != null) {
                    gameEventListener.onCellClicked(row, col);
                }
                return true;
            }
        }
        return false;
    }

    public boolean markCell(int row, int col, int player) {
        Log.d("TicTacToeView", "markCell(" + row + ", " + col + ", " + player + ") called");
        Log.d("TicTacToeView", "  row = " + row + ", col = " + col + ", player = " + player);
        Log.d("TicTacToeView", "  boardSize = " + boardSize);
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize || board[row][col] != 0) {
            Log.d("TicTacToeView", "  Invalid move or cell already occupied");
            return false;
        }
        board[row][col] = player;
        Log.d("TicTacToeView", "  Board updated: board[" + row + "][" + col + "] = " + player);
        invalidate();
        Log.d("TicTacToeView", "  View invalidated");
        return true;
    }

    public void resetBoard() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j] = 0;
            }
        }
        invalidate();
    }

    public int[][] getBoardState() {
        int[][] currentState = new int[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            System.arraycopy(board[i], 0, currentState[i], 0, boardSize);
        }
        return currentState;
    }

}