package model;

public enum PlayerType {
    HUMAN("Human"),
    MINIMAX("MiniMax"),
    MTCS_10K("MTCS 10k"),
    MTCS_30K("MTCS 30k"),
    MTCS_60K("MTCS 60k"),
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
