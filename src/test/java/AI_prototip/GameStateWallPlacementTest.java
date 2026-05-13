package AI_prototip;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameStateWallPlacementTest {

    @Test
    public void isLegalWallPlacementAcceptsValidWallOnOpenBoard() {
        GameState gameState = new GameState(stateWithWalls(10));

        assertTrue(gameState.isLegalWallPlacement(38));
    }

    @Test
    public void isLegalWallPlacementRejectsWallWhenCurrentPlayerHasNoWallsLeft() {
        GameState gameState = new GameState(stateWithWalls(0));

        assertFalse(gameState.isLegalWallPlacement(38));
    }

    @Test
    public void isLegalWallPlacementRejectsWallsOutsideTheWallGrid() {
        GameState gameState = new GameState(stateWithWalls(10));

        assertFalse(gameState.isLegalWallPlacement(-1));
        assertFalse(gameState.isLegalWallPlacement(128));
    }

    @Test
    public void isLegalWallPlacementRejectsWallsOverExistingWalls() {
        GameState gameState = new GameState(stateWithWalls(10, 38));

        assertFalse(gameState.isLegalWallPlacement(38));
    }

    @Test
    public void isLegalWallPlacementRejectsPerpendicularWallsOverExistingWalls() {
        GameState gameState = new GameState(stateWithWalls(10, 38));

        assertFalse(gameState.isLegalWallPlacement(39));
    }

    @Test
    public void isLegalWallPlacementRejectsHalfOverlappingHorizontalWalls() {
        GameState gameState = new GameState(stateWithWalls(10, 38));

        assertFalse(gameState.isLegalWallPlacement(36));
        assertFalse(gameState.isLegalWallPlacement(40));
        assertTrue(gameState.isLegalWallPlacement(42));
    }

    @Test
    public void isLegalWallPlacementRejectsHalfOverlappingVerticalWalls() {
        GameState gameState = new GameState(stateWithWalls(10, 75));

        assertFalse(gameState.isLegalWallPlacement(59));
        assertFalse(gameState.isLegalWallPlacement(91));
        assertFalse(gameState.isLegalWallPlacement(74));
    }

    @Test
    public void isLegalWallPlacementRejectsWallsThatBlockCurrentPlayerPath() {
        GameState gameState = new GameState(stateWithWalls(1,
                105, 33, 73, 93,
                39, 4, 90, 108,
                78, 27, 0, 30,
                61, 53, 9, 81,
                117, 13, 96
        ));

        assertFalse(gameState.isLegalWallPlacement(102));
    }

    @Test
    public void isLegalWallPlacementRejectsWallsThatBlockOpponentPath() {
        GameState gameState = new GameState(stateWithWalls(1,
                59, 115, 36, 124,
                106, 83, 22, 111,
                103, 79, 52, 72,
                93, 19, 39, 118,
                46, 6, 0
        ));

        assertFalse(gameState.isLegalWallPlacement(9));
    }

    @Test
    public void getPossibleWallPlacementCodesDoesNotReturnIllegalWalls() {
        GameState gameState = new GameState(stateWithWalls(10, 38));
        Set<Integer> possibleWalls = gameState.getPossibleWallPlacementCodes();

        assertFalse(possibleWalls.contains(36));
        assertFalse(possibleWalls.contains(38));
        assertFalse(possibleWalls.contains(39));
        assertFalse(possibleWalls.contains(40));
        assertTrue(possibleWalls.contains(42));
    }

    private int[] stateWithWalls(int currentPlayerWallsLeft, int... wallCodes) {
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
