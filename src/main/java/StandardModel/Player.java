package StandardModel;

public class Player {

    private final PlayerType playerType;
    private int row;
    private int col;
    private int wallsLeft;
    private final int finishRow;

    public int getFinishRow() {
        return finishRow;
    }

    public Player(PlayerType playerType, int row, int col, int finishRow) {
        this.playerType = playerType;
        this.row = row;
        this.col = col;
        this.finishRow = finishRow;
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
        return playerType.isAI();
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void reset(int row, int col, int wallsLeft) {
        this.row = row;
        this.col = col;
        this.wallsLeft = wallsLeft;
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
        return playerType.toString();
    }
}
