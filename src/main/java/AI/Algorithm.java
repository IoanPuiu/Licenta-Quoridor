package AI;

import model.PlayerType;

public class Algorithm {

    private static final int MTCS_10K_DEPTH = 10_000;
    private static final int MTCS_30K_DEPTH = 30_000;
    private static final int MTCS_60K_DEPTH = 60_000;

    private final MiniMax miniMax;
    private final Mtcs mtcs;
    private final GymPython gymPython;

    public Algorithm() {
        miniMax = new MiniMax();
        mtcs = new Mtcs();
        gymPython = new GymPython();
    }

    public int generateMove(GameState state, PlayerType aiType) {
        return switch (aiType) {
            case MINIMAX -> miniMax.generateMove(state);
            case MTCS_10K -> mtcs.generateMove(state, MTCS_10K_DEPTH);
            case MTCS_30K -> mtcs.generateMove(state, MTCS_30K_DEPTH);
            case MTCS_60K -> mtcs.generateMove(state, MTCS_60K_DEPTH);
            case GYM_PYTHON -> gymPython.generateMove(state);
            case HUMAN -> throw new IllegalArgumentException("Algorithm cannot generate a move for a human player.");
        };
    }
}
