package StandardModel;

import PerformanceModel.WallImpact;

public class Move {
    private final Player player;
    private final MoveType type;
    private final int targetRow;
    private final int targetCol;
    private final boolean isHorizontal;
    private final WallImpact wallImpact;

    public Move(Player player, MoveType type, int targetRow, int targetCol) {
        this(player, type, targetRow, targetCol, false);
    }

    public Move(Player player, MoveType type, int targetRow, int targetCol, boolean isHorizontal) {
        this(player, type, targetRow, targetCol, isHorizontal, WallImpact.none());
    }

    public Move(
            Player player,
            MoveType type,
            int targetRow,
            int targetCol,
            boolean isHorizontal,
            WallImpact wallImpact) {
        this.player = player;
        this.type = type;
        this.targetRow = targetRow;
        this.targetCol = targetCol;
        this.isHorizontal = isHorizontal;
        this.wallImpact = wallImpact == null ? WallImpact.none() : wallImpact;
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

    public WallImpact getWallImpact() {
        return wallImpact;
    }

    public Move withWallImpact(WallImpact wallImpact) {
        return new Move(player, type, targetRow, targetCol, isHorizontal, wallImpact);
    }
}

