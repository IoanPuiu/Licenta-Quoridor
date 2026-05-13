package AI;

import PerformanceModel.GameState;

public class GymPython implements Algorithm{

    @Override
    public int generateMove(GameState state) {
        return randomValidMove(state);
    }
}
