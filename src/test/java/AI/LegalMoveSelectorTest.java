package AI;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LegalMoveSelectorTest {

    @Test
    public void legalMovesCombinesPawnMovesAndWallPlacementsInSortedOrder() {
        GameState gameState = new GameState(openBoardState(10));

        List<Integer> expectedMoves = new ArrayList<>();
        IntStream.range(0, 128).forEach(expectedMoves::add);
        expectedMoves.addAll(List.of(267, 275, 277));

        assertEquals(expectedMoves, LegalMoveSelector.legalMoves(gameState));

        assertFalse(expectedMoves.contains(128));
    }

    @Test
    public void legalMovesReturnsOnlyPawnMovesWhenCurrentPlayerHasNoWallsLeft() {
        GameState gameState = new GameState(openBoardState(0));

        assertEquals(List.of(267, 275, 277), LegalMoveSelector.legalMoves(gameState));
    }

    @Test
    public void legalMovesDoesNotIncludeIllegalWallPlacements() {
        GameState gameState = new GameState(openBoardState(10, 38));

        List<Integer> legalMoves = LegalMoveSelector.legalMoves(gameState);

        assertFalse(legalMoves.contains(36));
        assertFalse(legalMoves.contains(38));
        assertFalse(legalMoves.contains(39));
        assertFalse(legalMoves.contains(40));
    }

    private int[] openBoardState(int currentPlayerWallsLeft, int... wallCodes) {
        int[] state = new int[25];
        Arrays.fill(state, -1);
        state[0] = 76;
        state[1] = 4;
        state[2] = currentPlayerWallsLeft;
        state[3] = 10;
        state[4] = 0;

        for (int i = 0; i < wallCodes.length; i++) {
            state[5 + i] = wallCodes[i];
        }

        return state;
    }
}
