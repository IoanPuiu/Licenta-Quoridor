package AI_prototip;

public class MiniMax {

    GameState state;

    public MiniMax(GameState state) {
        this.state = state;
    }

    public int generateMove() {
        return LegalMoveSelector.firstLegalMove(state);
    }
}
