package model;

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
}
