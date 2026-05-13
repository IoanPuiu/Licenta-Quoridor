package SlowModel;

import AI.MiniMax.MoveOrdering;
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
}
