package AI_prototip;

import SlowModel.Board;
import SlowModel.Move;
import SlowModel.MoveType;
import SlowModel.Player;
import SlowModel.PlayerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class GameStateTest {

    @Test
    public void constructorFromBoardPlayersAndOpponentBuildsExpectedState() {
        Board board = new Board(9);
        Player playerInTurn = new Player(PlayerType.HUMAN, 8, 4, 0);
        Player opponent = new Player(PlayerType.HUMAN, 0, 4, 8);
        board.getOneCell(playerInTurn.getRow(), playerInTurn.getCol()).setPlayer(playerInTurn);
        board.getOneCell(opponent.getRow(), opponent.getCol()).setPlayer(opponent);

        Move horizontalWall = new Move(playerInTurn, MoveType.WALL_PLACE, 2, 3, true);
        Move verticalWall = new Move(opponent, MoveType.WALL_PLACE, 4, 5, false);
        board.placeWall(horizontalWall);
        playerInTurn.update(horizontalWall);
        board.placeWall(verticalWall);
        opponent.update(verticalWall);

        GameState gameState = new GameState(board, playerInTurn, opponent);

        assertArrayEquals(new int[]{
                76, 4, 9, 9, 0,
                38, 75, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        }, gameState.getState());
    }

    @Test
    public void constructorKeepsWallCodeZeroDistinctFromEmptyWallSlots() {
        Board board = new Board(9);
        Player playerInTurn = new Player(PlayerType.HUMAN, 8, 4, 0);
        Player opponent = new Player(PlayerType.HUMAN, 0, 4, 8);
        board.getOneCell(playerInTurn.getRow(), playerInTurn.getCol()).setPlayer(playerInTurn);
        board.getOneCell(opponent.getRow(), opponent.getCol()).setPlayer(opponent);

        Move zeroCodeWall = new Move(playerInTurn, MoveType.WALL_PLACE, 0, 0, true);
        board.placeWall(zeroCodeWall);
        playerInTurn.update(zeroCodeWall);

        GameState gameState = new GameState(board, playerInTurn, opponent);

        assertArrayEquals(new int[]{
                76, 4, 9, 10, 0,
                0, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        }, gameState.getState());
    }

    @Test
    public void constructorStoresCurrentPlayerFinishRowWhenItIsBottomRow() {
        Board board = new Board(9);
        Player playerInTurn = new Player(PlayerType.HUMAN, 0, 4, 8);
        Player opponent = new Player(PlayerType.HUMAN, 8, 4, 0);
        board.getOneCell(playerInTurn.getRow(), playerInTurn.getCol()).setPlayer(playerInTurn);
        board.getOneCell(opponent.getRow(), opponent.getCol()).setPlayer(opponent);

        GameState gameState = new GameState(board, playerInTurn, opponent);

        assertArrayEquals(new int[]{
                4, 76, 10, 10, 8,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        }, gameState.getState());
    }
}
