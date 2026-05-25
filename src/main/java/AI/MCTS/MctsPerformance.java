package AI.MCTS;

import AI.Algorithm;
import PerformanceModel.GameState;

import java.util.concurrent.ThreadLocalRandom;

public class MctsPerformance implements Algorithm {

    private static final int NO_MOVE = -1;
    private static final int NO_WINNER = -1;

    private static final double EXPLORATION_WEIGHT = Math.sqrt(2.0);
    private static final double WIN_REWARD = 1.0;
    private static final double LOSS_REWARD = 0.0;
    private static final int DEFAULT_ROLLOUT_MOVE_LIMIT = 32;

    //Progressive widening:
    private static final double PW_CONSTANT = 2.0;
    private static final double PW_ALPHA = 0.5;
    private static final int MAX_CANDIDATE_MOVES = 64;

    private final int steps;
    private final int rolloutMoveLimit;
    private final MctsSelectionHeuristic selectionHeuristic;
    private final MctsRolloutHeuristic rolloutHeuristic;

    public MctsPerformance(int steps) {
        this(steps, DEFAULT_ROLLOUT_MOVE_LIMIT);
    }

    public MctsPerformance(int steps, int rolloutMoveLimit) {
        this(
                steps,
                rolloutMoveLimit,
                MctsSelectionHeuristic.WALLS_NEAR_PAWNS,
                MctsRolloutHeuristic.PAWN_MOVES);
    }

    public MctsPerformance(
            int steps,
            int rolloutMoveLimit,
            MctsSelectionHeuristic selectionHeuristic,
            MctsRolloutHeuristic rolloutHeuristic) {
        if (steps < 1) {
            throw new IllegalArgumentException("MCTS steps must be at least 1.");
        }
        if (rolloutMoveLimit < 1) {
            throw new IllegalArgumentException("MCTS rollout move limit must be at least 1.");
        }

        this.steps = steps;
        this.rolloutMoveLimit = rolloutMoveLimit;
        this.selectionHeuristic = selectionHeuristic == null
                ? MctsSelectionHeuristic.WALLS_NEAR_PAWNS
                : selectionHeuristic;
        this.rolloutHeuristic = rolloutHeuristic == null
                ? MctsRolloutHeuristic.PAWN_MOVES
                : rolloutHeuristic;
    }

    @Override
    public int generateMove(GameState state) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        MctsState rootState = new MctsState(state);

        int rootFinishLine = rootState.getCurrPlayerFinishLine();

        int winningMove = rootState.findImmediateWinningPawnMove();
        if (winningMove != NO_MOVE) {
            return winningMove;
        }

        Node root = new Node(rootState, null, NO_MOVE, selectionHeuristic);

        if (!root.hasUntriedMoves()) {
            throw new IllegalStateException("No candidate moves available.");
        }

        for (int step = 0; step < steps; step++) {
            Node selectedNode = select(root, rootFinishLine, random);

            MctsState rolloutState = new MctsState(selectedNode.state);

            double result = rollout(rolloutState, rootFinishLine, random);

            backpropagate(selectedNode, result);
        }

        return bestRootMove(root);
    }


    // 1. SELECTION + EXPANSION
    private Node select(Node root, int rootFinishLine, ThreadLocalRandom random) {
        Node currentNode = root;

        while (!currentNode.state.isTerminal()) {

            // Progressive Widening: Extindem nodul doar dacă are voie să aibă mai mulți copii.
            if (currentNode.hasUntriedMoves() && shouldExpand(currentNode)) {
                return expand(currentNode, random);
            }

            // Dacă nu are copii, dar are mutări neîncercate, trebuie extins.
            if (currentNode.childCount == 0) {
                if (currentNode.hasUntriedMoves()) {
                    return expand(currentNode, random);
                }

                return currentNode;
            }

            //Alegem copilul cu cel mai bun UCT.
            currentNode = bestUctChild(currentNode, rootFinishLine);
        }

        return currentNode;
    }

    private boolean shouldExpand(Node node) {
        int allowedChildren = Math.max(
                1,
                (int) (PW_CONSTANT * Math.pow(node.visits + 1, PW_ALPHA))
        );

        return node.childCount < allowedChildren;
    }

    private Node expand(Node node, ThreadLocalRandom random) {
        int move = node.takeRandomUntriedMove(random);

        MctsState childState = new MctsState(node.state);
        childState.applyMove(move);

        Node child = new Node(childState, node, move, selectionHeuristic);
        node.addChild(child);

        return child;
    }

    // 2. UCT
    private Node bestUctChild(Node node, int rootFinishLine) {
        Node bestChild = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        int movingPlayerFinishLine = node.state.getCurrPlayerFinishLine();
        double parentVisitsLog = Math.log(node.visits + 1.0);

        for (int i = 0; i < node.childCount; i++) {
            Node child = node.children[i];

            double score = uctScore(
                    child,
                    movingPlayerFinishLine,
                    rootFinishLine,
                    parentVisitsLog
            );

            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild;
    }

    private double uctScore(
            Node child,
            int movingPlayerFinishLine,
            int rootFinishLine,
            double parentVisitsLog
    ) {
        if (child.visits == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double rootWinRate = child.score / child.visits;

        double playerWinRate = movingPlayerFinishLine == rootFinishLine
                ? rootWinRate
                : WIN_REWARD - rootWinRate;

        double exploration = EXPLORATION_WEIGHT
                * Math.sqrt(parentVisitsLog / child.visits);

        return playerWinRate + exploration;
    }

    // 3. ROLLOUT
    private double rollout(
            MctsState state,
            int rootFinishLine,
            ThreadLocalRandom random
    ) {
        for (int depth = 0; depth < rolloutMoveLimit; depth++) {

            int winnerFinishLine = state.winnerFinishLine();

            if (winnerFinishLine != NO_WINNER) {
                return winnerFinishLine == rootFinishLine
                        ? WIN_REWARD
                        : LOSS_REWARD;
            }

            int move = state.selectRolloutMove(random, rolloutHeuristic);

            if (move == NO_MOVE) {
                return state.evaluateForRoot(rootFinishLine);
            }

            state.applyMove(move);
        }


        return state.evaluateForRoot(rootFinishLine);
    }

    // 4. BACKPROPAGATION
    private void backpropagate(Node node, double resultForRootPlayer) {
        Node current = node;

        while (current != null) {
            current.visits++;
            current.score += resultForRootPlayer;
            current = current.parent;
        }
    }

    // 5. ALEGEREA MUTĂRII FINALE
    private int bestRootMove(Node root) {
        Node bestChild = null;

        for (int i = 0; i < root.childCount; i++) {
            Node child = root.children[i];

            if (bestChild == null) {
                bestChild = child;
                continue;
            }

            if (child.visits > bestChild.visits) {
                bestChild = child;
                continue;
            }

            if (child.visits == bestChild.visits) {
                double childWinRate = child.winRate();
                double bestWinRate = bestChild.winRate();

                if (childWinRate > bestWinRate) {
                    bestChild = child;
                }
            }
        }

        if (bestChild == null) {
            return root.untriedMoves[0];
        }

        return bestChild.move;
    }

    // 6. NOD MCTS
    private static final class Node {

        private final MctsState state;
        private final Node parent;
        private final int move;

        private final int[] untriedMoves;
        private int untriedCount;

        private final Node[] children;
        private int childCount;

        private int visits;
        private double score;

        private Node(
                MctsState state,
                Node parent,
                int move,
                MctsSelectionHeuristic selectionHeuristic) {
            this.state = state;
            this.parent = parent;
            this.move = move;

            this.untriedMoves = new int[MAX_CANDIDATE_MOVES];
            this.children = new Node[MAX_CANDIDATE_MOVES];

            this.visits = 0;
            this.score = 0.0;

            if (state.isTerminal()) {
                this.untriedCount = 0;
            } else {
                this.untriedCount = state.generateCandidateMoves(this.untriedMoves, selectionHeuristic);
            }
        }

        private boolean hasUntriedMoves() {
            return untriedCount > 0;
        }

        private int takeRandomUntriedMove(ThreadLocalRandom random) {
            int index = random.nextInt(untriedCount);
            int move = untriedMoves[index];

            //înlocuim mutarea aleasă cu ultima mutare din buffer
            untriedMoves[index] = untriedMoves[untriedCount - 1];
            untriedCount--;

            return move;
        }

        private void addChild(Node child) {
            if (childCount >= children.length) {
                throw new IllegalStateException("Too many children in MCTS node.");
            }

            children[childCount++] = child;
        }

        private double winRate() {
            if (visits == 0) {
                return 0.0;
            }

            return score / visits;
        }
    }
}
