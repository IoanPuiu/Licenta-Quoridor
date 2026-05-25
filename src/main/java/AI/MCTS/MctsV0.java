package AI.MCTS;

import AI.Algorithm;
import PerformanceModel.GameState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MctsV0 implements Algorithm {
    private static final double EXPLORATION_WEIGHT = Math.sqrt(2.0);
    private static final double WIN_REWARD = 1.0;
    private static final double LOSS_REWARD = 0.0;
    private static final double DRAW_REWARD = 0.5;
    private static final int DEFAULT_ROLLOUT_MOVE_LIMIT = 32;

    private final int steps;
    private final int rolloutMoveLimit;

    public MctsV0(int steps) {
        this(steps, DEFAULT_ROLLOUT_MOVE_LIMIT);
    }

    public MctsV0(int steps, int rolloutMoveLimit) {
        if (steps < 1) {
            throw new IllegalArgumentException("MCTS step count must be at least 1.");
        }
        if (rolloutMoveLimit < 1) {
            throw new IllegalArgumentException("MCTS rollout move limit must be at least 1.");
        }
        this.steps = steps;
        this.rolloutMoveLimit = rolloutMoveLimit;
    }

    @Override
    public int generateMove(GameState state) {
        Set<Integer> legalMoves = state.getAllPossibleMoveCodes();
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal moves available.");
        }

        Integer winningMove = immediateWinningMove(state, legalMoves);
        if (winningMove != null) {
            return winningMove;
        }

        int rootFinishLine = state.getCurrPlayerFinishLine();
        Node root = new Node(new GameState(state), null, -1);

        for (int step = 0; step < steps; step++) {
            Node selectedNode = select(root, rootFinishLine);
            double result = rollout(new GameState(selectedNode.state), rootFinishLine);
            backpropagate(selectedNode, result);
        }

        return bestRootMove(root, legalMoves);
    }

    private Node select(Node node, int rootFinishLine) {
        Node currentNode = node;
        while (!currentNode.isTerminal()) {
            if (currentNode.hasUntriedMoves()) {
                return expand(currentNode);
            }
            if (currentNode.children.isEmpty()) {
                return currentNode;
            }
            currentNode = bestUctChild(currentNode, rootFinishLine);
        }
        return currentNode;
    }

    private Node expand(Node node) {
        int selectedIndex = ThreadLocalRandom.current().nextInt(node.untriedMoves.size());
        int move = node.untriedMoves.remove(selectedIndex);

        GameState childState = new GameState(node.state);
        childState.update(move);

        Node child = new Node(childState, node, move);
        node.children.add(child);
        return child;
    }

    private Node bestUctChild(Node node, int rootFinishLine) {
        int movingPlayerFinishLine = node.state.getCurrPlayerFinishLine();
        double parentVisitsLog = Math.log(Math.max(1, node.visits));

        return node.children.stream()
                .max(Comparator.comparingDouble(child ->
                        uctScore(child, movingPlayerFinishLine, rootFinishLine, parentVisitsLog)))
                .orElseThrow();
    }

    private double uctScore(Node child, int movingPlayerFinishLine, int rootFinishLine, double parentVisitsLog) {
        if (child.visits == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double rootWinRate = child.score / child.visits;
        double playerWinRate = movingPlayerFinishLine == rootFinishLine ?
                rootWinRate
                : WIN_REWARD - rootWinRate;
        double exploration = EXPLORATION_WEIGHT * Math.sqrt(parentVisitsLog / child.visits);
        return playerWinRate + exploration;
    }

    private double rollout(GameState state, int rootFinishLine) {
        for (int moveCount = 0; moveCount < rolloutMoveLimit; moveCount++) {
            Integer winnerFinishLine = winnerFinishLine(state);
            if (winnerFinishLine != null) {
                return winnerFinishLine == rootFinishLine ? WIN_REWARD : LOSS_REWARD;
            }
            int move = selectFastRolloutMove(state);
            if (move == -1) {
                return evaluate(state, rootFinishLine);
            }
            state.update(move);
        }
        return evaluate(state, rootFinishLine);
    }

    private int selectFastRolloutMove(GameState state) {
        List<Integer> pawnMoves = state.getListPossiblePawnMoveCodes();

        if (pawnMoves.isEmpty()) {
            return -1;
        }

        Integer winningPawnMove = immediateWinningMoveFromList(state, pawnMoves);
        if (winningPawnMove != null) {
            return winningPawnMove;
        }

        return pawnMoves.get(ThreadLocalRandom.current().nextInt(pawnMoves.size()));
    }

    private Integer immediateWinningMoveFromList(GameState state, List<Integer> pawnMoves) {
        for (int move : pawnMoves) {
            if (GameState.isPawnMoveCode(move)
                    && GameState.decodePawnMoveRow(move) == state.getCurrPlayerFinishLine()) {
                return move;
            }
        }
        return null;
    }

    private void backpropagate(Node node, double result) {
        Node currentNode = node;
        while (currentNode != null) {
            currentNode.visits++;
            currentNode.score += result;
            currentNode = currentNode.parent;
        }
    }

    private int bestRootMove(Node root, Set<Integer> legalMoves) {
        if (root.children.isEmpty()) {
            return randomMove(legalMoves);
        }

        return root.children.stream()
                .max(Comparator.comparingInt((Node child) -> child.visits)
                        .thenComparingDouble(child -> child.score / child.visits)
                        .thenComparingInt(child -> child.move))
                .map(child -> child.move)
                .orElseGet(() -> randomMove(legalMoves));
    }

    private double evaluate(GameState state, int rootFinishLine) {
        int rootDistance;
        int opponentDistance;
        int rootWalls;
        int opponentWalls;

        if (state.getCurrPlayerFinishLine() == rootFinishLine) {
            rootDistance = state.getCurrentPlayerDistanceToFinish();
            opponentDistance = state.getOpponentDistanceToFinish();
            rootWalls = state.getCurrPlayerWalls();
            opponentWalls = state.getOpponentWalls();
        } else {
            rootDistance = state.getOpponentDistanceToFinish();
            opponentDistance = state.getCurrentPlayerDistanceToFinish();
            rootWalls = state.getOpponentWalls();
            opponentWalls = state.getCurrPlayerWalls();
        }

        if (rootDistance == 0) {
            return WIN_REWARD;
        }
        if (opponentDistance == 0) {
            return LOSS_REWARD;
        }

        double distanceScore = (opponentDistance - rootDistance) / (double) (2 * GameState.BOARD_LENGTH);
        double wallScore = 0.02 * (rootWalls - opponentWalls);
        return clamp(DRAW_REWARD + distanceScore + wallScore, LOSS_REWARD, WIN_REWARD);
    }

    private Integer immediateWinningMove(GameState state, Set<Integer> legalMoves) {
        for (int move : legalMoves) {
            if (GameState.isPawnMoveCode(move)
                    && GameState.decodePawnMoveRow(move) == state.getCurrPlayerFinishLine()) {
                return move;
            }
        }
        return null;
    }

    private Integer winnerFinishLine(GameState state) {
        if (state.getCurrentPlayerDistanceToFinish() == 0) {
            return state.getCurrPlayerFinishLine();
        }
        if (state.getOpponentDistanceToFinish() == 0) {
            return state.getOpponentFinishLine();
        }
        return null;
    }

    private int randomMove(Set<Integer> moves) {
        int selectedIndex = ThreadLocalRandom.current().nextInt(moves.size());
        int currentIndex = 0;
        for (int move : moves) {
            if (currentIndex == selectedIndex) {
                return move;
            }
            currentIndex++;
        }

        throw new IllegalStateException("Could not select a legal move.");
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Node {
        private final GameState state;
        private final Node parent;
        private final int move;
        private final List<Integer> untriedMoves;
        private final List<Node> children;
        private int visits;
        private double score;

        private Node(GameState state, Node parent, int move) {
            this.state = state;
            this.parent = parent;
            this.move = move;
            this.untriedMoves = isTerminalState(state)
                    ? new ArrayList<>()
                    : new ArrayList<>(state.getAllPossibleMoveCodes());
            this.children = new ArrayList<>();
        }

        private boolean hasUntriedMoves() {
            return !untriedMoves.isEmpty();
        }

        private boolean isTerminal() {
            return isTerminalState(state);
        }

        private static boolean isTerminalState(GameState state) {
            return state.getCurrentPlayerDistanceToFinish() == 0
                    || state.getOpponentDistanceToFinish() == 0;
        }
    }
}
