package model;

import javafx.scene.paint.Color;

public class Player {

    private final String name;
    private final boolean isAI;
    private int row;
    private int col;
    private int wallsLeft;
    private final int finishRow;
    private final Color color;

    public int getFinishRow() {
        return finishRow;
    }


    public Player(String name, boolean isAI, int row, int col, int finishRow, Color color) {
        this.name = name;
        this.isAI = isAI;
        this.row = row;
        this.col = col;
        this.finishRow = finishRow;
        this.color = color;
        wallsLeft = 10;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int wallsLeft() {
        return wallsLeft;
    }

    public boolean isAI() {
        return isAI;
    }

    public Color getColor() {
        return color;
    }

    public void update(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE) {
            row = move.getTargetRow();
            col = move.getTargetCol();
        } else {
            wallsLeft--;
        }
    }


    @Override
    public String toString() {
        return name;
    }
}
