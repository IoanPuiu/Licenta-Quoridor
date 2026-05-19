package AI.MCTS;

public enum MctsSelectionHeuristic {
    WALLS_NEAR_PAWNS(
            "Walls near pawns",
            "Scores candidate walls generated around both pawns."),
    WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES(
            "Walls near pawns, existing walls and edges",
            "Extends selection with walls near existing walls and side-edge anchors.");

    private final String label;
    private final String description;

    MctsSelectionHeuristic(String label, String description) {
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
