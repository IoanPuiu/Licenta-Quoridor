package AI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LegalMoveSelector {

    private LegalMoveSelector() {
    }

    static int firstLegalMove(GameState state) {
        return state.getPossiblePawnMoveCodes().stream()
                .min(Integer::compareTo)
                .orElseGet(() -> state.getPossibleWallPlacementCodes().stream()
                        .min(Integer::compareTo)
                        .orElseThrow(() -> new IllegalStateException("No legal moves available.")));
    }

    static List<Integer> legalMoves(GameState state) {
        List<Integer> legalMoves = new ArrayList<>();
        legalMoves.addAll(state.getPossiblePawnMoveCodes());
        legalMoves.addAll(state.getPossibleWallPlacementCodes());
        Collections.sort(legalMoves);
        return legalMoves;
    }
}
