package AI;

import PerformanceModel.GameState;

public class MiniMax implements Algorithm{
    @Override
    public int generateMove(GameState state) {
        return randomValidMove(state);
    }
}
