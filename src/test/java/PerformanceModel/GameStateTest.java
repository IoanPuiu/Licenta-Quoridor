package PerformanceModel;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    @Test
    void constructorInitializesExpectedPublicStateAndDistances() {
        GameState gameState = new GameState();

        assertEquals(76, gameState.getCurrPlayerPos());
        assertEquals(4, gameState.getOpponentPos());
        assertEquals(10, gameState.getCurrPlayerWalls());
        assertEquals(10, gameState.getOpponentWalls());
        assertEquals(0, gameState.getCurrPlayerFinishLine());
        assertEquals(8, gameState.getOpponentFinishLine());
        assertEquals(8, gameState.getCurrentPlayerDistanceToFinish());
        assertEquals(8, gameState.getOpponentDistanceToFinish());
    }

    @Test
    void getPossiblePawnMoveCodesOnInitialBoardReturnsExpectedHardcodedMoves() {
        GameState gameState = new GameState();

        assertEquals(Set.of(267, 275, 277), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    void getPossiblePawnMoveCodesJumpsOverAdjacentOpponent() {
        GameState gameState = new GameState();

        gameState.update(267);
        gameState.update(213);
        gameState.update(258);
        gameState.update(222);
        gameState.update(249);
        gameState.update(231);
        gameState.update(240);

        assertEquals(31, gameState.getCurrPlayerPos());
        assertEquals(40, gameState.getOpponentPos());
        assertEquals(Set.of(222, 230, 232, 249), gameState.getPossiblePawnMoveCodes());
    }

    @Test
    void getPossibleWallPlacementCodesOnInitialBoardReturnsEveryWallCode() {
        GameState gameState = new GameState();

        Set<Integer> possibleWalls = gameState.getPossibleWallPlacementCodes();

        assertEquals(wallCodesFrom0To127(), possibleWalls);
        assertEquals(128, possibleWalls.size());
        assertTrue(possibleWalls.contains(0));
        assertTrue(possibleWalls.contains(127));
        assertFalse(possibleWalls.contains(128));
    }

    @Test
    void getAllPossibleMoveCodesOnInitialBoardCombinesWallsAndPawnMoves() {
        GameState gameState = new GameState();
        Set<Integer> expectedMoveCodes = wallCodesFrom0To127();
        expectedMoveCodes.addAll(Set.of(267, 275, 277));

        assertEquals(expectedMoveCodes, gameState.getAllPossibleMoveCodes());
    }

    @Test
    void updatePawnMoveChangesPawnThenSwapsPlayers() {
        GameState gameState = new GameState();

        gameState.update(267);

        assertEquals(4, gameState.getCurrPlayerPos());
        assertEquals(67, gameState.getOpponentPos());
        assertEquals(10, gameState.getCurrPlayerWalls());
        assertEquals(10, gameState.getOpponentWalls());
        assertEquals(8, gameState.getCurrPlayerFinishLine());
        assertEquals(0, gameState.getOpponentFinishLine());
    }

    @Test
    void distanceGettersUseUpdatedPawnPositionsAfterPawnMove() {
        GameState gameState = new GameState();

        gameState.update(267);

        assertEquals(8, gameState.getCurrentPlayerDistanceToFinish());
        assertEquals(7, gameState.getOpponentDistanceToFinish());
    }

    @Test
    void updateWallPlacementStoresWallDecrementsMoverWallsAndSwapsPlayers() {
        GameState gameState = new GameState();

        gameState.update(0);

        assertEquals(4, gameState.getCurrPlayerPos());
        assertEquals(76, gameState.getOpponentPos());
        assertEquals(10, gameState.getCurrPlayerWalls());
        assertEquals(9, gameState.getOpponentWalls());
        assertEquals(8, gameState.getCurrPlayerFinishLine());
        assertEquals(0, gameState.getOpponentFinishLine());

        Set<Integer> possibleWalls = gameState.getPossibleWallPlacementCodes();
        assertFalse(possibleWalls.contains(0));
        assertFalse(possibleWalls.contains(1));
        assertFalse(possibleWalls.contains(2));
        assertTrue(possibleWalls.contains(4));
        assertEquals(125, possibleWalls.size());
    }

    @Test
    void distanceGettersUseUpdatedPawnPositionsAfterPawnMoveComplex() {
        GameState gameState = new GameState();

        gameState.update(277);
        gameState.update(14);

        gameState.update(278);
        gameState.update(31);

        gameState.update(279);
        gameState.update(63);

        gameState.update(280);
        gameState.update(95);

        gameState.update(271);
        gameState.update(107);

        gameState.update(262);
        gameState.update(120);

        gameState.update(253);
        gameState.update(116);

        gameState.update(244);
        gameState.update(213);

        gameState.update(235);
        gameState.update(222);

        gameState.update(226);
        gameState.update(231);

        gameState.update(217);
        gameState.update(108);


        assertEquals(22, gameState.getCurrentPlayerDistanceToFinish());
        assertEquals(8, gameState.getOpponentDistanceToFinish());
    }

    private Set<Integer> wallCodesFrom0To127() {
        Set<Integer> wallCodes = new TreeSet<>();
        for (int wallCode = 0; wallCode < 128; wallCode++) {
            wallCodes.add(wallCode);
        }
        return wallCodes;
    }
}
