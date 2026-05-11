package AI;

public class MiniMax {

    public int generateMove(GameState state) {
        return LegalMoveSelector.firstLegalMove(state);
    }
}
