package model;

public record ThinkingStats(
        long bottomLastMoveNanos,
        long bottomTotalNanos,
        int bottomMoveCount,
        long bottomMaxNanos,
        long topLastMoveNanos,
        long topTotalNanos,
        int topMoveCount,
        long topMaxNanos) {

    public static ThinkingStats empty() {
        return new ThinkingStats(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public long bottomAverageNanos() {
        return bottomMoveCount == 0 ? 0 : bottomTotalNanos / bottomMoveCount;
    }

    public long topAverageNanos() {
        return topMoveCount == 0 ? 0 : topTotalNanos / topMoveCount;
    }
}
