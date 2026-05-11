package AI;

import model.PlayerType;

public class Algorithm {

    private static final int MTCS_10K_DEPTH = 10_000;
    private static final int MTCS_30K_DEPTH = 30_000;
    private static final int MTCS_60K_DEPTH = 60_000;

    private final MiniMaxAlgorithm miniMaxAlgorithm;
    private final MtcsAlgorithm mtcsAlgorithm;
    private final GymPythonAlgorithm gymPythonAlgorithm;

    public Algorithm() {
        miniMaxAlgorithm = new MiniMaxAlgorithm();
        mtcsAlgorithm = new MtcsAlgorithm();
        gymPythonAlgorithm = new GymPythonAlgorithm();
    }

    public int generateMove(GameState state, PlayerType aiType) {
        return switch (aiType) {
            case MINIMAX -> miniMaxAlgorithm.generateMove(state);
            case MTCS_10K -> mtcsAlgorithm.generateMove(state, MTCS_10K_DEPTH);
            case MTCS_30K -> mtcsAlgorithm.generateMove(state, MTCS_30K_DEPTH);
            case MTCS_60K -> mtcsAlgorithm.generateMove(state, MTCS_60K_DEPTH);
            case GYM_PYTHON -> gymPythonAlgorithm.generateMove(state);
            case HUMAN -> throw new IllegalArgumentException("Algorithm cannot generate a move for a human player.");
        };
    }
}
