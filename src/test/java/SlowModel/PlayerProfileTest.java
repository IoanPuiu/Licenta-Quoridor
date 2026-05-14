package SlowModel;

import AI.MiniMax.MoveOrdering;
import SlowModel.PlayerProfile.MtcsVariant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        PlayerProfile playerProfile = new PlayerProfile(PlayerType.MTCS_MEDIUM, "Ignored");

        assertEquals("MTCS Medium", playerProfile.displayName("Second Player"));
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
    public void mtcsDisplayNameUsesConfiguredStateBudget() {
        assertEquals("MCTS8K", PlayerProfile.mtcs(8_000).displayName("Second Player"));
        assertEquals("MCTS16K", PlayerProfile.mtcs(16_000).displayName("Second Player"));
        assertEquals("MCTS32K", PlayerProfile.mtcs(32_000).displayName("Second Player"));
        assertEquals("MCTS64K", PlayerProfile.mtcs(64_000).displayName("Second Player"));
    }

    @Test
    public void mtcsPerformanceProfileKeepsSelectedVariant() {
        PlayerProfile profile = PlayerProfile.mtcs(16_000, MtcsVariant.PERFORMANCE);

        assertEquals(MtcsVariant.PERFORMANCE, profile.mtcsVariant());
        assertEquals("MCTS16K-P", profile.displayName("Second Player"));
        assertEquals("MCTS16K - Performance", profile.selectionSummary("Second Player"));
    }
}
