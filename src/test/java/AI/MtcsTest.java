package AI;

import AI.MTCS.MtcsPerformance;
import AI.MTCS.MtcsV0;
import PerformanceModel.GameState;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MtcsTest {

    @Test
    void generateMoveReturnsLegalMoveOnInitialBoard() {
        GameState state = new GameState();
        MtcsV0 mtcsV0 = new MtcsV0(5);

        int move = mtcsV0.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void generateMoveDoesNotMutateGivenState() {
        GameState state = new GameState();
        MtcsV0 mtcsV0 = new MtcsV0(5);
        Set<Integer> legalMovesBeforeSearch = state.getAllPossibleMoveCodes();

        mtcsV0.generateMove(state);

        assertEquals(76, state.getCurrPlayerPos());
        assertEquals(4, state.getOpponentPos());
        assertEquals(10, state.getCurrPlayerWalls());
        assertEquals(10, state.getOpponentWalls());
        assertEquals(legalMovesBeforeSearch, state.getAllPossibleMoveCodes());
    }

    @Test
    void generateMoveChoosesImmediateWinningPawnMove() {
        GameState state = currentPlayerOneStepFromGoal();
        MtcsV0 mtcsV0 = new MtcsV0(1);

        int move = mtcsV0.generateMove(state);

        assertTrue(GameState.isPawnMoveCode(move));
        assertEquals(state.getCurrPlayerFinishLine(), GameState.decodePawnMoveRow(move));
    }

    @Test
    void constructorRejectsInvalidStepCount() {
        assertThrows(IllegalArgumentException.class, () -> new MtcsV0(0));
    }

    @Test
    void performanceGenerateMoveReturnsLegalMoveOnInitialBoard() {
        GameState state = new GameState();
        MtcsPerformance mtcsPerformance = new MtcsPerformance(5);

        int move = mtcsPerformance.generateMove(state);

        assertTrue(state.getAllPossibleMoveCodes().contains(move));
    }

    @Test
    void performanceGenerateMoveDoesNotMutateGivenState() {
        GameState state = new GameState();
        MtcsPerformance mtcsPerformance = new MtcsPerformance(5);
        Set<Integer> legalMovesBeforeSearch = state.getAllPossibleMoveCodes();

        mtcsPerformance.generateMove(state);

        assertEquals(76, state.getCurrPlayerPos());
        assertEquals(4, state.getOpponentPos());
        assertEquals(10, state.getCurrPlayerWalls());
        assertEquals(10, state.getOpponentWalls());
        assertEquals(legalMovesBeforeSearch, state.getAllPossibleMoveCodes());
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
