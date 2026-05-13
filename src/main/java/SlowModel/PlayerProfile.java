package SlowModel;

import AI.MiniMax.MoveOrdering;

public record PlayerProfile(
        PlayerType playerType,
        String humanName,
        int minimaxDepth,
        MoveOrdering minimaxMoveOrdering,
        int mtcsDepth,
        String aiDisplayName) {

    private static final int DEFAULT_MINIMAX_DEPTH = 2;
    private static final MoveOrdering DEFAULT_MINIMAX_MOVE_ORDERING = MoveOrdering.NONE;
    private static final int MTCS_EASY_DEPTH = 10_000;
    private static final int MTCS_MEDIUM_DEPTH = 30_000;
    private static final int MTCS_HARD_DEPTH = 60_000;

    public PlayerProfile {
        if (playerType == null) {
            throw new IllegalArgumentException("Player type cannot be null.");
        }
        if (minimaxDepth < 1) {
            throw new IllegalArgumentException("MiniMax depth must be at least 1.");
        }
        if (minimaxMoveOrdering == null) {
            minimaxMoveOrdering = DEFAULT_MINIMAX_MOVE_ORDERING;
        }
        if (mtcsDepth < 1) {
            throw new IllegalArgumentException("MTCS depth must be at least 1.");
        }
    }

    public PlayerProfile(PlayerType playerType, String humanName) {
        this(
                playerType,
                humanName,
                DEFAULT_MINIMAX_DEPTH,
                DEFAULT_MINIMAX_MOVE_ORDERING,
                defaultMtcsDepth(playerType),
                null);
    }

    public static PlayerProfile human(String humanName) {
        return new PlayerProfile(PlayerType.HUMAN, humanName);
    }

    public static PlayerProfile minimax(int depth, MoveOrdering moveOrdering) {
        MoveOrdering ordering = moveOrdering == null
                ? DEFAULT_MINIMAX_MOVE_ORDERING
                : moveOrdering;
        return new PlayerProfile(
                PlayerType.MINIMAX,
                "",
                depth,
                ordering,
                MTCS_MEDIUM_DEPTH,
                "MM%d%s".formatted(depth, ordering.compactSuffix()));
    }

    public static PlayerProfile mtcs(int depth) {
        return new PlayerProfile(
                mtcsTypeForDepth(depth),
                "",
                DEFAULT_MINIMAX_DEPTH,
                DEFAULT_MINIMAX_MOVE_ORDERING,
                depth,
                mtcsDisplayName(depth));
    }

    public static PlayerProfile gymPython() {
        return new PlayerProfile(PlayerType.GYM_PYTHON, "");
    }

    public String displayName(String fallbackName) {
        if (playerType.isAI()) {
            return aiDisplayName == null || aiDisplayName.isBlank()
                    ? playerType.toString()
                    : aiDisplayName;
        }

        if (humanName == null || humanName.isBlank()) {
            return fallbackName;
        }

        return humanName.trim();
    }

    public String selectionSummary(String fallbackName) {
        if (playerType == PlayerType.MINIMAX) {
            return "MiniMax D%d - %s".formatted(minimaxDepth, minimaxMoveOrdering.label());
        }
        return displayName(fallbackName);
    }

    private static int defaultMtcsDepth(PlayerType playerType) {
        return switch (playerType) {
            case MTCS_EASY -> MTCS_EASY_DEPTH;
            case MTCS_MEDIUM -> MTCS_MEDIUM_DEPTH;
            case MTCS_HARD -> MTCS_HARD_DEPTH;
            default -> MTCS_MEDIUM_DEPTH;
        };
    }

    private static PlayerType mtcsTypeForDepth(int depth) {
        if (depth <= MTCS_EASY_DEPTH) {
            return PlayerType.MTCS_EASY;
        }
        if (depth <= MTCS_MEDIUM_DEPTH) {
            return PlayerType.MTCS_MEDIUM;
        }
        return PlayerType.MTCS_HARD;
    }

    private static String mtcsDisplayName(int depth) {
        if (depth % 1_000 == 0) {
            return "MCTS%dK".formatted(depth / 1_000);
        }
        return "MCTS%d".formatted(depth);
    }
}
