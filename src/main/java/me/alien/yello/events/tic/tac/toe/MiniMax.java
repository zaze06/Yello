/*
 * MIT License
 *
 * Copyright (c) 2022. Zacharias Zellén
 *
 * This is not my file nor my code i don't take credit for this
 */

package me.alien.yello.events.tic.tac.toe;

public class MiniMax {
    private static final int MAX_DEPTH = 2;

    /**
     * Play moves on the board alternating between playing as X and O analysing
     * the board each time to return the value of the highest value move for the
     * X player. Return the highest value move when a terminal node or the
     * maximum search depth is reached.
     * @param board Board to play on and evaluate
     * @param depth The maximum depth of the game tree to search to
     * @param isMax Maximising or minimising player
     * @return Value of the board
     */
    public static int miniMax(Board board, int depth, boolean isMax) {
        int boardVal = evaluateBoard(board, depth);

        // Terminal node (win/lose/draw) or max depth reached.
        if (Math.abs(boardVal) > 0 || depth == 0
                || !board.anyMovesAvailable()) {
            return boardVal;
        }

        // Maximising player, find the maximum attainable value.
        if (isMax) {
            int highestVal = Integer.MIN_VALUE;
            for (int row = 0; row < board.getWidth(); row++) {
                for (int col = 0; col < board.getWidth(); col++) {
                    if (!board.isTileMarked(row, col)) {
                        board.setMarkAt(row, col, Mark.X);
                        highestVal = Math.max(highestVal, miniMax(board,
                                depth - 1, false));
                        board.setMarkAt(row, col, Mark.BLANK);
                    }
                }
            }
            return highestVal;
            // Minimising player, find the minimum attainable value;
        } else {
            int lowestVal = Integer.MAX_VALUE;
            for (int row = 0; row < board.getWidth(); row++) {
                for (int col = 0; col < board.getWidth(); col++) {
                    if (!board.isTileMarked(row, col)) {
                        board.setMarkAt(row, col, Mark.O);
                        lowestVal = Math.min(lowestVal, miniMax(board,
                                depth - 1, true));
                        board.setMarkAt(row, col, Mark.BLANK);
                    }
                }
            }
            return lowestVal;
        }
    }

    /**
     * Evaluate every legal move on the board and return the best one.
     * @param board Board to evaluate
     * @return Coordinates of best move
     */
    public static int[] getBestMove(Board board) {
        int[] bestMove = new int[]{-1, -1};
        int bestValue = Integer.MIN_VALUE;

        for (int row = 0; row < board.getWidth(); row++) {
            for (int col = 0; col < board.getWidth(); col++) {
                if (!board.isTileMarked(row, col)) {
                    board.setMarkAt(row, col, Mark.X);
                    int moveValue = miniMax(board, MAX_DEPTH, false);
                    board.setMarkAt(row, col, Mark.BLANK);
                    if (moveValue > bestValue) {
                        bestMove[0] = row;
                        bestMove[1] = col;
                        bestValue = moveValue;
                    }
                }
            }
        }
        return bestMove;
    }

    /**
     * Evaluate the given board from the perspective of the X player, return
     * 10 if a winning board configuration is found, -10 for a losing one and 0
     * for a draw, weight the value of a win/loss/draw according to how many
     * moves it would take to realise it using the depth of the game tree the
     * board configuration is at.
     * @param board Board to evaluate
     * @param depth depth of the game tree the board configuration is at
     * @return value of the board
     */
    private static int evaluateBoard(Board board, int depth) {
        int rowSum = 0;
        int bWidth = board.getWidth();
        int Xwin = Mark.X.getMark() * bWidth;
        int Owin = Mark.O.getMark() * bWidth;

        // Check rows for winner.
        for (int row = 0; row < bWidth; row++) {
            for (int col = 0; col < bWidth; col++) {
                rowSum += board.getMarkAt(row, col).getMark();
            }
            if (rowSum == Xwin) {
                return 10 + depth;
            } else if (rowSum == Owin) {
                return -10 - depth;
            }
            rowSum = 0;
        }

        // Check columns for winner.
        rowSum = 0;
        for (int col = 0; col < bWidth; col++) {
            for (int row = 0; row < bWidth; row++) {
                rowSum += board.getMarkAt(row, col).getMark();
            }
            if (rowSum == Xwin) {
                return 10 + depth;
            } else if (rowSum == Owin) {
                return -10 - depth;
            }
            rowSum = 0;
        }

        // Check diagonals for winner.
        // Top-left to bottom-right diagonal.
        rowSum = 0;
        for (int i = 0; i < bWidth; i++) {
            rowSum += board.getMarkAt(i, i).getMark();
        }
        if (rowSum == Xwin) {
            return 10 + depth;
        } else if (rowSum == Owin) {
            return -10 - depth;
        }

        // Top-right to bottom-left diagonal.
        rowSum = 0;
        int indexMax = bWidth - 1;
        for (int i = 0; i <= indexMax; i++) {
            rowSum += board.getMarkAt(i, indexMax - i).getMark();
        }
        if (rowSum == Xwin) {
            return 10 + depth;
        } else if (rowSum == Owin) {
            return -10 - depth;
        }

        return 0;
    }
}
