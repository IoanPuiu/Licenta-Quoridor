package AI.MCTS;

import PerformanceModel.GameState;

import java.util.concurrent.ThreadLocalRandom;

public final class MctsState {
    public static final int BOARD_SIZE = 9;
    public static final int CELL_COUNT = BOARD_SIZE * BOARD_SIZE; // 81
    public static final int WALL_GRID_SIZE = BOARD_SIZE - 1;      // 8
    public static final int WALL_COUNT_PER_ORIENTATION = WALL_GRID_SIZE * WALL_GRID_SIZE; // 64

    private static final int NO_WINNER = -1;

    // Direcții pentru adjacencyMask[pos]
    private static final int UP = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 4;
    private static final int RIGHT = 8;

    private int currPlayerPos;
    private int opponentPos;

    private int currPlayerWalls;
    private int opponentWalls;

    private int currPlayerFinishLine;
    private int opponentFinishLine;
    private long horizontalWalls;
    private long verticalWalls;
    /*
    GRAF OPTIMIZAT

      adjacencyMask[pos] spune în ce direcții se poate merge din celula pos.

      Exemplu:
      adjacencyMask[40] = UP | DOWN | LEFT

     */
    private final int[] adjacencyMask;
    private final int[] topDistances;
    private final int[] bottomDistances;

    private boolean distancesDirty;

    private final int[] bfsQueue;

    private final int[] moveBuffer;
    private final int[] rolloutMoveBuffer;
    private final int[] wallCandidateBuffer;


    public MctsState(GameState state) {
        this.adjacencyMask = new int[CELL_COUNT];
        this.topDistances = new int[CELL_COUNT];
        this.bottomDistances = new int[CELL_COUNT];
        this.bfsQueue = new int[CELL_COUNT];

        this.moveBuffer = new int[256];
        this.rolloutMoveBuffer = new int[64];
        this.wallCandidateBuffer = new int[64];

        loadFromGameState(state);
    }

    public MctsState(MctsState other) {
        this.currPlayerPos = other.currPlayerPos;
        this.opponentPos = other.opponentPos;
        this.currPlayerWalls = other.currPlayerWalls;
        this.opponentWalls = other.opponentWalls;
        this.currPlayerFinishLine = other.currPlayerFinishLine;
        this.opponentFinishLine = other.opponentFinishLine;

        this.horizontalWalls = other.horizontalWalls;
        this.verticalWalls = other.verticalWalls;

        this.adjacencyMask = other.adjacencyMask.clone();
        this.topDistances = other.topDistances.clone();
        this.bottomDistances = other.bottomDistances.clone();

        this.distancesDirty = other.distancesDirty;

        this.bfsQueue = new int[CELL_COUNT];
        this.moveBuffer = new int[256];
        this.rolloutMoveBuffer = new int[64];
        this.wallCandidateBuffer = new int[64];
    }


    private void loadFromGameState(GameState state) {
        currPlayerPos = state.getCurrPlayerPos();
        opponentPos = state.getOpponentPos();
        currPlayerWalls = state.getCurrPlayerWalls();
        opponentWalls = state.getOpponentWalls();
        currPlayerFinishLine = state.getCurrPlayerFinishLine();
        opponentFinishLine = state.getOpponentFinishLine();
        horizontalWalls = 0L;
        verticalWalls = 0L;
        initializeFullAdjacencyGraph();

        for (int wallMoveCode : state.getPlacedWalls()) {
            int bitIndex = wallBitIndex(decodeWallRow(wallMoveCode), decodeWallCol(wallMoveCode));
            if (decodeWallOrientation(wallMoveCode)) {
                horizontalWalls |= 1L << bitIndex;
            } else {
                verticalWalls |= 1L << bitIndex;
            }
            removeEdgesBlockedByWall(wallMoveCode);
        }
        distancesDirty = true;
    }

    private void initializeFullAdjacencyGraph() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int mask = 0;

                if (row > 0) {
                    mask |= UP;
                }
                if (row < BOARD_SIZE - 1) {
                    mask |= DOWN;
                }
                if (col > 0) {
                    mask |= LEFT;
                }
                if (col < BOARD_SIZE - 1) {
                    mask |= RIGHT;
                }

                adjacencyMask[posOf(row, col)] = mask;
            }
        }
    }


    public void applyMove(int moveCode) {
        if (isPawnMoveCode(moveCode)) {
            applyPawnMove(moveCode);
        } else {
            applyWallMove(moveCode);
        }

        swapPlayers();
    }

    private void applyPawnMove(int moveCode) {
        currPlayerPos = posOf(decodePawnMoveRow(moveCode), decodePawnMoveCol(moveCode));
    }

    private void applyWallMove(int moveCode) {
        int bitIndex = wallBitIndex(decodeWallRow(moveCode), decodeWallCol(moveCode));
        long bit = 1L << bitIndex;

        if (decodeWallOrientation(moveCode)) {
            horizontalWalls |= bit;
        } else {
            verticalWalls |= bit;
        }
        currPlayerWalls--;
        removeEdgesBlockedByWall(moveCode);
        distancesDirty = true;
    }

    private void swapPlayers() {
        int oldCurrPlayerPos = currPlayerPos;
        currPlayerPos = opponentPos;
        opponentPos = oldCurrPlayerPos;

        int oldCurrPlayerWalls = currPlayerWalls;
        currPlayerWalls = opponentWalls;
        opponentWalls = oldCurrPlayerWalls;

        int oldCurrPlayerFinishLine = currPlayerFinishLine;
        currPlayerFinishLine = opponentFinishLine;
        opponentFinishLine = oldCurrPlayerFinishLine;
    }


    private void removeEdgesBlockedByWall(int wallMoveCode) {
        int row = decodeWallRow(wallMoveCode);
        int col = decodeWallCol(wallMoveCode);

        if (decodeWallOrientation(wallMoveCode)) {
            removeEdge(posOf(row, col), posOf(row + 1, col));
            removeEdge(posOf(row, col + 1), posOf(row + 1, col + 1));
            return;
        }

        removeEdge(posOf(row, col), posOf(row, col + 1));
        removeEdge(posOf(row + 1, col), posOf(row + 1, col + 1));
    }

    private void removeEdge(int a, int b) {
        if (b == a - BOARD_SIZE) {
            adjacencyMask[a] &= ~UP;
            adjacencyMask[b] &= ~DOWN;
            return;
        }
        if (b == a + BOARD_SIZE) {
            adjacencyMask[a] &= ~DOWN;
            adjacencyMask[b] &= ~UP;
            return;
        }
        if (b == a - 1) {
            adjacencyMask[a] &= ~LEFT;
            adjacencyMask[b] &= ~RIGHT;
            return;
        }
        if (b == a + 1) {
            adjacencyMask[a] &= ~RIGHT;
            adjacencyMask[b] &= ~LEFT;
        }
    }


    public boolean currentPlayerReachedFinishLine() {
        return rowOf(currPlayerPos) == currPlayerFinishLine;
    }

    public boolean opponentReachedFinishLine() {
        return rowOf(opponentPos) == opponentFinishLine;
    }

    public int winnerFinishLine() {
        if (currentPlayerReachedFinishLine()) {
            return currPlayerFinishLine;
        }

        if (opponentReachedFinishLine()) {
            return opponentFinishLine;
        }

        return NO_WINNER;
    }

    public boolean isTerminal() {
        return currentPlayerReachedFinishLine() || opponentReachedFinishLine();
    }

    private void ensureDistancesUpdated() {
        if (!distancesDirty) {
            return;
        }

        computeDistancesFromFinishLine(0, topDistances);
        computeDistancesFromFinishLine(BOARD_SIZE - 1, bottomDistances);

        distancesDirty = false;
    }

    private void computeDistancesFromFinishLine(int finishRow, int[] outputDistances) {
        for (int i = 0; i < CELL_COUNT; i++) {
            outputDistances[i] = -1;
        }

        int head = 0;
        int tail = 0;
        for (int col = 0; col < BOARD_SIZE; col++) {
            int pos = posOf(finishRow, col);
            outputDistances[pos] = 0;
            bfsQueue[tail++] = pos;
        }

        while (head < tail) {
            int pos = bfsQueue[head++];
            int nextDistance = outputDistances[pos] + 1;
            int mask = adjacencyMask[pos];

            if ((mask & UP) != 0) {
                int next = pos - BOARD_SIZE;
                if (outputDistances[next] == -1) {
                    outputDistances[next] = nextDistance;
                    bfsQueue[tail++] = next;
                }
            }
            if ((mask & DOWN) != 0) {
                int next = pos + BOARD_SIZE;
                if (outputDistances[next] == -1) {
                    outputDistances[next] = nextDistance;
                    bfsQueue[tail++] = next;
                }
            }
            if ((mask & LEFT) != 0) {
                int next = pos - 1;
                if (outputDistances[next] == -1) {
                    outputDistances[next] = nextDistance;
                    bfsQueue[tail++] = next;
                }
            }
            if ((mask & RIGHT) != 0) {
                int next = pos + 1;
                if (outputDistances[next] == -1) {
                    outputDistances[next] = nextDistance;
                    bfsQueue[tail++] = next;
                }
            }
        }
        // Arrays.fill(outputDistances, INF)
        // pune toate celulele de pe finishRow în bfsQueue
        // BFS folosind adjacencyMask
    }

    public int getCurrentPlayerDistanceToFinish() {
        ensureDistancesUpdated();

        if (currPlayerFinishLine == 0) {
            return topDistances[currPlayerPos];
        }

        return bottomDistances[currPlayerPos];
    }

    public int getOpponentDistanceToFinish() {
        ensureDistancesUpdated();

        if (opponentFinishLine == 0) {
            return topDistances[opponentPos];
        }

        return bottomDistances[opponentPos];
    }


    public int generatePawnMoves(int[] outputBuffer) {
        int count = 0;
        int currRow = rowOf(currPlayerPos);
        int currCol = colOf(currPlayerPos);
        int opponentRow = rowOf(opponentPos);
        int opponentCol = colOf(opponentPos);
        int mask = adjacencyMask[currPlayerPos];

        if ((mask & UP) != 0) {
            int next = currPlayerPos - BOARD_SIZE;
            if (next == opponentPos) {
                int rowDirection = opponentRow - currRow;
                int colDirection = opponentCol - currCol;
                int jumpRow = opponentRow + rowDirection;
                int jumpCol = opponentCol + colDirection;

                if (jumpRow >= 0 && jumpRow < BOARD_SIZE && jumpCol >= 0 && jumpCol < BOARD_SIZE
                        && (adjacencyMask[opponentPos] & UP) != 0) {
                    outputBuffer[count++] = encodePawnMove(jumpRow, jumpCol);
                } else {
                    int sideRow = opponentRow + colDirection;
                    int sideCol = opponentCol + rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & LEFT) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }

                    sideRow = opponentRow - colDirection;
                    sideCol = opponentCol - rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & RIGHT) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }
                }
            } else {
                outputBuffer[count++] = encodePawnMove(rowOf(next), colOf(next));
            }
        }

        if ((mask & DOWN) != 0) {
            int next = currPlayerPos + BOARD_SIZE;
            if (next == opponentPos) {
                int rowDirection = opponentRow - currRow;
                int colDirection = opponentCol - currCol;
                int jumpRow = opponentRow + rowDirection;
                int jumpCol = opponentCol + colDirection;

                if (jumpRow >= 0 && jumpRow < BOARD_SIZE && jumpCol >= 0 && jumpCol < BOARD_SIZE
                        && (adjacencyMask[opponentPos] & DOWN) != 0) {
                    outputBuffer[count++] = encodePawnMove(jumpRow, jumpCol);
                } else {
                    int sideRow = opponentRow + colDirection;
                    int sideCol = opponentCol + rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & RIGHT) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }

                    sideRow = opponentRow - colDirection;
                    sideCol = opponentCol - rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & LEFT) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }
                }
            } else {
                outputBuffer[count++] = encodePawnMove(rowOf(next), colOf(next));
            }
        }

        if ((mask & LEFT) != 0) {
            int next = currPlayerPos - 1;
            if (next == opponentPos) {
                int rowDirection = opponentRow - currRow;
                int colDirection = opponentCol - currCol;
                int jumpRow = opponentRow + rowDirection;
                int jumpCol = opponentCol + colDirection;

                if (jumpRow >= 0 && jumpRow < BOARD_SIZE && jumpCol >= 0 && jumpCol < BOARD_SIZE
                        && (adjacencyMask[opponentPos] & LEFT) != 0) {
                    outputBuffer[count++] = encodePawnMove(jumpRow, jumpCol);
                } else {
                    int sideRow = opponentRow + colDirection;
                    int sideCol = opponentCol + rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & UP) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }

                    sideRow = opponentRow - colDirection;
                    sideCol = opponentCol - rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & DOWN) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }
                }
            } else {
                outputBuffer[count++] = encodePawnMove(rowOf(next), colOf(next));
            }
        }

        if ((mask & RIGHT) != 0) {
            int next = currPlayerPos + 1;
            if (next == opponentPos) {
                int rowDirection = opponentRow - currRow;
                int colDirection = opponentCol - currCol;
                int jumpRow = opponentRow + rowDirection;
                int jumpCol = opponentCol + colDirection;

                if (jumpRow >= 0 && jumpRow < BOARD_SIZE && jumpCol >= 0 && jumpCol < BOARD_SIZE
                        && (adjacencyMask[opponentPos] & RIGHT) != 0) {
                    outputBuffer[count++] = encodePawnMove(jumpRow, jumpCol);
                } else {
                    int sideRow = opponentRow + colDirection;
                    int sideCol = opponentCol + rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & DOWN) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }

                    sideRow = opponentRow - colDirection;
                    sideCol = opponentCol - rowDirection;
                    if (sideRow >= 0 && sideRow < BOARD_SIZE && sideCol >= 0 && sideCol < BOARD_SIZE
                            && (adjacencyMask[opponentPos] & UP) != 0) {
                        outputBuffer[count++] = encodePawnMove(sideRow, sideCol);
                    }
                }
            } else {
                outputBuffer[count++] = encodePawnMove(rowOf(next), colOf(next));
            }
        }
        // calculează mutările normale
        // calculează săritura peste adversar
        // calculează mutările diagonale dacă este cazul
        // returnează count
        return count;
    }

    public int findImmediateWinningPawnMove() {
        int count = generatePawnMoves(rolloutMoveBuffer);

        for (int i = 0; i < count; i++) {
            int move = rolloutMoveBuffer[i];
            if (decodePawnMoveRow(move) == currPlayerFinishLine) {
                return move;
            }
        }

        return -1;
    }


    public int generateCandidateMoves(int[] outputBuffer, MctsSelectionHeuristic selectionHeuristic) {
        int count = 0;
        MctsSelectionHeuristic selectedHeuristic = selectionHeuristic == null
                ? MctsSelectionHeuristic.WALLS_NEAR_PAWNS
                : selectionHeuristic;

        count = appendPawnMoves(outputBuffer, count);
        count = switch (selectedHeuristic) {
            case WALLS_NEAR_PAWNS -> appendRelevantWallMoves(outputBuffer, count);
            case WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES -> appendWallMovesNearPawnsWallsEdges(outputBuffer, count);
        };

        return count;
    }

    private int appendPawnMoves(int[] outputBuffer, int count) {

        int pawnCount = generatePawnMoves(rolloutMoveBuffer);
        boolean findMoveOnShortestPath = false;

        ensureDistancesUpdated();
        int currentShortestDist = currPlayerFinishLine == 0 ?
                topDistances[currPlayerPos] :
                bottomDistances[currPlayerPos];

        for (int i = 0; i < pawnCount; i++) {

            int newShortestDist = currPlayerFinishLine == 0 ?
                    topDistances[rolloutMoveBuffer[i] - 200] :
                    bottomDistances[rolloutMoveBuffer[i] - 200];
            if (currentShortestDist - newShortestDist > 0) {
                outputBuffer[count++] = rolloutMoveBuffer[i];
                findMoveOnShortestPath = true;
            }

        }

        if (!findMoveOnShortestPath)

            for (int i = 0; i < pawnCount; i++) {
                outputBuffer[count++] = rolloutMoveBuffer[i];
            }


        return count;
    }

    private int appendRelevantWallMoves(int[] outputBuffer, int count) {
        if (currPlayerWalls == 0) {
            return count;
        }

        int candidateCount = 0;
        int opponentRow = rowOf(opponentPos);
        int opponentCol = colOf(opponentPos);
        int currRow = rowOf(currPlayerPos);
        int currCol = colOf(currPlayerPos);

        candidateCount = appendWallMovesNearPawn(candidateCount, opponentRow, opponentCol);
        candidateCount = appendWallMovesNearPawn(candidateCount, currRow, currCol);

        return appendBestScoredWallMoves(
                outputBuffer,
                count,
                candidateCount,
                opponentRow,
                opponentCol,
                currRow,
                currCol);
    }

    private int appendWallMovesNearPawnsWallsEdges(int[] outputBuffer, int count) {
        if (currPlayerWalls == 0) {
            return count;
        }

        int candidateCount = 0;
        int opponentRow = rowOf(opponentPos);
        int opponentCol = colOf(opponentPos);
        int currRow = rowOf(currPlayerPos);
        int currCol = colOf(currPlayerPos);

        candidateCount = appendWallMovesNearPawn(candidateCount, opponentRow, opponentCol);
        candidateCount = appendWallMovesNearPawn(candidateCount, currRow, currCol);
        candidateCount = appendHorizontalWallMovesOnSideEdges(candidateCount);
        candidateCount = appendWallMovesNearExistingWalls(candidateCount);

        return appendBestScoredWallMoves(
                outputBuffer,
                count,
                candidateCount,
                opponentRow,
                opponentCol,
                currRow,
                currCol);
    }

    private int appendWallMovesNearPawn(int candidateCount, int centerRow, int centerCol) {
        for (int row = centerRow - 1; row <= centerRow; row++) {
            for (int col = centerCol - 1; col <= centerCol; col++) {
                candidateCount = appendWallMovesAtAnchor(candidateCount, row, col);
                if (candidateCount >= wallCandidateBuffer.length) {
                    return candidateCount;
                }
            }
        }

        return candidateCount;
    }

    private int appendHorizontalWallMovesOnSideEdges(int candidateCount) {
        for (int row = 0; row < WALL_GRID_SIZE; row++) {
            candidateCount = appendWallCandidate(candidateCount, encodeWallMove(row, 0, true));
            candidateCount = appendWallCandidate(
                    candidateCount,
                    encodeWallMove(row, WALL_GRID_SIZE - 1, true));
            if (candidateCount >= wallCandidateBuffer.length) {
                return candidateCount;
            }
        }

        return candidateCount;
    }

    private int appendWallMovesNearExistingWalls(int candidateCount) {
        long existingWallSlots = horizontalWalls | verticalWalls;

        while (existingWallSlots != 0L && candidateCount < wallCandidateBuffer.length) {
            int bitIndex = Long.numberOfTrailingZeros(existingWallSlots);
            existingWallSlots &= existingWallSlots - 1;

            int wallRow = bitIndex / WALL_GRID_SIZE;
            int wallCol = bitIndex % WALL_GRID_SIZE;
            for (int row = wallRow - 1; row <= wallRow + 1; row++) {
                for (int col = wallCol - 1; col <= wallCol + 1; col++) {
                    if (row == wallRow && col == wallCol) {
                        continue;
                    }
                    candidateCount = appendWallMovesAtAnchor(candidateCount, row, col);
                    if (candidateCount >= wallCandidateBuffer.length) {
                        return candidateCount;
                    }
                }
            }

            boolean horizontal = (horizontalWalls & (1L << bitIndex)) != 0;
            if (horizontal) {
                candidateCount = appendWallMove(candidateCount, wallRow, wallCol - 2, true);
                candidateCount = appendWallMove(candidateCount, wallRow, wallCol + 2, true);
            } else {
                candidateCount = appendWallMove(candidateCount, wallRow - 2, wallCol, false);
                candidateCount = appendWallMove(candidateCount, wallRow + 2, wallCol, false);
            }
        }

        return candidateCount;
    }

    private int appendWallMovesAtAnchor(int candidateCount, int row, int col) {
        candidateCount = appendWallMove(candidateCount, row, col, true);
        return appendWallMove(candidateCount, row, col, false);
    }

    private int appendWallMove(int candidateCount, int row, int col, boolean horizontal) {
        if (row < 0 || row >= WALL_GRID_SIZE || col < 0 || col >= WALL_GRID_SIZE) {
            return candidateCount;
        }

        return appendWallCandidate(candidateCount, encodeWallMove(row, col, horizontal));
    }

    private int appendWallCandidate(int candidateCount, int move) {
        if (candidateCount >= wallCandidateBuffer.length) {
            return candidateCount;
        }

        int row = decodeWallRow(move);
        int col = decodeWallCol(move);
        if (row < 0 || row >= WALL_GRID_SIZE || col < 0 || col >= WALL_GRID_SIZE) {
            return candidateCount;
        }
        if (!isWallSlotFree(move)) {
            return candidateCount;
        }

        for (int i = 0; i < candidateCount; i++) {
            if (wallCandidateBuffer[i] == move) {
                return candidateCount;
            }
        }

        wallCandidateBuffer[candidateCount++] = move;
        return candidateCount;
    }

    private int appendBestScoredWallMoves(
            int[] outputBuffer,
            int count,
            int candidateCount,
            int opponentRow,
            int opponentCol,
            int currRow,
            int currCol
    ) {
        int currentDistanceBefore = getCurrentPlayerDistanceToFinish();
        int opponentDistanceBefore = getOpponentDistanceToFinish();
        int maxWalls = 8;

        for (int selected = 0; selected < maxWalls; selected++) {
            int bestIndex = -1;
            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < candidateCount; i++) {
                int move = wallCandidateBuffer[i];
                if (move == -1 || !isLegalWallMove(move)) {
                    continue;
                }

                int row = decodeWallRow(move);
                int col = decodeWallCol(move);
                int firstA;
                int firstB;
                int secondA;
                int secondB;

                if (decodeWallOrientation(move)) {
                    firstA = posOf(row, col);
                    firstB = posOf(row + 1, col);
                    secondA = posOf(row, col + 1);
                    secondB = posOf(row + 1, col + 1);
                } else {
                    firstA = posOf(row, col);
                    firstB = posOf(row, col + 1);
                    secondA = posOf(row + 1, col);
                    secondB = posOf(row + 1, col + 1);
                }

                int oldFirstA = adjacencyMask[firstA];
                int oldFirstB = adjacencyMask[firstB];
                int oldSecondA = adjacencyMask[secondA];
                int oldSecondB = adjacencyMask[secondB];
                long oldHorizontalWalls = horizontalWalls;
                long oldVerticalWalls = verticalWalls;

                if (decodeWallOrientation(move)) {
                    horizontalWalls |= 1L << wallBitIndex(row, col);
                } else {
                    verticalWalls |= 1L << wallBitIndex(row, col);
                }

                removeEdgesBlockedByWall(move);
                distancesDirty = true;

                int currentDistanceAfter = getCurrentPlayerDistanceToFinish();
                int opponentDistanceAfter = getOpponentDistanceToFinish();
                int score = 100 * (opponentDistanceAfter - opponentDistanceBefore)
                        - 110 * (currentDistanceAfter - currentDistanceBefore);

                if (score <= 0) continue;

                adjacencyMask[firstA] = oldFirstA;
                adjacencyMask[firstB] = oldFirstB;
                adjacencyMask[secondA] = oldSecondA;
                adjacencyMask[secondB] = oldSecondB;
                horizontalWalls = oldHorizontalWalls;
                verticalWalls = oldVerticalWalls;
                distancesDirty = true;

                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }

            if (bestIndex == -1) {
                break;
            }

            outputBuffer[count++] = wallCandidateBuffer[bestIndex];
            wallCandidateBuffer[bestIndex] = -1;
        }

        return count;
    }

    public int generateRolloutMoves(
            int[] outputBuffer,
            MctsRolloutHeuristic rolloutHeuristic,
            ThreadLocalRandom random) {
        int count = appendPawnMoves(outputBuffer, 0);
        MctsRolloutHeuristic selectedHeuristic = rolloutHeuristic == null
                ? MctsRolloutHeuristic.PAWN_MOVES
                : rolloutHeuristic;

        return switch (selectedHeuristic) {
            case PAWN_MOVES -> count;
            case PAWN_MOVES_RANDOM_WALLS -> appendRandomWallMoves(outputBuffer, count, random);
            case PAWN_MOVES_RELEVANT_WALLS -> appendRelevantWallMoves(outputBuffer, count);
        };
    }

    private int appendRandomWallMoves(int[] outputBuffer, int count, ThreadLocalRandom random) {
        if (currPlayerWalls == 0) {
            return count;
        }

        int addedWalls = 0;
        int attempts = 0;
        int maxWalls = 4;

        while (addedWalls < maxWalls && attempts < 32 && count < outputBuffer.length) {
            attempts++;
            int move = encodeWallMove(
                    random.nextInt(WALL_GRID_SIZE),
                    random.nextInt(WALL_GRID_SIZE),
                    random.nextBoolean());

            if (!isLegalWallMove(move) || containsMove(outputBuffer, count, move)) {
                continue;
            }

            outputBuffer[count++] = move;
            addedWalls++;
        }

        return count;
    }

    private boolean containsMove(int[] outputBuffer, int count, int move) {
        for (int i = 0; i < count; i++) {
            if (outputBuffer[i] == move) {
                return true;
            }
        }

        return false;
    }

    /*
     * Alege o mutare rapidă pentru rollout.
     */
    public int selectRolloutMove(ThreadLocalRandom random) {
        return selectRolloutMove(random, MctsRolloutHeuristic.PAWN_MOVES);
    }

    public int selectRolloutMove(ThreadLocalRandom random, MctsRolloutHeuristic rolloutHeuristic) {
        int winningMove = findImmediateWinningPawnMove();

        if (winningMove != -1) {
            return winningMove;
        }

        int count = generateRolloutMoves(rolloutMoveBuffer, rolloutHeuristic, random);

        if (count == 0) {
            return -1;
        }

        return rolloutMoveBuffer[random.nextInt(count)];
    }


    // ============================================================
    // 16. VALIDARE PEREȚI
    // ============================================================

    /*
     * Verifică rapid dacă un perete este ocupat sau se suprapune.
     */
    public boolean isWallSlotFree(int wallMoveCode) {
        int row = decodeWallRow(wallMoveCode);
        int col = decodeWallCol(wallMoveCode);

        if (row < 0 || row >= WALL_GRID_SIZE || col < 0 || col >= WALL_GRID_SIZE) {
            return false;
        }

        int bitIndex = wallBitIndex(row, col);
        long bit = 1L << bitIndex;
        boolean horizontal = decodeWallOrientation(wallMoveCode);

        if ((horizontalWalls & bit) != 0 || (verticalWalls & bit) != 0) {
            return false;
        }

        if (horizontal) {
            if (col > 0 && (horizontalWalls & (1L << wallBitIndex(row, col - 1))) != 0) {
                return false;
            }
            if (col < WALL_GRID_SIZE - 1 && (horizontalWalls & (1L << wallBitIndex(row, col + 1))) != 0) {
                return false;
            }
        } else {
            if (row > 0 && (verticalWalls & (1L << wallBitIndex(row - 1, col))) != 0) {
                return false;
            }
            if (row < WALL_GRID_SIZE - 1 && (verticalWalls & (1L << wallBitIndex(row + 1, col))) != 0) {
                return false;
            }
        }
        // verifică horizontalWalls / verticalWalls
        // verifică suprapunere
        // verifică intersectare
        return true;
    }

    /*
     * Verifică dacă peretele lasă drum valid ambilor jucători.
     *
     * Folosit doar pentru pereții candidați, nu pentru toate pozițiile posibile.
     */
    public boolean wallKeepsBothPlayersConnected(int wallMoveCode) {
        int row = decodeWallRow(wallMoveCode);
        int col = decodeWallCol(wallMoveCode);
        int firstA;
        int firstB;
        int secondA;
        int secondB;

        if (decodeWallOrientation(wallMoveCode)) {
            firstA = posOf(row, col);
            firstB = posOf(row + 1, col);
            secondA = posOf(row, col + 1);
            secondB = posOf(row + 1, col + 1);
        } else {
            firstA = posOf(row, col);
            firstB = posOf(row, col + 1);
            secondA = posOf(row + 1, col);
            secondB = posOf(row + 1, col + 1);
        }

        int oldFirstA = adjacencyMask[firstA];
        int oldFirstB = adjacencyMask[firstB];
        int oldSecondA = adjacencyMask[secondA];
        int oldSecondB = adjacencyMask[secondB];

        removeEdgesBlockedByWall(wallMoveCode);
        computeDistancesFromFinishLine(0, topDistances);
        computeDistancesFromFinishLine(BOARD_SIZE - 1, bottomDistances);

        boolean currentConnected = currPlayerFinishLine == 0
                ? topDistances[currPlayerPos] != -1
                : bottomDistances[currPlayerPos] != -1;
        boolean opponentConnected = opponentFinishLine == 0
                ? topDistances[opponentPos] != -1
                : bottomDistances[opponentPos] != -1;
        adjacencyMask[firstA] = oldFirstA;
        adjacencyMask[firstB] = oldFirstB;
        adjacencyMask[secondA] = oldSecondA;
        adjacencyMask[secondB] = oldSecondB;
        distancesDirty = true;
        // aplică temporar peretele
        // BFS doar pentru existența drumului
        // revine la starea anterioară
        return currentConnected && opponentConnected;
    }

    /*
     * Verifică dacă peretele este legal complet.
     */
    public boolean isLegalWallMove(int wallMoveCode) {
        return currPlayerWalls > 0
                && isWallSlotFree(wallMoveCode)
                && wallKeepsBothPlayersConnected(wallMoveCode);
    }


    // ============================================================
    // 17. EVALUARE EURISTICĂ
    // ============================================================

    /*
     * Evaluare rapidă pentru MCTS când rollout-ul se oprește înainte de final.
     *
     * Returnează valoare între 0 și 1 din perspectiva root player-ului.
     */
    public double evaluateForRoot(int rootFinishLine) {
        ensureDistancesUpdated();

        int rootDistance;
        int opponentDistance;
        int rootWalls;
        int opponentWalls;

        if (currPlayerFinishLine == rootFinishLine) {
            rootDistance = getCurrentPlayerDistanceToFinish();
            opponentDistance = getOpponentDistanceToFinish();
            rootWalls = currPlayerWalls;
            opponentWalls = this.opponentWalls;
        } else {
            rootDistance = getOpponentDistanceToFinish();
            opponentDistance = getCurrentPlayerDistanceToFinish();
            rootWalls = this.opponentWalls;
            opponentWalls = currPlayerWalls;
        }

        return evaluateDistancesAndWalls(rootDistance, opponentDistance, rootWalls, opponentWalls);
    }

    /*
     * Transformă distanțele și pereții într-un scor 0..1.
     */
    private double evaluateDistancesAndWalls(
            int rootDistance,
            int opponentDistance,
            int rootWalls,
            int opponentWalls) {
        // scor gradual, nu doar 0 / 0.5 / 1
        if (rootDistance == 0) {
            return 1.0;
        }
        if (opponentDistance == 0) {
            return 0.0;
        }

        double distanceScore = (opponentDistance - rootDistance) / (double) (2 * BOARD_SIZE);
        double wallScore = 0.02 * (rootWalls - opponentWalls);
        double score = 0.5 + distanceScore + wallScore;

        if (score < 0.0) {
            return 0.0;
        }
        if (score > 1.0) {
            return 1.0;
        }
        return score;
    }


    // ============================================================
    // 18. HELPERS POZIȚII
    // ============================================================

    private static int rowOf(int pos) {
        return pos / BOARD_SIZE;
    }

    private static int colOf(int pos) {
        return pos % BOARD_SIZE;
    }

    private static int posOf(int row, int col) {
        return row * BOARD_SIZE + col;
    }


    // ============================================================
    // 19. HELPERS MUTĂRI
    // ============================================================

    public static boolean isPawnMoveCode(int moveCode) {
        // returnează true dacă moveCode reprezintă mutare de pion
        return moveCode >= GameState.PAWN_MOVE_CODE_OFFSET;
    }

    public static int encodePawnMove(int row, int col) {
        // codifică o mutare de pion
        return GameState.PAWN_MOVE_CODE_OFFSET + posOf(row, col);
    }

    public static int decodePawnMoveRow(int moveCode) {
        // extrage rândul din moveCode
        return (moveCode - GameState.PAWN_MOVE_CODE_OFFSET) / BOARD_SIZE;
    }

    public static int decodePawnMoveCol(int moveCode) {
        // extrage coloana din moveCode
        return (moveCode - GameState.PAWN_MOVE_CODE_OFFSET) % BOARD_SIZE;
    }

    public static int encodeWallMove(int row, int col, boolean horizontal) {
        // codifică o mutare de perete
        return wallBitIndex(row, col) * 2 + (horizontal ? 0 : 1);
    }

    public static int decodeWallRow(int moveCode) {
        return (moveCode / 2) / WALL_GRID_SIZE;
    }

    public static int decodeWallCol(int moveCode) {
        return (moveCode / 2) % WALL_GRID_SIZE;
    }

    public static boolean decodeWallOrientation(int moveCode) {
        // true = horizontal, false = vertical
        return moveCode % 2 == 0;
    }

    private static int wallBitIndex(int row, int col) {
        return row * WALL_GRID_SIZE + col;
    }


    // ============================================================
    // 20. GETTERE MINIME
    // ============================================================

    public int getCurrPlayerFinishLine() {
        return currPlayerFinishLine;
    }

    public int getOpponentFinishLine() {
        return opponentFinishLine;
    }

    public int getCurrPlayerWalls() {
        return currPlayerWalls;
    }

    public int getOpponentWalls() {
        return opponentWalls;
    }

    public int getCurrPlayerPos() {
        return currPlayerPos;
    }

    public int getOpponentPos() {
        return opponentPos;
    }
}
