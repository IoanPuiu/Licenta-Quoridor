package AI;

import PerformanceModel.GameState;

public class Mtcs implements Algorithm{
    public Mtcs(int steps) {

    }

    @Override
    public int generateMove(GameState state) {
        return randomValidMove(state);
    }
}
