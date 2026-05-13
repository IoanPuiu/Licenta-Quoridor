package AI_prototip;

import java.util.List;
import java.util.Random;

public class Mtcs {

    private final GameState state;
    private final int depth;

    public Mtcs(GameState state, int depth) {
        this.state = state;
        this.depth = depth;
    }

    public int generateMove() {
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
