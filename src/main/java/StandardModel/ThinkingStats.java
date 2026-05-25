package StandardModel;

public record ThinkingStats(
        long bottomLastMoveNanos,
        long bottomMoveTimeTotalNanos,
        int bottomMoveCount,
        long bottomMaxNanos,
        long bottomTotalThinkingNanos,
        int bottomWallImpactTotal,
        long topLastMoveNanos,
        long topMoveTimeTotalNanos,
        int topMoveCount,
        long topMaxNanos,
        long topTotalThinkingNanos,
        int topWallImpactTotal) {

    public static ThinkingStats empty() {
        return new ThinkingStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public long bottomAverageNanos() {
        return bottomMoveCount == 0 ? 0 : bottomMoveTimeTotalNanos / bottomMoveCount;
    }

    public long topAverageNanos() {
        return topMoveCount == 0 ? 0 : topMoveTimeTotalNanos / topMoveCount;
    }

    public ThinkingStats plus(ThinkingStats other, boolean keepOtherLastMove) {
        long mergedBottomLastMoveNanos = keepOtherLastMove ? other.bottomLastMoveNanos : 0;
        long mergedTopLastMoveNanos = keepOtherLastMove ? other.topLastMoveNanos : 0;

        return new ThinkingStats(
                mergedBottomLastMoveNanos,
                bottomMoveTimeTotalNanos + other.bottomMoveTimeTotalNanos,
                bottomMoveCount + other.bottomMoveCount,
                Math.max(bottomMaxNanos, other.bottomMaxNanos),
                bottomTotalThinkingNanos + other.bottomTotalThinkingNanos,
                bottomWallImpactTotal + other.bottomWallImpactTotal,
                mergedTopLastMoveNanos,
                topMoveTimeTotalNanos + other.topMoveTimeTotalNanos,
                topMoveCount + other.topMoveCount,
                Math.max(topMaxNanos, other.topMaxNanos),
                topTotalThinkingNanos + other.topTotalThinkingNanos,
                topWallImpactTotal + other.topWallImpactTotal);
    }

    public ThinkingStats appendBottomMove(boolean includeInAverage, long thinkingTimeNanos, int wallImpact) {
        long averageContribution = includeInAverage ? thinkingTimeNanos : 0;
        int moveCountContribution = includeInAverage ? 1 : 0;

        return new ThinkingStats(
                thinkingTimeNanos,
                bottomMoveTimeTotalNanos + averageContribution,
                bottomMoveCount + moveCountContribution,
                Math.max(bottomMaxNanos, thinkingTimeNanos),
                bottomTotalThinkingNanos + thinkingTimeNanos,
                bottomWallImpactTotal + wallImpact,
                topLastMoveNanos,
                topMoveTimeTotalNanos,
                topMoveCount,
                topMaxNanos,
                topTotalThinkingNanos,
                topWallImpactTotal);
    }

    public ThinkingStats appendTopMove(boolean includeInAverage, long thinkingTimeNanos, int wallImpact) {
        long averageContribution = includeInAverage ? thinkingTimeNanos : 0;
        int moveCountContribution = includeInAverage ? 1 : 0;

        return new ThinkingStats(
                bottomLastMoveNanos,
                bottomMoveTimeTotalNanos,
                bottomMoveCount,
                bottomMaxNanos,
                bottomTotalThinkingNanos,
                bottomWallImpactTotal,
                thinkingTimeNanos,
                topMoveTimeTotalNanos + averageContribution,
                topMoveCount + moveCountContribution,
                Math.max(topMaxNanos, thinkingTimeNanos),
                topTotalThinkingNanos + thinkingTimeNanos,
                topWallImpactTotal + wallImpact);
    }
}
