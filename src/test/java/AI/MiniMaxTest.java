package AI;

import PerformanceModel.GameState;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniMaxTest {

    @Test
    void generateMoveReturnsLegalMoveOnInitialBoard() {
        GameState state = new GameState();
        MiniMax miniMax = new MiniMax(1);

        int move = miniMax.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void fastMoveOrderingReturnsLegalMoveOnInitialBoard() {
        GameState state = new GameState();
        MiniMax miniMax = new MiniMax(2, MiniMax.MoveOrdering.FAST);

        int move = miniMax.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void generateMoveDoesNotMutateGivenState() {
        GameState state = new GameState();
        MiniMax miniMax = new MiniMax(1);
        Set<Integer> legalMovesBeforeSearch = state.getAllPossibleMoveCodes();

        miniMax.generateMove(state);

        assertEquals(76, state.getCurrPlayerPos());
        assertEquals(4, state.getOpponentPos());
        assertEquals(10, state.getCurrPlayerWalls());
        assertEquals(10, state.getOpponentWalls());
        assertEquals(legalMovesBeforeSearch, state.getAllPossibleMoveCodes());
    }

    @Test
    void generateMoveChoosesImmediateWinningPawnMove() {
        GameState state = currentPlayerOneStepFromGoal();
        MiniMax miniMax = new MiniMax(1);

        int move = miniMax.generateMove(state);

        assertTrue(GameState.isPawnMoveCode(move));
        assertEquals(state.getCurrPlayerFinishLine(), GameState.decodePawnMoveRow(move));
    }

    @Test
    void generateMoveWithoutWallsChoosesShortestPathPawnMoveForAnyDepthOrOrdering() {
        GameState state = currentPlayerWithoutWalls();
        int expectedMove = shortestPathPawnMove(state);

        int shallowMove = new MiniMax(1, MiniMax.MoveOrdering.NONE).generateMove(state);
        int deepOrderedMove = new MiniMax(4, MiniMax.MoveOrdering.PRECISE).generateMove(state);

        assertEquals(0, state.getCurrPlayerWalls());
        assertEquals(expectedMove, shallowMove);
        assertEquals(expectedMove, deepOrderedMove);
    }

    private GameState currentPlayerOneStepFromGoal() {
        GameState state = new GameState();

        state.update(267);
        state.update(1);
        state.update(258);
        state.update(15);
        state.update(249);
        state.update(33);
        state.update(240);
        state.update(47);
        state.update(231);
        state.update(65);
        state.update(222);
        state.update(79);
        state.update(213);
        state.update(97);

        return state;
    }

    private GameState currentPlayerWithoutWalls() {
        GameState state = new GameState();

        for (int wallCount = 0; wallCount < GameState.MAX_PLAYER_WALLS; wallCount++) {
            state.update(state.getPossibleWallPlacementCodes().iterator().next());
            state.update(pawnMoveThatKeepsMoverFarthestFromFinish(state));
        }

        return state;
    }

    private int shortestPathPawnMove(GameState state) {
        return state.getPossiblePawnMoveCodes().stream()
                .min(Comparator.comparingInt((Integer move) -> distanceAfterPawnMove(state, move))
                        .thenComparingInt(Integer::intValue))
                .orElseThrow();
    }

    private int pawnMoveThatKeepsMoverFarthestFromFinish(GameState state) {
        return state.getPossiblePawnMoveCodes().stream()
                .max(Comparator.comparingInt((Integer move) -> distanceAfterPawnMove(state, move))
                        .thenComparingInt(Integer::intValue))
                .orElseThrow();
    }

    private int distanceAfterPawnMove(GameState state, int move) {
        GameState child = new GameState(state);
        child.update(move);
        return child.getOpponentDistanceToFinish();
    }
}
