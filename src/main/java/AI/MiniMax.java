package AI;

public class MiniMax {

    GameState state;

    public MiniMax(GameState state) {
        this.state = state;
    }

    public int generateMove() {
        return LegalMoveSelector.firstLegalMove(state);
    }
}
