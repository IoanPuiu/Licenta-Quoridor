package AI;

public class MiniMaxAlgorithm {

    public int generateMove(GameState state) {
        return LegalMoveSelector.firstLegalMove(state);
    }
}
