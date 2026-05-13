package AI;

import PerformanceModel.GameState;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public interface Algorithm {
    int generateMove(GameState state);

    default int randomValidMove(GameState state) {
        Set<Integer> legalMoves = state.getAllPossibleMoveCodes();
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal moves available.");
        }

        int selectedIndex = ThreadLocalRandom.current().nextInt(legalMoves.size());
        int currentIndex = 0;
        for (int move : legalMoves) {
            if (currentIndex == selectedIndex) {
                return move;
            }
            currentIndex++;
        }

        throw new IllegalStateException("Could not select a legal move.");
    }
}
