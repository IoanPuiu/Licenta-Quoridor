package AI;

import model.PlayerType;

public class Algorithm {

    private static final int MTCS_EASY_DEPTH = 10_000;
    private static final int MTCS_MEDIUM_DEPTH = 30_000;
    private static final int MTCS_HARD_DEPTH = 60_000;

    public int generateMove(GameState state, PlayerType aiType) {
        return switch (aiType) {
            case MINIMAX -> new MiniMax(state).generateMove();
            case MTCS_EASY -> new Mtcs(state, MTCS_EASY_DEPTH).generateMove();
            case MTCS_MEDIUM -> new Mtcs(state, MTCS_MEDIUM_DEPTH).generateMove();
            case MTCS_HARD -> new Mtcs(state, MTCS_HARD_DEPTH).generateMove();
            case GYM_PYTHON -> new GymPython(state).generateMove();
            case HUMAN -> throw new IllegalArgumentException("Algorithm cannot generate a move for a human player.");
        };
    }
}
