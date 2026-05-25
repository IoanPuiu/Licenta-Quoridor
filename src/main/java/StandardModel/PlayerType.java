package StandardModel;

public enum PlayerType {
    HUMAN("Human"),
    MINIMAX("MiniMax"),
    MCTS_EASY("MCTS Easy"),
    MCTS_MEDIUM("MCTS Medium"),
    MCTS_HARD("MCTS Hard"),
    MCTS_EXTREME("MCTS Extreme"),
    GYM_PYTHON("Gym Python");

    private final String label;

    PlayerType(String label) {
        this.label = label;
    }

    public boolean isAI() {
        return this != HUMAN;
    }

    @Override
    public String toString() {
        return label;
    }
}
