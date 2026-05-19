package AI;

import AI.MCTS.MctsPerformance;
import AI.MCTS.MctsRolloutHeuristic;
import AI.MCTS.MctsSelectionHeuristic;
import AI.MCTS.MctsV0;
import PerformanceModel.GameState;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MctsTest {

    @Test
    void generateMoveReturnsLegalMoveOnInitialBoard() {
        GameState state = new GameState();
        MctsV0 mctsV0 = new MctsV0(5);

        int move = mctsV0.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void generateMoveDoesNotMutateGivenState() {
        GameState state = new GameState();
        MctsV0 mctsV0 = new MctsV0(5);
        Set<Integer> legalMovesBeforeSearch = state.getAllPossibleMoveCodes();

        mctsV0.generateMove(state);

        assertEquals(76, state.getCurrPlayerPos());
        assertEquals(4, state.getOpponentPos());
        assertEquals(10, state.getCurrPlayerWalls());
        assertEquals(10, state.getOpponentWalls());
        assertEquals(legalMovesBeforeSearch, state.getAllPossibleMoveCodes());
    }

    @Test
    void generateMoveChoosesImmediateWinningPawnMove() {
        GameState state = currentPlayerOneStepFromGoal();
        MctsV0 mctsV0 = new MctsV0(1);

        int move = mctsV0.generateMove(state);

        assertTrue(GameState.isPawnMoveCode(move));
        assertEquals(state.getCurrPlayerFinishLine(), GameState.decodePawnMoveRow(move));
    }

    @Test
    void constructorRejectsInvalidStepCount() {
        assertThrows(IllegalArgumentException.class, () -> new MctsV0(0));
    }

    @Test
    void constructorRejectsInvalidRolloutMoveLimit() {
        assertThrows(IllegalArgumentException.class, () -> new MctsV0(5, 0));
    }

    @Test
    void performanceGenerateMoveReturnsLegalMoveOnInitialBoard() {
        GameState state = new GameState();
        MctsPerformance mctsPerformance = new MctsPerformance(5);

        int move = mctsPerformance.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void performanceGenerateMoveAcceptsConfiguredHeuristics() {
        GameState state = new GameState();
        MctsPerformance mctsPerformance = new MctsPerformance(
                5,
                32,
                MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES,
                MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS);

        int move = mctsPerformance.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void performanceGenerateMoveDoesNotMutateGivenState() {
        GameState state = new GameState();
        MctsPerformance mctsPerformance = new MctsPerformance(5);
        Set<Integer> legalMovesBeforeSearch = state.getAllPossibleMoveCodes();

        mctsPerformance.generateMove(state);

        assertEquals(76, state.getCurrPlayerPos());
        assertEquals(4, state.getOpponentPos());
        assertEquals(10, state.getCurrPlayerWalls());
        assertEquals(10, state.getOpponentWalls());
        assertEquals(legalMovesBeforeSearch, state.getAllPossibleMoveCodes());
    }

    @Test
    void performanceConstructorRejectsInvalidRolloutMoveLimit() {
        assertThrows(IllegalArgumentException.class, () -> new MctsPerformance(5, 0));
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
}
