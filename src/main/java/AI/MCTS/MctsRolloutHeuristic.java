package AI.MCTS;

public enum MctsRolloutHeuristic {
    PAWN_MOVES(
            "Pawn moves",
            "Rollouts advance with fast pawn-only moves."),
    PAWN_MOVES_RANDOM_WALLS(
            "Pawn moves and random walls",
            "Rollouts mix pawn moves with a few legal random wall candidates."),
    PAWN_MOVES_RELEVANT_WALLS(
            "Pawn moves and relevant walls",
            "Rollouts mix pawn moves with scored wall candidates.");

    private final String label;
    private final String description;

    MctsRolloutHeuristic(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }
}
