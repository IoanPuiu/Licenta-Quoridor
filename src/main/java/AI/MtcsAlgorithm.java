package AI;

import java.util.List;
import java.util.Random;

public class MtcsAlgorithm {

    public int generateMove(GameState state, int depth) {
        List<Integer> legalMoves = LegalMoveSelector.legalMoves(state);
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal moves available.");
        }

        Random random = new Random(createSeed(state, depth));
        return legalMoves.get(random.nextInt(legalMoves.size()));
    }

    private int createSeed(GameState state, int depth) {
        int seed = 17;
        for (int value : state.getState()) {
            seed = seed * 31 + value;
        }
        return seed * 31 + depth;
    }
}
