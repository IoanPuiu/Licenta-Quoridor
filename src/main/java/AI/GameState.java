package AI;

import model.Board;
import model.Cell;
import model.Player;
import model.Wall;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    private final int[] state;
    /*
    state = [
    0.  current player position     [number between 100 and 180]
    1.  opponent position           [number between 100 and 180]
    2.  current player walls left   [number between 0 and 10]
    3.  opponent walls left         [number between 0 and 10]
    4.  first wall position         [number is (row * boardLength + col) * 2 for horizontal walls,
                                     or (row * boardLength + col) * 2 + 1 for vertical walls]
    ...
    23. last wall position
    */

    public int[] getState() {
        return state;
    }

    public GameState() {
        state = new int[24];
    }

    public GameState(Board board, Player playerInTurn, Player opponent) {
        state = new int[24];
        state[0] = encodeCellPosition(playerInTurn.getRow(), playerInTurn.getCol(), board.getBoardLength());
        state[1] = encodeCellPosition(opponent.getRow(), opponent.getCol(), board.getBoardLength());
        state[2] = playerInTurn.wallsLeft();
        state[3] = opponent.wallsLeft();

        Set<Wall> visitedWalls = new HashSet<>();
        int wallIndex = 4;
        for (int row = 0; row < board.getBoardLength()-1 && wallIndex < state.length; row++) {
            for (int col = 0; col < board.getBoardLength()-1 && wallIndex < state.length; col++) {
                Cell cell = board.getOneCell(row, col);
                wallIndex = addWallPositionIfNeeded(cell.getRightWall(), visitedWalls, wallIndex, board.getBoardLength());
                wallIndex = addWallPositionIfNeeded(cell.getLowerWall(), visitedWalls, wallIndex, board.getBoardLength());
            }
        }
    }

    private int encodeCellPosition(int row, int col, int boardLength) {
        return 100 + row * boardLength + col;
    }

    private int addWallPositionIfNeeded(Wall wall, Set<Wall> visitedWalls, int wallIndex, int boardLength) {
        // The same wall object is referenced by two cells, so it must be encoded only once.
        if (wall == null || !visitedWalls.add(wall) || wall.getCells().size() != 4) {
            return wallIndex;
        }

        int minRow = boardLength;
        int minCol = boardLength;
        for (Cell wallCell : wall.getCells()) {
            minRow = Math.min(minRow, wallCell.getRow());
            minCol = Math.min(minCol, wallCell.getCol());
        }

        state[wallIndex] = encodeWallPosition(minRow, minCol, boardLength, wall.isHorizontal());
        return wallIndex + 1;
    }

    private int encodeWallPosition(int row, int col, int boardLength, boolean isHorizontal) {
        int doubleOfCellCode = (row * boardLength + col) * 2;
        return isHorizontal ? doubleOfCellCode : doubleOfCellCode + 1;
    }

}
