package AI_prototip;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameStatePawnMoveTest {

    @Test
    public void getPossiblePawnMoveCodesOneWallBehindOpponent() {
        GameState gameState = new GameState(new int[]{
                40, 31, 10, 10, 0,
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
                40, 31, 10, 10, 0,
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
                40, 31, 10, 10, 0,
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
                40, 31, 10, 10, 0,
                57, 72, 71, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(222), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    public void getPossiblePawnMoveCodesMoreComplicatedWallsAndJumpedEdges() {
        GameState gameState = new GameState(new int[]{
                28, 27, 10, 10, 0,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(218, 219, 229, 236, 237), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    public void getPossiblePawnMoveCodesMoreComplicatedWallsAndJumpedEdgesRight() {
        GameState gameState = new GameState(new int[]{
                70, 71, 10, 10, 0,
                126, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(261, 262, 269), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    public void getPossiblePawnMoveCodesMoreComplicatedWallsAndJumpedEdgesUP() {
        GameState gameState = new GameState(new int[]{
                13, 4, 10, 10, 0,
                23, 24, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(203, 205, 214), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    public void getPossiblePawnMoveCodesMoreComplicatedWallsAndJumpedEdgesDown() {
        GameState gameState = new GameState(new int[]{
                63, 72, 10, 10, 0,
                96, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        assertEquals(Set.of(264, 273), gameState.getPossiblePawnMoveCodes());
    }
}
