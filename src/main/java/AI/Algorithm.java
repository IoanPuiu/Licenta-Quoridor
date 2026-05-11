package AI;

import model.PlayerType;

public class Algorithm {

    private static final int MTCS_10K_DEPTH = 10_000;
    private static final int MTCS_30K_DEPTH = 30_000;
    private static final int MTCS_60K_DEPTH = 60_000;

    public int generateMove(GameState state, PlayerType aiType) {
        return switch (aiType) {
            case MINIMAX -> new MiniMax(state).generateMove();
            case MTCS_10K -> new Mtcs(state, MTCS_10K_DEPTH).generateMove();
            case MTCS_30K -> new Mtcs(state, MTCS_30K_DEPTH).generateMove();
            case MTCS_60K -> new Mtcs(state, MTCS_60K_DEPTH).generateMove();
            case GYM_PYTHON -> new GymPython(state).generateMove();
            case HUMAN -> throw new IllegalArgumentException("Algorithm cannot generate a move for a human player.");
        };
    }
}
