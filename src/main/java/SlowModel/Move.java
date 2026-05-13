package SlowModel;

public class Move {
    private final Player player;
    private final MoveType type;
    private final int targetRow;
    private final int targetCol;
    private final boolean isHorizontal;

    public Move(Player player, MoveType type, int targetRow, int targetCol) {
        this(player, type, targetRow, targetCol, false);
    }

    public Move(Player player, MoveType type, int targetRow, int targetCol, boolean isHorizontal) {
        this.player = player;
        this.type = type;
        this.targetRow = targetRow;
        this.targetCol = targetCol;
        this.isHorizontal = isHorizontal;
    }

    public Player getPlayer() {
        return player;
    }

    public MoveType getType() {
        return type;
    }

    public int getTargetRow() {
        return targetRow;
    }

    public int getTargetCol() {
        return targetCol;
    }

    public boolean isHorizontal() {
        return isHorizontal;
    }
}

