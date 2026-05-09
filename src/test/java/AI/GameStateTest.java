package AI;

import javafx.scene.paint.Color;
import model.Board;
import model.Move;
import model.MoveType;
import model.Player;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameStateTest {

    @Test
    public void constructorFromBoardPlayersAndOpponentBuildsExpectedState() {
        Board board = new Board(9);
        Player playerInTurn = new Player("First Player", false, 8, 4, 0, Color.CYAN);
        Player opponent = new Player("Second Player", false, 0, 4, 8, Color.ORANGE);
        board.getOneCell(playerInTurn.getRow(), playerInTurn.getCol()).setPlayer(playerInTurn);
        board.getOneCell(opponent.getRow(), opponent.getCol()).setPlayer(opponent);

        Move horizontalWall = new Move(playerInTurn, MoveType.WALL_PLACE, 2, 3, true);
        Move verticalWall = new Move(opponent, MoveType.WALL_PLACE, 4, 5, false);
        board.placeWall(horizontalWall);
        playerInTurn.update(horizontalWall);
        board.placeWall(verticalWall);
        opponent.update(verticalWall);

        GameState gameState = new GameState(board, playerInTurn, opponent);

        int[] state = gameState.getState();
        assertArrayEquals(new int[]{176, 104, 9, 9}, Arrays.copyOfRange(state, 0, 4));
        assertEquals(1, countInWallPositions(state, encodeWall(2, 3, board.getBoardLength(), true)));
        assertEquals(1, countInWallPositions(state, encodeWall(4, 5, board.getBoardLength(), false)));
    }

    private int encodeWall(int row, int col, int boardLength, boolean isHorizontal) {
        int doubleOfCellCode = (row * boardLength + col) * 2;
        return isHorizontal ? doubleOfCellCode : doubleOfCellCode + 1;
    }

    private int countInWallPositions(int[] state, int wallCode) {
        int count = 0;
        for (int i = 4; i < state.length; i++) {
            if (state[i] == wallCode) {
                count++;
            }
        }
        return count;
    }
}
