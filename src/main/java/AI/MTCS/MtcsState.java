package AI.MTCS;

import PerformanceModel.GameState;

import java.util.concurrent.ThreadLocalRandom;

public final class MtcsState {

    // ============================================================
    // 1. CONSTANTE TABLĂ
    // ============================================================

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


    // ============================================================
    // 2. STARE JUCĂTORI
    // ============================================================

    private int currPlayerPos;
    private int opponentPos;

    private int currPlayerWalls;
    private int opponentWalls;

    private int currPlayerFinishLine;
    private int opponentFinishLine;


    // ============================================================
    // 3. PEREȚI
    // ============================================================

    /*
     * Pereții sunt reprezentați ca bitset-uri.
     *
     * Pentru tablă 9x9 există 8x8 poziții posibile pentru fiecare orientare.
     * Deci avem 64 pereți orizontali și 64 verticali.
     *
     * Un long are 64 biți, deci este perfect.
     */
    private long horizontalWalls;
    private long verticalWalls;


    // ============================================================
    // 4. GRAF OPTIMIZAT
    // ============================================================

    /*
     * adjacencyMask[pos] spune în ce direcții se poate merge din celula pos.
     *
     * Exemplu:
     * adjacencyMask[40] = UP | DOWN | LEFT
     *
     * Înseamnă că din celula 40 poți merge sus, jos și stânga,
     * dar nu dreapta.
     */
    private final int[] adjacencyMask;


    // ============================================================
    // 5. DISTANȚE CĂTRE LINIILE DE FINISH
    // ============================================================

    /*
     * topDistances[pos] = distanța minimă de la pos până la linia 0.
     * bottomDistances[pos] = distanța minimă de la pos până la linia 8.
     *
     * Se recalculează doar la apel ensureDistancesUpdated().
     */
    private final int[] topDistances;
    private final int[] bottomDistances;

    private boolean distancesDirty;


    // ============================================================
    // 6. BUFFER-E INTERNE PENTRU PERFORMANȚĂ
    // ============================================================

    /*
     * Folosite pentru BFS fără să aloci Queue, LinkedList, HashSet etc.
     */
    private final int[] bfsQueue;

    /*
     * Folosite pentru generarea mutărilor fără Set/List.
     * Metodele vor returna numărul de mutări scrise în buffer.
     */
    private final int[] moveBuffer;
    private final int[] rolloutMoveBuffer;
    private final int[] wallCandidateBuffer;


    // ============================================================
    // 7. CONSTRUCTORI
    // ============================================================

    /*
     * Creează o stare optimizată pornind de la GameState-ul normal.
     */
    public MtcsState(GameState state) {
        this.adjacencyMask = new int[CELL_COUNT];
        this.topDistances = new int[CELL_COUNT];
        this.bottomDistances = new int[CELL_COUNT];
        this.bfsQueue = new int[CELL_COUNT];

        this.moveBuffer = new int[256];
        this.rolloutMoveBuffer = new int[16];
        this.wallCandidateBuffer = new int[64];

        loadFromGameState(state);
    }

    /*
     * Constructor de copiere rapidă.
     * Folosit dacă păstrezi câte o stare în fiecare nod MCTS.
     */
    public MtcsState(MtcsState other) {
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
        this.rolloutMoveBuffer = new int[16];
        this.wallCandidateBuffer = new int[64];
    }


    // ============================================================
    // 8. INIȚIALIZARE
    // ============================================================

    /*
     * Copiază informațiile esențiale din GameState-ul tău actual.
     */
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
        // setează poziții, pereți disponibili, finish lines
        // construiește horizontalWalls / verticalWalls
        // inițializează adjacencyMask
        // elimină muchiile blocate de pereții deja existenți
        // marchează distancesDirty = true
    }

    /*
     * Construiește graful inițial complet al tablei fără pereți.
     */
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
        // pentru fiecare celulă setează UP/DOWN/LEFT/RIGHT dacă sunt valide
    }


    // ============================================================
    // 9. UPDATE STARE
    // ============================================================

    /*
     * Aplică o mutare și apoi schimbă jucătorii.
     */
    public void applyMove(int moveCode) {
        if (isPawnMoveCode(moveCode)) {
            applyPawnMove(moveCode);
        } else {
            applyWallMove(moveCode);
        }

        swapPlayers();
    }

    /*
     * Aplică doar mutarea pionului.
     * Nu modifică graful.
     * Nu recalculează distanțele.
     */
    private void applyPawnMove(int moveCode) {
        currPlayerPos = posOf(decodePawnMoveRow(moveCode), decodePawnMoveCol(moveCode));
        // currPlayerPos = poziția decodată din moveCode
    }

    /*
     * Aplică un perete.
     * Nu reconstruiește tot graful.
     * Elimină doar cele două muchii afectate.
     */
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
        // setează bitul în horizontalWalls sau verticalWalls
        // scade currPlayerWalls
        // removeEdgesBlockedByWall(moveCode)
        // distancesDirty = true
    }

    /*
     * Schimbă perspectiva jucătorilor.
     * După fiecare mutare, current player devine opponent și invers.
     */
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
        // swap currPlayerPos cu opponentPos
        // swap currPlayerWalls cu opponentWalls
        // swap currPlayerFinishLine cu opponentFinishLine
    }


    // ============================================================
    // 10. UPDATE INCREMENTAL AL GRAFULUI
    // ============================================================

    /*
     * Elimină din adjacencyMask cele două muchii blocate de perete.
     */
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
        // decode wall row, col, orientation
        // pentru perete orizontal: elimină două muchii verticale
        // pentru perete vertical: elimină două muchii orizontale
    }

    /*
     * Elimină muchia bidirecțională dintre două celule.
     */
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
        // modifică adjacencyMask[a]
        // modifică adjacencyMask[b]
    }


    // ============================================================
    // 11. VERIFICARE TERMINALĂ RAPIDĂ
    // ============================================================

    /*
     * Verifică dacă jucătorul curent a câștigat.
     * Nu folosește BFS.
     */
    public boolean currentPlayerReachedFinishLine() {
        return rowOf(currPlayerPos) == currPlayerFinishLine;
    }

    /*
     * Verifică dacă adversarul a câștigat.
     * Nu folosește BFS.
     */
    public boolean opponentReachedFinishLine() {
        return rowOf(opponentPos) == opponentFinishLine;
    }

    /*
     * Returnează linia de finish a câștigătorului sau NO_WINNER.
     */
    public int winnerFinishLine() {
        if (currentPlayerReachedFinishLine()) {
            return currPlayerFinishLine;
        }

        if (opponentReachedFinishLine()) {
            return opponentFinishLine;
        }

        return NO_WINNER;
    }

    /*
     * Spune dacă starea este terminală.
     */
    public boolean isTerminal() {
        return currentPlayerReachedFinishLine() || opponentReachedFinishLine();
    }


    // ============================================================
    // 12. DISTANȚE LA FINISH
    // ============================================================

    /*
     * Recalculează topDistances și bottomDistances doar dacă este nevoie.
     */
    private void ensureDistancesUpdated() {
        if (!distancesDirty) {
            return;
        }

        computeDistancesFromFinishLine(0, topDistances);
        computeDistancesFromFinishLine(BOARD_SIZE - 1, bottomDistances);

        distancesDirty = false;
    }

    /*
     * BFS rapid dintr-o linie de finish către toate celulele.
     * Nu folosește Queue, Set, Map.
     */
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

    /*
     * Distanța jucătorului curent până la finish.
     */
    public int getCurrentPlayerDistanceToFinish() {
        ensureDistancesUpdated();

        if (currPlayerFinishLine == 0) {
            return topDistances[currPlayerPos];
        }

        return bottomDistances[currPlayerPos];
    }

    /*
     * Distanța adversarului până la finish.
     */
    public int getOpponentDistanceToFinish() {
        ensureDistancesUpdated();

        if (opponentFinishLine == 0) {
            return topDistances[opponentPos];
        }

        return bottomDistances[opponentPos];
    }


    // ============================================================
    // 13. GENERARE MUTĂRI PION
    // ============================================================

    /*
     * Generează doar mutările pionului pentru jucătorul curent.
     *
     * Scrie mutările în outputBuffer.
     * Returnează câte mutări a scris.
     *
     * Important:
     * Nu alocă List.
     * Nu alocă Set.
     */
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

    /*
     * Returnează o mutare câștigătoare de pion, dacă există.
     * Dacă nu există, returnează -1.
     */
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


    // ============================================================
    // 14. GENERARE MUTĂRI PENTRU MCTS
    // ============================================================

    /*
     * Generează mutările candidate pentru nodurile MCTS.
     *
     * Recomandare:
     * - toate mutările de pion
     * - doar cei mai relevanți pereți
     *
     * Nu este obligatoriu să întoarcă toate mutările legale.
     * Pentru MCTS, este mai important să fie rapid și relevant.
     */
    public int generateCandidateMoves(int[] outputBuffer) {
        int count = 0;

        count = appendPawnMoves(outputBuffer, count);
        count = appendRelevantWallMoves(outputBuffer, count);

        return count;
    }

    /*
     * Adaugă mutările pionului în buffer.
     */
    private int appendPawnMoves(int[] outputBuffer, int count) {
        int pawnCount = generatePawnMoves(rolloutMoveBuffer);

        for (int i = 0; i < pawnCount; i++) {
            outputBuffer[count++] = rolloutMoveBuffer[i];
        }

        return count;
    }

    /*
     * Adaugă doar pereți relevanți.
     *
     * Exemple de pereți relevanți:
     * - lângă drumul minim al adversarului
     * - lângă adversar
     * - pereți care cresc distanța adversarului
     * - pereți care nu cresc mult distanța proprie
     */
    private int appendRelevantWallMoves(int[] outputBuffer, int count) {
        if (currPlayerWalls == 0) {
            return count;
        }

        int candidateCount = 0;
        int opponentRow = rowOf(opponentPos);
        int opponentCol = colOf(opponentPos);
        int currRow = rowOf(currPlayerPos);
        int currCol = colOf(currPlayerPos);

        for (int centerIndex = 0; centerIndex < 2; centerIndex++) {
            int centerRow = centerIndex == 0 ? opponentRow : currRow;
            int centerCol = centerIndex == 0 ? opponentCol : currCol;

            for (int row = centerRow - 1; row <= centerRow; row++) {
                for (int col = centerCol - 1; col <= centerCol; col++) {
                    if (row < 0 || row >= WALL_GRID_SIZE || col < 0 || col >= WALL_GRID_SIZE) {
                        continue;
                    }

                    int horizontalMove = encodeWallMove(row, col, true);
                    int verticalMove = encodeWallMove(row, col, false);
                    boolean horizontalFound = false;
                    boolean verticalFound = false;

                    for (int i = 0; i < candidateCount; i++) {
                        if (wallCandidateBuffer[i] == horizontalMove) {
                            horizontalFound = true;
                        }
                        if (wallCandidateBuffer[i] == verticalMove) {
                            verticalFound = true;
                        }
                    }

                    if (!horizontalFound && candidateCount < wallCandidateBuffer.length) {
                        wallCandidateBuffer[candidateCount++] = horizontalMove;
                    }
                    if (!verticalFound && candidateCount < wallCandidateBuffer.length) {
                        wallCandidateBuffer[candidateCount++] = verticalMove;
                    }
                }
            }
        }

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
                int distanceToOpponent = Math.abs(row - opponentRow) + Math.abs(col - opponentCol);
                int distanceToCurrent = Math.abs(row - currRow) + Math.abs(col - currCol);
                int score = 100 * (opponentDistanceAfter - opponentDistanceBefore)
                        - 70 * (currentDistanceAfter - currentDistanceBefore)
                        + 8 * (4 - distanceToOpponent)
                        - 3 * (4 - distanceToCurrent);

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

        // generează candidați în wallCandidateBuffer
        // filtrează legalitatea
        // păstrează top N pereți după scor euristic

        return count;
    }


    // ============================================================
    // 15. GENERARE MUTĂRI PENTRU ROLLOUT
    // ============================================================

    /*
     * Generează mutări foarte rapide pentru rollout.
     *
     * Recomandare:
     * - în principal mutări de pion
     * - eventual 1-3 pereți foarte relevanți
     *
     * Scopul este viteza, nu exhaustivitatea.
     */
    public int generateRolloutMoves(int[] outputBuffer) {
        return generatePawnMoves(outputBuffer);
    }

    /*
     * Alege o mutare rapidă pentru rollout.
     */
    public int selectRolloutMove(ThreadLocalRandom random) {
        int winningMove = findImmediateWinningPawnMove();

        if (winningMove != -1) {
            return winningMove;
        }

        int count = generateRolloutMoves(rolloutMoveBuffer);

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
