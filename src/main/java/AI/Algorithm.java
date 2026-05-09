package AI;

import model.Board;
import model.Move;
import model.Player;

public class Algorithm {

    public Move generateMove(Board board, Player playerInTurn, Player opponent) {

        GameState state = new GameState(board, playerInTurn, opponent);
        //create min max strategy to generate a move for the playerInTurn in actual board state
        return null;
    }
}
