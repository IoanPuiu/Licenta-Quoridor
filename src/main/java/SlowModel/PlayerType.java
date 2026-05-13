package SlowModel;

public enum PlayerType {
    HUMAN("Human"),
    MINIMAX("MiniMax"),
    MTCS_EASY("MTCS Easy"),
    MTCS_MEDIUM("MTCS Medium"),
    MTCS_HARD("MTCS Hard"),
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
