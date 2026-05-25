package AI;

import PerformanceModel.GameState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class MiniMax implements Algorithm {
    public enum MoveOrdering {
        NONE("No Move Ordering", ""),
        FAST("Fast Move Ordering", "F"),
        PRECISE("Precise Move Ordering", "P");

        private final String label;
        private final String compactSuffix;

        MoveOrdering(String label, String compactSuffix) {
            this.label = label;
            this.compactSuffix = compactSuffix;
        }

        public String label() {
            return label;
        }

        public String compactSuffix() {
            return compactSuffix;
        }
    }

    private static final int DEFAULT_DEPTH = 2;
    private static final MoveOrdering DEFAULT_MOVE_ORDERING = MoveOrdering.NONE;
    private static final int WIN_SCORE = 1_000_000;
    private static final int INFINITY = 2_000_000;

    private final int maxDepth;
    private final MoveOrdering moveOrdering;

    public MiniMax() {
        this(DEFAULT_DEPTH, DEFAULT_MOVE_ORDERING);
    }

    public MiniMax(int maxDepth) {
        this(maxDepth, DEFAULT_MOVE_ORDERING);
    }

    public MiniMax(int maxDepth, MoveOrdering moveOrdering) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("MiniMax depth must be at least 1.");
        }
        this.maxDepth = maxDepth;
        this.moveOrdering = moveOrdering == null ? DEFAULT_MOVE_ORDERING : moveOrdering;
    }

    @Override
    public int generateMove(GameState state) {
        Set<Integer> legalMoves = strategicMoves(state);
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal moves available.");
        }

        int bestMove = legalMoves.iterator().next();
        int bestScore = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;

        for (int move : orderedMoves(state, legalMoves)) {
            GameState child = new GameState(state);
            child.update(move);

            int score = -minimax(child, maxDepth - 1, -beta, -alpha);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            alpha = Math.max(alpha, bestScore);
        }

        return bestMove;
    }

    private int minimax(GameState state, int depth, int alpha, int beta) {
        int terminalScore = terminalScore(state, depth);
        if (terminalScore != 0) {
            return terminalScore;
        }

        if (depth == 0) {
            return evaluate(state);
        }

        Set<Integer> legalMoves = strategicMoves(state);
        if (legalMoves.isEmpty()) {
            return evaluate(state);
        }

        int bestScore = -INFINITY;
        for (int move : orderedMoves(state, legalMoves)) {
            GameState child = new GameState(state);
            child.update(move);

            int score = -minimax(child, depth - 1, -beta, -alpha);
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);

            if (alpha >= beta) {
                break;
            }
        }

        return bestScore;
    }

    private Set<Integer> strategicMoves(GameState state) {
        if (state.getCurrPlayerWalls() > 0) {
            return state.getAllPossibleMoveCodes();
        }

        Set<Integer> pawnMoves = state.getPossiblePawnMoveCodes();
        if (pawnMoves.isEmpty()) {
            return pawnMoves;
        }

        return Set.of(shortestPathPawnMove(state, pawnMoves));
    }

    private int shortestPathPawnMove(GameState state, Set<Integer> pawnMoves) {
        return pawnMoves.stream()
                .min(Comparator.comparingInt((Integer move) -> distanceAfterPawnMove(state, move))
                        .thenComparingInt(Integer::intValue))
                .orElseThrow();
    }

    private int distanceAfterPawnMove(GameState state, int move) {
        GameState child = new GameState(state);
        child.update(move);
        return child.getOpponentDistanceToFinish();
    }

    private List<Integer> orderedMoves(GameState state, Set<Integer> legalMoves) {
        List<Integer> moves = new ArrayList<>(legalMoves);
        if (moveOrdering == MoveOrdering.NONE) {
            return moves;
        }
        if (moves.size() < 2) {
            return moves;
        }

        if (moveOrdering == MoveOrdering.PRECISE) {
            moves.sort(Comparator.comparingInt((Integer move) -> preciseMoveOrderScore(state, move)).reversed());
            return moves;
        }

        MoveOrderContext context = new MoveOrderContext(state);
        moves.sort(Comparator.comparingInt((Integer move) -> fastMoveOrderScore(context, move))
                .reversed()
                .thenComparingInt(Integer::intValue));
        return moves;
    }

    private int preciseMoveOrderScore(GameState state, int move) {
        GameState child = new GameState(state);
        child.update(move);

        int terminalScore = terminalScore(child, 0);
        if (terminalScore != 0) {
            return -terminalScore;
        }

        return -evaluate(child);
    }

    private int fastMoveOrderScore(MoveOrderContext context, int move) {
        int score = 0;

        if (moveWinsImmediately(context, move)) {
            score += 1_000_000;
        }
        if (moveBlocksOpponentImmediateWin(context, move)) {
            score += 500_000;
        }

        if (GameState.isPawnMoveCode(move)) {
            int newPosition = GameState.decodePawnMovePosition(move);
            int newDist = context.currentFinishDistances[newPosition];
            score += 100 * (context.currentDist - newDist) - 20;
            return score;
        }

        if (wallTouchesOpponentShortestPath(context, move)) {
            score += 300;
        }
        if (wallNearOpponentPawn(context, move)) {
            score += 80;
        }
        if (wallNearCurrentPlayerPath(context, move)) {
            score -= 80;
        }

        return score;
    }

    private boolean moveWinsImmediately(MoveOrderContext context, int move) {
        return GameState.isPawnMoveCode(move)
                && context.currentFinishDistances[GameState.decodePawnMovePosition(move)] == 0;
    }

    private boolean moveBlocksOpponentImmediateWin(MoveOrderContext context, int move) {
        if (!context.opponentCanWinImmediately) {
            return false;
        }

        if (GameState.isPawnMoveCode(move)) {
            int newPosition = GameState.decodePawnMovePosition(move);
            if (!context.opponentImmediateWinTargets[newPosition]) {
                return false;
            }
            for (ImmediateWinThreat threat : context.opponentImmediateWinThreats) {
                if (threat.targetPosition() != newPosition) {
                    return false;
                }
            }
            return true;
        }

        for (ImmediateWinThreat threat : context.opponentImmediateWinThreats) {
            if (!wallBlocksThreat(move, threat)) {
                return false;
            }
        }
        return true;
    }

    private boolean wallTouchesOpponentShortestPath(MoveOrderContext context, int wallCode) {
        return wallTouchesShortestPath(
                wallCode,
                context.opponentStartDistances,
                context.opponentFinishDistances,
                context.opponentDist);
    }

    private boolean wallNearCurrentPlayerPath(MoveOrderContext context, int wallCode) {
        return wallTouchesShortestPath(
                wallCode,
                context.currentStartDistances,
                context.currentFinishDistances,
                context.currentDist);
    }

    private boolean wallTouchesShortestPath(int wallCode, int[] startDistances, int[] finishDistances, int totalDistance) {
        int row = GameState.decodeWallRow(wallCode);
        int col = GameState.decodeWallCol(wallCode);

        if (GameState.decodeWallIsHorizontal(wallCode)) {
            return edgeOnShortestPath(position(row, col), position(row + 1, col), startDistances, finishDistances, totalDistance)
                    || edgeOnShortestPath(position(row, col + 1), position(row + 1, col + 1), startDistances, finishDistances, totalDistance);
        }

        return edgeOnShortestPath(position(row, col), position(row, col + 1), startDistances, finishDistances, totalDistance)
                || edgeOnShortestPath(position(row + 1, col), position(row + 1, col + 1), startDistances, finishDistances, totalDistance);
    }

    private boolean edgeOnShortestPath(
            int firstPosition,
            int secondPosition,
            int[] startDistances,
            int[] finishDistances,
            int totalDistance) {
        return directedEdgeOnShortestPath(firstPosition, secondPosition, startDistances, finishDistances, totalDistance)
                || directedEdgeOnShortestPath(secondPosition, firstPosition, startDistances, finishDistances, totalDistance);
    }

    private boolean directedEdgeOnShortestPath(
            int fromPosition,
            int toPosition,
            int[] startDistances,
            int[] finishDistances,
            int totalDistance) {
        return startDistances[fromPosition] >= 0
                && finishDistances[toPosition] >= 0
                && startDistances[fromPosition] + 1 + finishDistances[toPosition] == totalDistance;
    }

    private boolean wallNearOpponentPawn(MoveOrderContext context, int wallCode) {
        int wallRow = GameState.decodeWallRow(wallCode);
        int wallCol = GameState.decodeWallCol(wallCode);
        int opponentRow = rowOf(context.state.getOpponentPos());
        int opponentCol = colOf(context.state.getOpponentPos());

        return opponentRow >= wallRow
                && opponentRow <= wallRow + 1
                && opponentCol >= wallCol
                && opponentCol <= wallCol + 1;
    }

    private boolean wallBlocksThreat(int wallCode, ImmediateWinThreat threat) {
        return wallBlocksEdge(wallCode, threat.firstEdge())
                || threat.secondEdge() != null && wallBlocksEdge(wallCode, threat.secondEdge());
    }

    private boolean wallBlocksEdge(int wallCode, Edge edge) {
        int row = GameState.decodeWallRow(wallCode);
        int col = GameState.decodeWallCol(wallCode);

        if (GameState.decodeWallIsHorizontal(wallCode)) {
            return sameEdge(edge, position(row, col), position(row + 1, col))
                    || sameEdge(edge, position(row, col + 1), position(row + 1, col + 1));
        }

        return sameEdge(edge, position(row, col), position(row, col + 1))
                || sameEdge(edge, position(row + 1, col), position(row + 1, col + 1));
    }

    private boolean sameEdge(Edge edge, int firstPosition, int secondPosition) {
        return edge.firstPosition() == firstPosition && edge.secondPosition() == secondPosition
                || edge.firstPosition() == secondPosition && edge.secondPosition() == firstPosition;
    }

    private Edge[] requiredEdgesForOpponentPawnMove(GameState state, int move) {
        int targetPosition = GameState.decodePawnMovePosition(move);
        int opponentPosition = state.getOpponentPos();
        int currentPosition = state.getCurrPlayerPos();

        if (areAdjacent(opponentPosition, targetPosition)) {
            return new Edge[]{new Edge(opponentPosition, targetPosition)};
        }

        if (areAdjacent(opponentPosition, currentPosition) && areAdjacent(currentPosition, targetPosition)) {
            return new Edge[]{new Edge(opponentPosition, currentPosition), new Edge(currentPosition, targetPosition)};
        }

        return new Edge[0];
    }

    private boolean areAdjacent(int firstPosition, int secondPosition) {
        int rowDiff = Math.abs(rowOf(firstPosition) - rowOf(secondPosition));
        int colDiff = Math.abs(colOf(firstPosition) - colOf(secondPosition));
        return rowDiff + colDiff == 1;
    }

    private int position(int row, int col) {
        return row * GameState.BOARD_LENGTH + col;
    }

    private final class MoveOrderContext {
        private final GameState state;
        private final int currentDist;
        private final int opponentDist;
        private final int[] currentStartDistances;
        private final int[] opponentStartDistances;
        private final int[] currentFinishDistances;
        private final int[] opponentFinishDistances;
        private final boolean[] opponentImmediateWinTargets;
        private final List<ImmediateWinThreat> opponentImmediateWinThreats;
        private final boolean opponentCanWinImmediately;

        private MoveOrderContext(GameState state) {
            this.state = state;
            currentDist = state.getCurrentPlayerDistanceToFinish();
            opponentDist = state.getOpponentDistanceToFinish();
            currentStartDistances = state.computeDistancesFromCurrentPlayer();
            opponentStartDistances = state.computeDistancesFromOpponent();
            currentFinishDistances = state.getCurrentPlayerDistancesToFinishByPosition();
            opponentFinishDistances = state.getOpponentDistancesToFinishByPosition();
            opponentImmediateWinTargets = new boolean[GameState.BOARD_CELL_COUNT];
            opponentImmediateWinThreats = new ArrayList<>();

            for (int opponentMove : state.getOpponentPossiblePawnMoveCodes()) {
                int targetPosition = GameState.decodePawnMovePosition(opponentMove);
                if (opponentFinishDistances[targetPosition] == 0) {
                    Edge[] requiredEdges = requiredEdgesForOpponentPawnMove(state, opponentMove);
                    if (requiredEdges.length == 1) {
                        opponentImmediateWinTargets[targetPosition] = true;
                        opponentImmediateWinThreats.add(new ImmediateWinThreat(targetPosition, requiredEdges[0], null));
                    } else if (requiredEdges.length == 2) {
                        opponentImmediateWinTargets[targetPosition] = true;
                        opponentImmediateWinThreats.add(new ImmediateWinThreat(targetPosition, requiredEdges[0], requiredEdges[1]));
                    }
                }
            }

            opponentCanWinImmediately = !opponentImmediateWinThreats.isEmpty();
        }
    }

    private record ImmediateWinThreat(int targetPosition, Edge firstEdge, Edge secondEdge) {
    }

    private record Edge(int firstPosition, int secondPosition) {
    }

    private int terminalScore(GameState state, int depth) {
        if (state.getCurrentPlayerDistanceToFinish() == 0) {
            return WIN_SCORE + depth;
        }
        if (state.getOpponentDistanceToFinish() == 0) {
            return -WIN_SCORE - depth;
        }
        return 0;
    }

    private int evaluate(GameState state) {

        int currentPlayerDist = state.getCurrentPlayerDistanceToFinish();
        int oppDist = state.getOpponentDistanceToFinish();
        int diff = oppDist - currentPlayerDist;

        int currentPlayerManhattanDistanceToGoal = distanceToGoalLine(state.getCurrPlayerPos(), state.getCurrPlayerFinishLine());
        int oppManhattanDistanceToGoal = distanceToGoalLine(state.getOpponentPos(), state.getOpponentFinishLine());
        int currentPlayerDetour = currentPlayerDist - currentPlayerManhattanDistanceToGoal;
        int oppDetour = oppDist - oppManhattanDistanceToGoal;

        int currentPlayerProgress = progressTowardGoal(state.getCurrPlayerPos(), state.getCurrPlayerFinishLine());
        int oppProgress = progressTowardGoal(state.getOpponentPos(), state.getOpponentFinishLine());

        int raceCloseness = Math.max(0, 6 - Math.abs(diff));
        int endgameDistance = Math.min(currentPlayerDist, oppDist);
        int wallWeight = endgameDistance <= 2 ? 3 : 8 + 2 * raceCloseness;

        return 140 * diff
                + 18 * diff * Math.abs(diff)
                + 45 * (oppDetour - currentPlayerDetour)
                + 10 * (currentPlayerProgress - oppProgress)
                + wallWeight * (state.getCurrPlayerWalls() - state.getOpponentWalls());
    }

    private int distanceToGoalLine(int position, int finishLine) {
        return Math.abs(rowOf(position) - finishLine);
    }

    private int progressTowardGoal(int position, int finishLine) {
        int row = rowOf(position);
        if (finishLine == 0) {
            return GameState.BOARD_LENGTH - 1 - row;
        }
        return row;
    }

    private int rowOf(int position) {
        return position / GameState.BOARD_LENGTH;
    }

    private int colOf(int position) {
        return position % GameState.BOARD_LENGTH;
    }
}
