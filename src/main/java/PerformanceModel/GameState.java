package PerformanceModel;

import java.util.*;

public class GameState {
    public static final int BOARD_LENGTH = 9;
    public static final int BOARD_CELL_COUNT = BOARD_LENGTH * BOARD_LENGTH;
    public static final int MAX_PLAYER_WALLS = 10;
    public static final int MAX_PLACED_WALLS = MAX_PLAYER_WALLS * 2;
    public static final int PAWN_MOVE_CODE_OFFSET = 200;

    private static final int WALL_GRID_LENGTH = BOARD_LENGTH - 1;
    private static final int MAX_WALL_CODE = WALL_GRID_LENGTH * WALL_GRID_LENGTH * 2;
    private static final int UNREACHABLE_DISTANCE = -1;

    private int currPlayerPos;
    private int opponentPos;
    private int currPlayerWalls;
    private int opponentWalls;
    private int currPlayerFinishLine;
    private int opponentFinishLine;
    private final Set<Integer> placedWalls;
    private final Map<Integer, Set<Integer>> graph;
    private int[] topDistances;
    private int[] bottomDistances;

    public GameState() {
        this(true);
    }

    public GameState(boolean isBottomPlayerStarting) {
        currPlayerPos = encodeCellPosition(BOARD_LENGTH - 1, BOARD_LENGTH / 2);
        opponentPos = encodeCellPosition(0, BOARD_LENGTH / 2);
        currPlayerWalls = MAX_PLAYER_WALLS;
        opponentWalls = MAX_PLAYER_WALLS;
        currPlayerFinishLine = 0;
        opponentFinishLine = 8;
        placedWalls = new HashSet<>();
        graph = buildStartGameGraph();
        topDistances = computeDistancesFromRow(graph, 0);
        bottomDistances = computeDistancesFromRow(graph, BOARD_LENGTH - 1);

        if (!isBottomPlayerStarting) {
            swapPlayers();
        }
    }

    public GameState(GameState other) {
        currPlayerPos = other.currPlayerPos;
        opponentPos = other.opponentPos;
        currPlayerWalls = other.currPlayerWalls;
        opponentWalls = other.opponentWalls;
        currPlayerFinishLine = other.currPlayerFinishLine;
        opponentFinishLine = other.opponentFinishLine;
        placedWalls = new HashSet<>(other.placedWalls);
        graph = copyGraph(other.graph);
        topDistances = other.topDistances.clone();
        bottomDistances = other.bottomDistances.clone();
    }


    public int getCurrentPlayerDistanceToFinish() {
        return distanceToFinish(currPlayerPos, currPlayerFinishLine, topDistances, bottomDistances);
    }

    public int getOpponentDistanceToFinish() {
        return distanceToFinish(opponentPos, opponentFinishLine, topDistances, bottomDistances);
    }

    public Set<Integer> getPossiblePawnMoveCodes() {
        Set<Integer> possibleMoveCodes = new TreeSet<>();
        addPawnMoveCodes(possibleMoveCodes, currPlayerPos, opponentPos);
        return possibleMoveCodes;
    }

    public List<Integer> getListPossiblePawnMoveCodes() {
        List<Integer> possibleMoveCodes = new LinkedList<>();
        addPawnMoveCodes(possibleMoveCodes, currPlayerPos, opponentPos);
        return possibleMoveCodes;
    }

    public Set<Integer> getOpponentPossiblePawnMoveCodes() {
        Set<Integer> possibleMoveCodes = new TreeSet<>();
        addPawnMoveCodes(possibleMoveCodes, opponentPos, currPlayerPos);
        return possibleMoveCodes;
    }

    private void addPawnMoveCodes(Set<Integer> possibleMoveCodes, int movingPlayerPos, int blockingPlayerPos) {
        for (int neighbourPosition : graph.get(movingPlayerPos)) {
            if (neighbourPosition == blockingPlayerPos) {
                addJumpOrSideMoveCodes(possibleMoveCodes, movingPlayerPos, blockingPlayerPos);
            } else {
                possibleMoveCodes.add(encodePawnMoveCode(neighbourPosition));
            }
        }
    }

    private void addPawnMoveCodes(List<Integer> possibleMoveCodes, int movingPlayerPos, int blockingPlayerPos) {
        for (int neighbourPosition : graph.get(movingPlayerPos)) {
            if (neighbourPosition == blockingPlayerPos) {
                addJumpOrSideMoveCodes(possibleMoveCodes, movingPlayerPos, blockingPlayerPos);
            } else {
                possibleMoveCodes.add(encodePawnMoveCode(neighbourPosition));
            }
        }
    }

    public Set<Integer> getPossibleWallPlacementCodes() {
        Set<Integer> possibleWallPlacementCodes = new TreeSet<>();

        if (currPlayerWalls <= 0 || placedWalls.size() >= MAX_PLACED_WALLS) {
            return possibleWallPlacementCodes;
        }

        for (int wallCode = 0; wallCode < MAX_WALL_CODE; wallCode++) {
            if (isLegalWallPlacement(wallCode)) {
                possibleWallPlacementCodes.add(wallCode);
            }
        }

        return possibleWallPlacementCodes;
    }

    public Set<Integer> getAllPossibleMoveCodes() {
        Set<Integer> moveCodes = new TreeSet<>();
        moveCodes.addAll(getPossibleWallPlacementCodes());
        moveCodes.addAll(getPossiblePawnMoveCodes());
        return moveCodes;
    }

    public int wallImpact(int wallCode) {
        return wallImpactBreakdown(wallCode).net();
    }

    public WallImpact wallImpactBreakdown(int wallCode) {
        if (isPawnMoveCode(wallCode)) {
            return WallImpact.none();
        }
        if (!isLegalWallPlacement(wallCode)) {
            throw new IllegalArgumentException("Illegal wall placement code: " + wallCode);
        }

        int currentPlayerDistanceBefore = getCurrentPlayerDistanceToFinish();
        int opponentDistanceBefore = getOpponentDistanceToFinish();
        Map<Integer, Set<Integer>> graphAfterPlacement = copyGraph(graph);
        updateGraph(graphAfterPlacement, wallCode);
        int[] topDistancesAfterPlacement = computeDistancesFromRow(graphAfterPlacement, 0);
        int[] bottomDistancesAfterPlacement = computeDistancesFromRow(graphAfterPlacement, BOARD_LENGTH - 1);

        int currentPlayerDistanceAfter = distanceToFinish(
                currPlayerPos,
                currPlayerFinishLine,
                topDistancesAfterPlacement,
                bottomDistancesAfterPlacement);
        int opponentDistanceAfter = distanceToFinish(
                opponentPos,
                opponentFinishLine,
                topDistancesAfterPlacement,
                bottomDistancesAfterPlacement);

        int movesAddedToOpponent = opponentDistanceAfter - opponentDistanceBefore;
        int movesAddedToCurrentPlayer = currentPlayerDistanceAfter - currentPlayerDistanceBefore;
        return new WallImpact(movesAddedToOpponent, movesAddedToCurrentPlayer);
    }


    private boolean isLegalWallPlacement(int wallCode) {
        return currPlayerWalls > 0
                && !conflictsWithExistingWall(wallCode)
                && bothPlayersHavePathAfterWallPlacement(wallCode);
    }

    public void update(int moveCode) {
        if (isPawnMoveCode(moveCode)) {
            updatePawnMove(moveCode);
        } else {
            updateWallPlacement(moveCode);
        }

        swapPlayers();
    }

    public int getCurrPlayerPos() {
        return currPlayerPos;
    }

    public int getOpponentPos() {
        return opponentPos;
    }

    public int getCurrPlayerWalls() {
        return currPlayerWalls;
    }

    public int getOpponentWalls() {
        return opponentWalls;
    }

    public int getCurrPlayerFinishLine() {
        return currPlayerFinishLine;
    }

    public int getOpponentFinishLine() {
        return opponentFinishLine;
    }

    public int getDistanceFromNewPositionToFinish(int newPosition) {
        return distanceToFinish(newPosition, currPlayerFinishLine, topDistances, bottomDistances);
    }

    public int[] computeDistancesFromCurrentPlayer() {
        return computeDistancesFromPosition(currPlayerPos);
    }

    public int[] computeDistancesFromOpponent() {
        return computeDistancesFromPosition(opponentPos);
    }

    public int[] getCurrentPlayerDistancesToFinishByPosition() {
        return distancesToFinish(currPlayerFinishLine).clone();
    }

    public int[] getOpponentDistancesToFinishByPosition() {
        return distancesToFinish(opponentFinishLine).clone();
    }

    public static boolean isPawnMoveCode(int moveCode) {
        return moveCode >= PAWN_MOVE_CODE_OFFSET;
    }

    public static int decodePawnMoveRow(int moveCode) {
        return decodePawnMovePosition(moveCode) / BOARD_LENGTH;
    }

    public static int decodePawnMoveCol(int moveCode) {
        return decodePawnMovePosition(moveCode) % BOARD_LENGTH;
    }

    public static int decodeWallRow(int moveCode) {
        return wallAnchor(moveCode) / WALL_GRID_LENGTH;
    }

    public static int decodeWallCol(int moveCode) {
        return wallAnchor(moveCode) % WALL_GRID_LENGTH;
    }

    public static boolean decodeWallIsHorizontal(int moveCode) {
        return isHorizontalWall(moveCode);
    }

    private static int encodePawnMoveCode(int position) {
        return PAWN_MOVE_CODE_OFFSET + position;
    }

    public static int decodePawnMovePosition(int moveCode) {
        return moveCode - PAWN_MOVE_CODE_OFFSET;
    }


    private static int encodeCellPosition(int row, int col) {
        return row * BOARD_LENGTH + col;
    }


    private void updatePawnMove(int moveCode) {
        currPlayerPos = decodePawnMovePosition(moveCode);
    }

    private void updateWallPlacement(int wallCode) {
        if (!isLegalWallPlacement(wallCode)) {
            throw new IllegalArgumentException("Illegal wall placement code: " + wallCode);
        }

        placedWalls.add(wallCode);
        currPlayerWalls--;
        updateGraph(graph, wallCode);
        updateDistances();
    }

    private void updateDistances() {
        topDistances = computeDistancesFromRow(graph, 0);
        bottomDistances = computeDistancesFromRow(graph, BOARD_LENGTH - 1);
    }

    private void swapPlayers() {
        int previousCurrPlayerPos = currPlayerPos;
        int previousCurrPlayerWalls = currPlayerWalls;
        int previousCurrPlayerFinishLine = currPlayerFinishLine;

        currPlayerPos = opponentPos;
        currPlayerWalls = opponentWalls;
        currPlayerFinishLine = opponentFinishLine;

        opponentPos = previousCurrPlayerPos;
        opponentWalls = previousCurrPlayerWalls;
        opponentFinishLine = previousCurrPlayerFinishLine;
    }

    private Map<Integer, Set<Integer>> buildStartGameGraph() {
        Map<Integer, Set<Integer>> openGraph = new HashMap<>();

        for (int position = 0; position < BOARD_CELL_COUNT; position++) {
            openGraph.put(position, new HashSet<>());
        }

        for (int row = 0; row < BOARD_LENGTH; row++) {
            for (int col = 0; col < BOARD_LENGTH; col++) {
                int position = encodeCellPosition(row, col);
                if (row + 1 < BOARD_LENGTH) {
                    addGraphEdge(openGraph, position, encodeCellPosition(row + 1, col));
                }
                if (col + 1 < BOARD_LENGTH) {
                    addGraphEdge(openGraph, position, encodeCellPosition(row, col + 1));
                }
            }
        }

        return openGraph;
    }


    private int[] computeDistancesFromRow(Map<Integer, Set<Integer>> localGraph, int startRow) {

        int[] distances = new int[BOARD_CELL_COUNT];
        Arrays.fill(distances, UNREACHABLE_DISTANCE);
        Queue<Integer> queue = new ArrayDeque<>();

        for (int col = 0; col < BOARD_LENGTH; col++) {
            int position = encodeCellPosition(startRow, col);
            distances[position] = 0;
            queue.add(position);
        }

        while (!queue.isEmpty()) {
            int position = queue.remove();
            for (int neighbourPosition : localGraph.get(position)) {
                if (distances[neighbourPosition] == UNREACHABLE_DISTANCE) {
                    distances[neighbourPosition] = distances[position] + 1;
                    queue.add(neighbourPosition);
                }
            }
        }

        return distances;
    }

    private int[] computeDistancesFromPosition(int startPosition) {
        int[] distances = new int[BOARD_CELL_COUNT];
        Arrays.fill(distances, UNREACHABLE_DISTANCE);
        Queue<Integer> queue = new ArrayDeque<>();

        distances[startPosition] = 0;
        queue.add(startPosition);

        while (!queue.isEmpty()) {
            int position = queue.remove();
            for (int neighbourPosition : graph.get(position)) {
                if (distances[neighbourPosition] == UNREACHABLE_DISTANCE) {
                    distances[neighbourPosition] = distances[position] + 1;
                    queue.add(neighbourPosition);
                }
            }
        }

        return distances;
    }

    private int[] distancesToFinish(int finishLine) {
        return finishLine == 0 ? topDistances : bottomDistances;
    }

    private void addJumpOrSideMoveCodes(Set<Integer> possibleMoveCodes, int movingPlayerPos, int blockingPlayerPos) {
        int movingRow = rowOf(movingPlayerPos);
        int movingCol = colOf(movingPlayerPos);
        int blockingRow = rowOf(blockingPlayerPos);
        int blockingCol = colOf(blockingPlayerPos);
        int rowDirection = blockingRow - movingRow;
        int colDirection = blockingCol - movingCol;
        int jumpRow = blockingRow + rowDirection;
        int jumpCol = blockingCol + colDirection;

        if (isInsideBoard(jumpRow, jumpCol)) {
            int jumpPosition = encodeCellPosition(jumpRow, jumpCol);
            if (graph.get(blockingPlayerPos).contains(jumpPosition)) {
                possibleMoveCodes.add(encodePawnMoveCode(jumpPosition));
                return;
            }
        }

        addSideMoveCodeIfPossible(possibleMoveCodes, blockingPlayerPos, blockingRow + colDirection, blockingCol + rowDirection);
        addSideMoveCodeIfPossible(possibleMoveCodes, blockingPlayerPos, blockingRow - colDirection, blockingCol - rowDirection);
    }

    private void addJumpOrSideMoveCodes(List<Integer> possibleMoveCodes, int movingPlayerPos, int blockingPlayerPos) {
        int movingRow = rowOf(movingPlayerPos);
        int movingCol = colOf(movingPlayerPos);
        int blockingRow = rowOf(blockingPlayerPos);
        int blockingCol = colOf(blockingPlayerPos);
        int rowDirection = blockingRow - movingRow;
        int colDirection = blockingCol - movingCol;
        int jumpRow = blockingRow + rowDirection;
        int jumpCol = blockingCol + colDirection;

        if (isInsideBoard(jumpRow, jumpCol)) {
            int jumpPosition = encodeCellPosition(jumpRow, jumpCol);
            if (graph.get(blockingPlayerPos).contains(jumpPosition)) {
                possibleMoveCodes.add(encodePawnMoveCode(jumpPosition));
                return;
            }
        }

        addSideMoveCodeIfPossible(possibleMoveCodes, blockingPlayerPos, blockingRow + colDirection, blockingCol + rowDirection);
        addSideMoveCodeIfPossible(possibleMoveCodes, blockingPlayerPos, blockingRow - colDirection, blockingCol - rowDirection);
    }

    private void addSideMoveCodeIfPossible(Set<Integer> possibleMoveCodes, int fromPosition, int row, int col) {
        if (!isInsideBoard(row, col)) {
            return;
        }

        int sidePosition = encodeCellPosition(row, col);
        if (graph.get(fromPosition).contains(sidePosition)) {
            possibleMoveCodes.add(encodePawnMoveCode(sidePosition));
        }
    }

    private void addSideMoveCodeIfPossible(List<Integer> possibleMoveCodes, int fromPosition, int row, int col) {
        if (!isInsideBoard(row, col)) {
            return;
        }

        int sidePosition = encodeCellPosition(row, col);
        if (graph.get(fromPosition).contains(sidePosition)) {
            possibleMoveCodes.add(encodePawnMoveCode(sidePosition));
        }
    }

    private boolean conflictsWithExistingWall(int wallCode) {
        for (int placedWall : placedWalls) {
            if (conflicts(wallCode, placedWall)) {
                return true;
            }
        }

        return false;
    }

    private boolean conflicts(int firstWallCode, int secondWallCode) {
        if (wallAnchor(firstWallCode) == wallAnchor(secondWallCode)) {
            return true;
        }

        if (isHorizontalWall(firstWallCode) != isHorizontalWall(secondWallCode)) {
            return false;
        }

        if (isHorizontalWall(firstWallCode)) {
            return decodeWallRow(firstWallCode) == decodeWallRow(secondWallCode)
                    && Math.abs(decodeWallCol(firstWallCode) - decodeWallCol(secondWallCode)) <= 1;
        }

        return decodeWallCol(firstWallCode) == decodeWallCol(secondWallCode)
                && Math.abs(decodeWallRow(firstWallCode) - decodeWallRow(secondWallCode)) <= 1;
    }

    private boolean bothPlayersHavePathAfterWallPlacement(int wallCode) {
        Map<Integer, Set<Integer>> graphAfterPlacement = copyGraph(graph);
        updateGraph(graphAfterPlacement, wallCode);

        int[] topDistancesAfterPlacement = computeDistancesFromRow(graphAfterPlacement, 0);
        int[] bottomDistancesAfterPlacement = computeDistancesFromRow(graphAfterPlacement, BOARD_LENGTH - 1);

        return distanceToFinish(currPlayerPos, currPlayerFinishLine, topDistancesAfterPlacement, bottomDistancesAfterPlacement)
                != UNREACHABLE_DISTANCE
                && distanceToFinish(opponentPos, opponentFinishLine, topDistancesAfterPlacement, bottomDistancesAfterPlacement)
                != UNREACHABLE_DISTANCE;
    }

    private int distanceToFinish(int position, int finishLine, int[] topDistances, int[] bottomDistances) {
        return finishLine == 0 ? topDistances[position] : bottomDistances[position];
    }

    private Map<Integer, Set<Integer>> copyGraph(Map<Integer, Set<Integer>> sourceGraph) {
        Map<Integer, Set<Integer>> graphCopy = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : sourceGraph.entrySet()) {
            graphCopy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return graphCopy;
    }

    private void addGraphEdge(Map<Integer, Set<Integer>> targetGraph, int firstCellPosition, int secondCellPosition) {
        targetGraph.get(firstCellPosition).add(secondCellPosition);
        targetGraph.get(secondCellPosition).add(firstCellPosition);
    }

    private void updateGraph(Map<Integer, Set<Integer>> targetGraph, int wallCode) {
        int row = decodeWallRow(wallCode);
        int col = decodeWallCol(wallCode);

        if (isHorizontalWall(wallCode)) {
            removeGraphEdge(targetGraph, encodeCellPosition(row, col), encodeCellPosition(row + 1, col));
            removeGraphEdge(targetGraph, encodeCellPosition(row, col + 1), encodeCellPosition(row + 1, col + 1));
        } else {
            removeGraphEdge(targetGraph, encodeCellPosition(row, col), encodeCellPosition(row, col + 1));
            removeGraphEdge(targetGraph, encodeCellPosition(row + 1, col), encodeCellPosition(row + 1, col + 1));
        }
    }

    private void removeGraphEdge(Map<Integer, Set<Integer>> targetGraph, int firstCellPosition, int secondCellPosition) {
        targetGraph.get(firstCellPosition).remove(secondCellPosition);
        targetGraph.get(secondCellPosition).remove(firstCellPosition);
    }


    private static boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < BOARD_LENGTH && col >= 0 && col < BOARD_LENGTH;
    }

    private static int rowOf(int position) {
        return position / BOARD_LENGTH;
    }

    private static int colOf(int position) {
        return position % BOARD_LENGTH;
    }

    private static int wallAnchor(int wallCode) {
        return wallCode / 2;
    }

    private static boolean isHorizontalWall(int wallCode) {
        return wallCode % 2 == 0;
    }

    public Set<Integer> getPlacedWalls() {
        return placedWalls;
    }
}
