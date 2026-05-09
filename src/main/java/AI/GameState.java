package AI;

import model.Board;
import model.Cell;
import model.Player;
import model.Wall;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameState {
    private static final int DEFAULT_BOARD_LENGTH = 9;
    private static final int STATE_LENGTH = 24;
    private static final int FIRST_WALL_INDEX = 4;
    private static final int EMPTY_WALL_SLOT = -1;
    private static final int PAWN_MOVE_CODE_OFFSET = 200;

    private final int[] state;
    private final int boardLength;
    /*
    state = [

    0.  current player position
            - number between 0 and 80
            - current player can reach each cell of the 9x9 grid

    1.  opponent position
            - number between 0 and 80
            - opponent player can reach each cell of the 9x9 grid

    2.  current player walls left
            - number between 0 and 10
            - current player has maximum 10 walls at any state

    3.  opponent walls left
            - number between 0 and 10
            - opponent has maximum 10 walls at any state

    4.  first wall code
            - number is between 0 and 127
            - horizontal walls codes = (row * (boardLength - 1) + col) * 2
            - vertical walls codes = (row * (boardLength  -1) + col) * 2 + 1
            - we use rows and cols in range(0..boardLength - 1) because, since a wall has the length of 2 cells and its code is represented by the first adjacent cell, we can not have walls represented by last row or col
            - -1 when no wall is stored in this slot

    ...

    23. last wall code
    */

    public int[] getState() {
        return state;
    }

    public GameState() {
        this(DEFAULT_BOARD_LENGTH);
    }

    public GameState(int boardLength) {
        this.boardLength = boardLength;
        state = new int[STATE_LENGTH];
        Arrays.fill(state, EMPTY_WALL_SLOT);
    }

    public GameState(int[] state) {
        this(DEFAULT_BOARD_LENGTH, state);
    }

    public GameState(int boardLength, int[] state) {
        if (state.length != STATE_LENGTH) {
            throw new IllegalArgumentException("Game state must contain exactly " + STATE_LENGTH + " values.");
        }

        this.boardLength = boardLength;
        this.state = Arrays.copyOf(state, state.length);
    }

    public GameState(Board board, Player playerInTurn, Player opponent) {
        this(board.getBoardLength());
        state[0] = encodeCellPosition(playerInTurn.getRow(), playerInTurn.getCol(), board.getBoardLength());
        state[1] = encodeCellPosition(opponent.getRow(), opponent.getCol(), board.getBoardLength());
        state[2] = playerInTurn.wallsLeft();
        state[3] = opponent.wallsLeft();

        Set<Wall> visitedWalls = new HashSet<>();
        int wallIndex = 4;
        for (int row = 0; row < board.getBoardLength() - 1 && wallIndex < state.length; row++) {
            for (int col = 0; col < board.getBoardLength() - 1 && wallIndex < state.length; col++) {
                Cell cell = board.getOneCell(row, col);
                wallIndex = addWallPositionIfNeeded(cell.getRightWall(), visitedWalls, wallIndex, board.getBoardLength());
                wallIndex = addWallPositionIfNeeded(cell.getLowerWall(), visitedWalls, wallIndex, board.getBoardLength());
            }
        }
    }

    public Map<Integer, Set<Integer>> generatePawnMoveGraph() {
        Map<Integer, Set<Integer>> graph = new HashMap<>();

        for (int row = 0; row < boardLength; row++) {
            for (int col = 0; col < boardLength; col++) {
                graph.put(encodeCellPosition(row, col, boardLength), new HashSet<>());
            }
        }

        for (int row = 0; row < boardLength; row++) {
            for (int col = 0; col < boardLength; col++) {
                int cellPosition = encodeCellPosition(row, col, boardLength);
                if (row + 1 < boardLength) {
                    addGraphEdge(graph, cellPosition, encodeCellPosition(row + 1, col, boardLength));
                }
                if (col + 1 < boardLength) {
                    addGraphEdge(graph, cellPosition, encodeCellPosition(row, col + 1, boardLength));
                }
            }
        }

        for (int i = FIRST_WALL_INDEX; i < state.length; i++) {
            if (state[i] != EMPTY_WALL_SLOT) {
                removeWallEdges(graph, state[i]);
            }
        }

        return graph;
    }

    public Set<Integer> getPossiblePawnMoveCodes() {

        // ATENTIE: aceasta functie trebuie sa returneze doar codurile miscarii pionului jucatorului curent, nu si ale oponentului

        Map<Integer, Set<Integer>> graph = generatePawnMoveGraph();
        int playerPosition = state[0];
        int opponentPosition = state[1];
        Set<Integer> possibleMovePositions = new HashSet<>();

        if (!graph.containsKey(playerPosition) || !graph.containsKey(opponentPosition)) {
            return possibleMovePositions;
        }

        for (int neighbourPosition : graph.get(playerPosition)) {
            if (neighbourPosition == opponentPosition) {
                addJumpOrSideMoves(graph, possibleMovePositions, playerPosition, opponentPosition);
            } else {
                possibleMovePositions.add(neighbourPosition);
            }
        }

        Set<Integer> possibleMoveCodes = new HashSet<>();
        for (int position : possibleMovePositions) {
            possibleMoveCodes.add(encodePawnMoveCode(position));
        }

        return possibleMoveCodes;
    }

    public Set<Integer> getPossiblePawnMovePositions() {
        return getPossiblePawnMoveCodes();
    }

    public boolean isLegalPawnMove(int moveCode) {
        return getPossiblePawnMoveCodes().contains(moveCode);
    }

    private void addJumpOrSideMoves(Map<Integer, Set<Integer>> graph, Set<Integer> possibleMoves, int playerPosition, int opponentPosition) {
        int playerRow = decodeRow(playerPosition);
        int playerCol = decodeCol(playerPosition);
        int opponentRow = decodeRow(opponentPosition);
        int opponentCol = decodeCol(opponentPosition);
        int rowDirection = opponentRow - playerRow;
        int colDirection = opponentCol - playerCol;
        int jumpRow = opponentRow + rowDirection;
        int jumpCol = opponentCol + colDirection;

        if (isInsideBoard(jumpRow, jumpCol)) {
            int jumpPosition = encodeCellPosition(jumpRow, jumpCol, boardLength);
            if (graph.get(opponentPosition).contains(jumpPosition)) {
                possibleMoves.add(jumpPosition);
                return;
            }
        }

        addSideMoveIfPossible(graph, possibleMoves, opponentPosition, opponentRow + colDirection, opponentCol + rowDirection);
        addSideMoveIfPossible(graph, possibleMoves, opponentPosition, opponentRow - colDirection, opponentCol - rowDirection);
    }

    private void addSideMoveIfPossible(Map<Integer, Set<Integer>> graph, Set<Integer> possibleMoves, int opponentPosition, int row, int col) {
        if (!isInsideBoard(row, col)) {
            return;
        }

        int sidePosition = encodeCellPosition(row, col, boardLength);
        if (graph.get(opponentPosition).contains(sidePosition)) {
            possibleMoves.add(sidePosition);
        }
    }

    private void addGraphEdge(Map<Integer, Set<Integer>> graph, int firstCellPosition, int secondCellPosition) {
        graph.get(firstCellPosition).add(secondCellPosition);
        graph.get(secondCellPosition).add(firstCellPosition);
    }

    private void removeWallEdges(Map<Integer, Set<Integer>> graph, int wallPosition) {
        int cellPosition = wallPosition / 2;
        int wallGridLength = boardLength - 1;
        int row = cellPosition / wallGridLength;
        int col = cellPosition % wallGridLength;

        if (row < 0 || row >= boardLength - 1 || col < 0 || col >= boardLength - 1) {
            return;
        }

        if (wallPosition % 2 == 0) {
            removeGraphEdge(graph, encodeCellPosition(row, col, boardLength), encodeCellPosition(row + 1, col, boardLength));
            removeGraphEdge(graph, encodeCellPosition(row, col + 1, boardLength), encodeCellPosition(row + 1, col + 1, boardLength));
        } else {
            removeGraphEdge(graph, encodeCellPosition(row, col, boardLength), encodeCellPosition(row, col + 1, boardLength));
            removeGraphEdge(graph, encodeCellPosition(row + 1, col, boardLength), encodeCellPosition(row + 1, col + 1, boardLength));
        }
    }

    private void removeGraphEdge(Map<Integer, Set<Integer>> graph, int firstCellPosition, int secondCellPosition) {
        graph.get(firstCellPosition).remove(secondCellPosition);
        graph.get(secondCellPosition).remove(firstCellPosition);
    }

    private int decodeRow(int cellPosition) {
        return cellPosition / boardLength;
    }

    private int decodeCol(int cellPosition) {
        return cellPosition % boardLength;
    }

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < boardLength && col >= 0 && col < boardLength;
    }

    private int encodeCellPosition(int row, int col, int boardLength) {
        return row * boardLength + col;
    }

    private int encodePawnMoveCode(int position) {
        return PAWN_MOVE_CODE_OFFSET + position;
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
        int wallGridLength = boardLength - 1;
        int doubleOfCellCode = (row * wallGridLength + col) * 2;
        return isHorizontal ? doubleOfCellCode : doubleOfCellCode + 1;
    }

}
