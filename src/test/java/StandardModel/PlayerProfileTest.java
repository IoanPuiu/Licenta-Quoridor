package StandardModel;

import AI.MiniMax.MoveOrdering;
import AI.MCTS.MctsRolloutHeuristic;
import AI.MCTS.MctsSelectionHeuristic;
import StandardModel.PlayerProfile.MctsVariant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PlayerProfileTest {

    @Test
    public void humanDisplayNameUsesConfiguredName() {
        PlayerProfile playerProfile = new PlayerProfile(PlayerType.HUMAN, "Ana");

        assertEquals("Ana", playerProfile.displayName("First Player"));
    }

    @Test
    public void humanDisplayNameUsesFallbackWhenNameIsBlank() {
        PlayerProfile playerProfile = new PlayerProfile(PlayerType.HUMAN, " ");

        assertEquals("First Player", playerProfile.displayName("First Player"));
    }

    @Test
    public void aiDisplayNameUsesAlgorithmName() {
        PlayerProfile playerProfile = new PlayerProfile(PlayerType.MCTS_MEDIUM, "Ignored");

        assertEquals("MCTS Medium", playerProfile.displayName("Second Player"));
    }

    @Test
    public void minimaxDisplayNameUsesCompactDepthAndOrdering() {
        assertEquals("MM2F", PlayerProfile.minimax(2, MoveOrdering.FAST).displayName("Second Player"));
        assertEquals("MM2P", PlayerProfile.minimax(2, MoveOrdering.PRECISE).displayName("Second Player"));
        assertEquals("MM2", PlayerProfile.minimax(2, MoveOrdering.NONE).displayName("Second Player"));
    }

    @Test
    public void minimaxSelectionSummaryUsesOrderingLabel() {
        assertEquals(
                "MiniMax D3 - Fast Move Ordering",
                PlayerProfile.minimax(3, MoveOrdering.FAST).selectionSummary("Second Player"));
    }

    @Test
    public void mctsDisplayNameUsesConfiguredStateBudget() {
        assertEquals("MCTS8K", PlayerProfile.mcts(8_000).displayName("Second Player"));
        assertEquals("MCTS16K", PlayerProfile.mcts(16_000).displayName("Second Player"));
        assertEquals("MCTS32K", PlayerProfile.mcts(32_000).displayName("Second Player"));
        assertEquals("MCTS64K", PlayerProfile.mcts(64_000).displayName("Second Player"));
    }

    @Test
    public void mctsPerformanceProfileKeepsSelectedVariant() {
        PlayerProfile profile = PlayerProfile.mcts(16_000, MctsVariant.PERFORMANCE);

        assertEquals(MctsVariant.PERFORMANCE, profile.mctsVariant());
        assertEquals("MCTS16K-P", profile.displayName("Second Player"));
        assertEquals("MCTS16K - Performance - Rollout 32", profile.selectionSummary("Second Player"));
    }

    @Test
    public void mctsProfileKeepsSelectedRolloutMoveLimit() {
        PlayerProfile profile = PlayerProfile.mcts(16_000, MctsVariant.PERFORMANCE, 64);

        assertEquals(64, profile.mctsRolloutMoveLimit());
        assertEquals("MCTS16K-P-R64", profile.displayName("Second Player"));
        assertEquals("MCTS16K - Performance - Rollout 64", profile.selectionSummary("Second Player"));
    }

    @Test
    public void mctsPerformanceProfileKeepsSelectedHeuristics() {
        PlayerProfile profile = PlayerProfile.mcts(
                16_000,
                MctsVariant.PERFORMANCE,
                MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES,
                MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS,
                32);

        assertEquals(
                MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES,
                profile.mctsSelectionHeuristic());
        assertEquals(MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS, profile.mctsRolloutHeuristic());
    }

    @Test
    public void mctsPerformanceCardSummaryIncludesChosenSettings() {
        PlayerProfile profile = PlayerProfile.mcts(
                16_000,
                MctsVariant.PERFORMANCE,
                MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES,
                MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS,
                32);

        assertEquals(
                "D16K | Performance | Sel Pawns+walls+edges | Roll Relevant | Limit 32",
                profile.cardSettingsSummary("Second Player"));
    }

    @Test
    public void mctsV0CardSummaryShowsVisibleStartSettingsOnly() {
        PlayerProfile profile = PlayerProfile.mcts(16_000, MctsVariant.V0, 64);

        assertEquals("D16K | V0 | Limit 64", profile.cardSettingsSummary("Second Player"));
    }

    @Test
    public void mctsV0IgnoresPerformanceHeuristics() {
        PlayerProfile profile = PlayerProfile.mcts(
                16_000,
                MctsVariant.V0,
                MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES,
                MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS,
                32);

        assertEquals(PlayerProfile.DEFAULT_MCTS_SELECTION_HEURISTIC, profile.mctsSelectionHeuristic());
        assertEquals(PlayerProfile.DEFAULT_MCTS_ROLLOUT_HEURISTIC, profile.mctsRolloutHeuristic());
    }

    @Test
    public void mctsProfileRejectsUnsupportedRolloutMoveLimit() {
        assertThrows(IllegalArgumentException.class, () -> PlayerProfile.mcts(16_000, MctsVariant.V0, 24));
    }
}
