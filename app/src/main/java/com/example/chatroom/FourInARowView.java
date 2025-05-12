package com.example.chatroom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FourInARowView extends View {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private int[][] board = new int[ROWS][COLS];
    private Paint player1Paint, player2Paint, gridPaint;
    private OnMoveListener moveListener;

    public interface OnMoveListener {
        void onMoveMade(int row, int col, int player);
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.moveListener = listener;
    }

    public FourInARowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        player1Paint = new Paint();
        player1Paint.setColor(Color.RED);
        player2Paint = new Paint();
        player2Paint.setColor(Color.YELLOW);
        gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK);
        gridPaint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cellWidth = getWidth() / (float) COLS;
        float cellHeight = getHeight() / (float) ROWS;

        Paint boardPaint = new Paint();
        boardPaint.setColor(Color.rgb(33, 150, 243));
        canvas.drawRect(0, 0, getWidth(), getHeight(), boardPaint);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float cx = col * cellWidth + cellWidth / 2f;
                float cy = row * cellHeight + cellHeight / 2f;
                float radius = Math.min(cellWidth, cellHeight) / 2f - 12;

                Paint paint;
                if (board[row][col] == 1) {
                    paint = player1Paint;
                } else if (board[row][col] == 2) {
                    paint = player2Paint;
                } else {
                    paint = new Paint();
                    paint.setColor(Color.WHITE);
                }

                canvas.drawCircle(cx, cy, radius, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return false;
        int col = (int) (e.getX() / (getWidth() / COLS));
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == 0) {
                if (moveListener != null) moveListener.onMoveMade(row, col, 0);
                return true;
            }
        }
        return false;
    }

    public boolean placeChip(int row, int col, int player) {
        if (board[row][col] == 0) {
            board[row][col] = player;
            invalidate();
            return true;
        }
        return false;
    }

    public void resetBoard() {
        board = new int[ROWS][COLS];
        invalidate();
    }

    public int[][] getBoard() {
        return board;
    }
}
