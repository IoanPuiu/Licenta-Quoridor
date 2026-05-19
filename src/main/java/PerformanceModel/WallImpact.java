package PerformanceModel;

public record WallImpact(int movesAddedToOpponent, int movesAddedToCurrentPlayer) {
    private static final WallImpact NONE = new WallImpact(0, 0);

    public static WallImpact none() {
        return NONE;
    }

    public int net() {
        return movesAddedToOpponent - movesAddedToCurrentPlayer;
    }

    public String displayText() {
        return movesAddedToOpponent + "-" + movesAddedToCurrentPlayer;
    }
}
