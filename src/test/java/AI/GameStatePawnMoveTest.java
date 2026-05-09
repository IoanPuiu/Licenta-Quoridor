package AI;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameStatePawnMoveTest {

    @Test
    public void getPossiblePawnMoveCodesOneWallBehindOpponent() {
        GameState gameState = new GameState(new int[]{
                40, 31, 10, 10,
                40, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(230, 232, 239, 241, 249), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    public void isLegalPawnMoveUsesPawnMoveCodeOffset() {
        GameState gameState = new GameState(new int[]{
                40, 31, 10, 10,
                40, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertTrue(gameState.isLegalPawnMove(230));
        assertFalse(gameState.isLegalPawnMove(30));
    }

    @Test
    public void getPossiblePawnMoveCodesMoreComplicatedWalls() {
        GameState gameState = new GameState(new int[]{
                40, 31, 10, 10,
                38, 57, 72, 71,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(230), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    public void getPossiblePawnMoveCodesMoreComplicatedWallsAndJumpOver() {
        GameState gameState = new GameState(new int[]{
                40, 31, 10, 10,
                57, 72, 71, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(222), gameState.getPossiblePawnMoveCodes());
    }
}
