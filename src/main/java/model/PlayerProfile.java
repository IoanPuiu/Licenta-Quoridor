package model;

public record PlayerProfile(PlayerType playerType, String humanName) {

    public String displayName(String fallbackName) {
        if (playerType.isAI()) {
            return playerType.toString();
        }

        if (humanName == null || humanName.isBlank()) {
            return fallbackName;
        }

        return humanName.trim();
    }
}
