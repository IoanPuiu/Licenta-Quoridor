package SlowModel;

import AI.MCTS.MctsRolloutHeuristic;
import AI.MCTS.MctsSelectionHeuristic;
import AI.MiniMax.MoveOrdering;

import java.util.ArrayList;
import java.util.List;

public record PlayerProfile(
        PlayerType playerType,
        String humanName,
        int minimaxDepth,
        MoveOrdering minimaxMoveOrdering,
        int mctsDepth,
        MctsVariant mctsVariant,
        MctsSelectionHeuristic mctsSelectionHeuristic,
        MctsRolloutHeuristic mctsRolloutHeuristic,
        int mctsRolloutMoveLimit,
        String aiDisplayName) {

    public enum MctsVariant {
        V0("V0", ""),
        PERFORMANCE("Performance", "-P");

        private final String label;
        private final String compactSuffix;

        MctsVariant(String label, String compactSuffix) {
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

    private static final int DEFAULT_MINIMAX_DEPTH = 2;
    private static final MoveOrdering DEFAULT_MINIMAX_MOVE_ORDERING = MoveOrdering.NONE;
    private static final MctsVariant DEFAULT_MCTS_VARIANT = MctsVariant.V0;
    public static final MctsSelectionHeuristic DEFAULT_MCTS_SELECTION_HEURISTIC =
            MctsSelectionHeuristic.WALLS_NEAR_PAWNS;
    public static final MctsRolloutHeuristic DEFAULT_MCTS_ROLLOUT_HEURISTIC =
            MctsRolloutHeuristic.PAWN_MOVES;
    public static final int DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT = 32;
    private static final int MCTS_EASY_DEPTH = 8_000;
    private static final int MCTS_MEDIUM_DEPTH = 16_000;
    private static final int MCTS_HARD_DEPTH = 32_000;
    private static final int MCTS_EXTREME_DEPTH = 64_000;
    private static final int MCTS_SHORT_ROLLOUT_MOVE_LIMIT = 16;
    private static final int MCTS_MEDIUM_ROLLOUT_MOVE_LIMIT = DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT;
    private static final int MCTS_LONG_ROLLOUT_MOVE_LIMIT = 64;

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
        if (mctsDepth < 1) {
            throw new IllegalArgumentException("MCTS depth must be at least 1.");
        }
        if (mctsVariant == null) {
            mctsVariant = DEFAULT_MCTS_VARIANT;
        }
        if (mctsSelectionHeuristic == null) {
            mctsSelectionHeuristic = DEFAULT_MCTS_SELECTION_HEURISTIC;
        }
        if (mctsRolloutHeuristic == null) {
            mctsRolloutHeuristic = DEFAULT_MCTS_ROLLOUT_HEURISTIC;
        }
        if (!isSupportedMctsRolloutMoveLimit(mctsRolloutMoveLimit)) {
            throw new IllegalArgumentException("MCTS rollout move limit must be one of 16, 32, or 64.");
        }
        if (mctsVariant != MctsVariant.PERFORMANCE) {
            mctsSelectionHeuristic = DEFAULT_MCTS_SELECTION_HEURISTIC;
            mctsRolloutHeuristic = DEFAULT_MCTS_ROLLOUT_HEURISTIC;
        }
    }

    public PlayerProfile(PlayerType playerType, String humanName) {
        this(
                playerType,
                humanName,
                DEFAULT_MINIMAX_DEPTH,
                DEFAULT_MINIMAX_MOVE_ORDERING,
                defaultMctsDepth(playerType),
                DEFAULT_MCTS_VARIANT,
                DEFAULT_MCTS_SELECTION_HEURISTIC,
                DEFAULT_MCTS_ROLLOUT_HEURISTIC,
                DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT,
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
                MCTS_MEDIUM_DEPTH,
                DEFAULT_MCTS_VARIANT,
                DEFAULT_MCTS_SELECTION_HEURISTIC,
                DEFAULT_MCTS_ROLLOUT_HEURISTIC,
                DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT,
                "MM%d%s".formatted(depth, ordering.compactSuffix()));
    }

    public static PlayerProfile mcts(int depth) {
        return mcts(depth, DEFAULT_MCTS_VARIANT);
    }

    public static PlayerProfile mcts(int depth, MctsVariant variant) {
        return mcts(depth, variant, DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT);
    }

    public static PlayerProfile mcts(int depth, MctsVariant variant, int rolloutMoveLimit) {
        return mcts(
                depth,
                variant,
                DEFAULT_MCTS_SELECTION_HEURISTIC,
                DEFAULT_MCTS_ROLLOUT_HEURISTIC,
                rolloutMoveLimit);
    }

    public static PlayerProfile mcts(
            int depth,
            MctsVariant variant,
            MctsSelectionHeuristic selectionHeuristic,
            MctsRolloutHeuristic rolloutHeuristic,
            int rolloutMoveLimit) {
        MctsVariant selectedVariant = variant == null
                ? DEFAULT_MCTS_VARIANT
                : variant;
        MctsSelectionHeuristic selectedSelectionHeuristic = selectedVariant == MctsVariant.PERFORMANCE
                ? defaultIfNull(selectionHeuristic, DEFAULT_MCTS_SELECTION_HEURISTIC)
                : DEFAULT_MCTS_SELECTION_HEURISTIC;
        MctsRolloutHeuristic selectedRolloutHeuristic = selectedVariant == MctsVariant.PERFORMANCE
                ? defaultIfNull(rolloutHeuristic, DEFAULT_MCTS_ROLLOUT_HEURISTIC)
                : DEFAULT_MCTS_ROLLOUT_HEURISTIC;
        return new PlayerProfile(
                mctsTypeForDepth(depth),
                "",
                DEFAULT_MINIMAX_DEPTH,
                DEFAULT_MINIMAX_MOVE_ORDERING,
                depth,
                selectedVariant,
                selectedSelectionHeuristic,
                selectedRolloutHeuristic,
                rolloutMoveLimit,
                mctsDisplayName(depth, selectedVariant, rolloutMoveLimit));
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
        if (isMctsPlayer()) {
            return "%s - %s - Rollout %d".formatted(
                    mctsDisplayName(depthOrDefaultMctsDepth()),
                    mctsVariant.label(),
                    mctsRolloutMoveLimit);
        }
        return displayName(fallbackName);
    }

    public String cardTitle(String fallbackName) {
        if (playerType == PlayerType.HUMAN) {
            return displayName(fallbackName);
        }
        if (playerType == PlayerType.MINIMAX) {
            return "MiniMax";
        }
        if (isMctsPlayer()) {
            return "MCTS";
        }
        return displayName(fallbackName);
    }

    public String cardSettingsSummary(String fallbackName) {
        return String.join(" | ", cardSettingsSummaryParts(fallbackName));
    }

    public String cardSettingsDetails(String fallbackName) {
        return String.join(System.lineSeparator(), cardSettingsDetailLines(fallbackName));
    }

    private List<String> cardSettingsSummaryParts(String fallbackName) {
        if (playerType == PlayerType.HUMAN) {
            return List.of("Human", "Name: " + displayName(fallbackName));
        }
        if (playerType == PlayerType.MINIMAX) {
            return List.of(
                    "Depth " + minimaxDepth,
                    minimaxMoveOrdering.label());
        }
        if (isMctsPlayer()) {
            List<String> parts = new ArrayList<>();
            parts.add("D" + compactMctsDepth(depthOrDefaultMctsDepth()));
            parts.add(mctsVariant.label());
            if (mctsVariant == MctsVariant.PERFORMANCE) {
                parts.add("Sel " + compactSelectionHeuristic());
                parts.add("Roll " + compactRolloutHeuristic());
            }
            parts.add("Limit " + mctsRolloutMoveLimit);
            return parts;
        }
        return List.of(displayName(fallbackName));
    }

    private List<String> cardSettingsDetailLines(String fallbackName) {
        if (playerType == PlayerType.HUMAN) {
            return List.of(
                    "Type: Human",
                    "Name: " + displayName(fallbackName));
        }
        if (playerType == PlayerType.MINIMAX) {
            return List.of(
                    "Type: MiniMax",
                    "Depth: " + minimaxDepth,
                    "Move ordering: " + minimaxMoveOrdering.label());
        }
        if (isMctsPlayer()) {
            List<String> lines = new ArrayList<>();
            lines.add("Type: MCTS");
            lines.add("Depth: " + compactMctsDepth(depthOrDefaultMctsDepth()));
            lines.add("Variant: " + mctsVariant.label());
            if (mctsVariant == MctsVariant.PERFORMANCE) {
                lines.add("Selection heuristic: " + mctsSelectionHeuristic.label());
                lines.add("Rollout heuristic: " + mctsRolloutHeuristic.label());
            }
            lines.add("Rollout limit: " + mctsRolloutMoveLimit);
            return lines;
        }
        return List.of("Type: " + displayName(fallbackName));
    }

    private boolean isMctsPlayer() {
        return playerType == PlayerType.MCTS_EASY
                || playerType == PlayerType.MCTS_MEDIUM
                || playerType == PlayerType.MCTS_HARD
                || playerType == PlayerType.MCTS_EXTREME;
    }

    private int depthOrDefaultMctsDepth() {
        return mctsDepth > 0 ? mctsDepth : defaultMctsDepth(playerType);
    }

    private static int defaultMctsDepth(PlayerType playerType) {
        return switch (playerType) {
            case MCTS_EASY -> MCTS_EASY_DEPTH;
            case MCTS_MEDIUM -> MCTS_MEDIUM_DEPTH;
            case MCTS_HARD -> MCTS_HARD_DEPTH;
            case MCTS_EXTREME -> MCTS_EXTREME_DEPTH;
            default -> MCTS_MEDIUM_DEPTH;
        };
    }

    private static PlayerType mctsTypeForDepth(int depth) {
        if (depth <= MCTS_EASY_DEPTH) {
            return PlayerType.MCTS_EASY;
        }
        if (depth <= MCTS_MEDIUM_DEPTH) {
            return PlayerType.MCTS_MEDIUM;
        }
        if (depth <= MCTS_HARD_DEPTH) {
            return PlayerType.MCTS_HARD;
        }
        return PlayerType.MCTS_EXTREME;
    }

    private static String mctsDisplayName(int depth) {
        if (depth % 1_000 == 0) {
            return "MCTS%dK".formatted(depth / 1_000);
        }
        return "MCTS%d".formatted(depth);
    }

    private static String compactMctsDepth(int depth) {
        if (depth % 1_000 == 0) {
            return "%dK".formatted(depth / 1_000);
        }
        return String.valueOf(depth);
    }

    private String compactSelectionHeuristic() {
        return switch (mctsSelectionHeuristic) {
            case WALLS_NEAR_PAWNS -> "Pawns";
            case WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES -> "Pawns+walls+edges";
        };
    }

    private String compactRolloutHeuristic() {
        return switch (mctsRolloutHeuristic) {
            case PAWN_MOVES -> "Pawns";
            case PAWN_MOVES_RANDOM_WALLS -> "Random walls";
            case PAWN_MOVES_RELEVANT_WALLS -> "Relevant";
        };
    }

    private static String mctsDisplayName(int depth, MctsVariant variant, int rolloutMoveLimit) {
        return mctsDisplayName(depth) + variant.compactSuffix() + rolloutCompactSuffix(rolloutMoveLimit);
    }

    private static String rolloutCompactSuffix(int rolloutMoveLimit) {
        return rolloutMoveLimit == DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT
                ? ""
                : "-R%d".formatted(rolloutMoveLimit);
    }

    private static boolean isSupportedMctsRolloutMoveLimit(int rolloutMoveLimit) {
        return rolloutMoveLimit == MCTS_SHORT_ROLLOUT_MOVE_LIMIT
                || rolloutMoveLimit == MCTS_MEDIUM_ROLLOUT_MOVE_LIMIT
                || rolloutMoveLimit == MCTS_LONG_ROLLOUT_MOVE_LIMIT;
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
