package model.BoardTest;

import javafx.scene.paint.Color;
import model.Board;
import model.Move;
import model.Player;
import org.junit.jupiter.api.Test;

import static model.MoveType.PAWN_MOVE;

public class PlaceWall {
    //init(): create an empty board with 2 players randomly placed on the board

    //test cases:
    //1. place a horizontal wall on the board
    //2. place a vertical wall on the board


    private Board board;
    private Player firstPlayer;

    private void init() {
        board = new Board(9);
        firstPlayer = new Player("First Player", false, board.getBoardLength() - 1, board.getBoardLength() / 2, 0, Color.CYAN);
        board.getOneCell(firstPlayer.getRow(), firstPlayer.getCol()).setPlayer(firstPlayer);
        board.setFirstPlayer(firstPlayer);
    }

    @Test
    public void testPlaceHorizontalWall() {
        init();

        //place a horizontal wall on the board
        board.placeWall(new Move(firstPlayer, PAWN_MOVE, 4, 4, true));

        //assert that the wall is placed successfully
        assert board.getOneCell(4, 4).getRightWall() != null;
        assert board.getOneCell(4, 5).getLeftWall() != null;
    }

    @Test
    public void testPlaceVerticalWall() {
        init();

        //place a vertical wall on the board
        board.placeWall(new Move(firstPlayer, PAWN_MOVE, 4, 4, false));

        //assert that the wall is placed successfully
        assert board.getOneCell(4, 4).getLowerWall() != null;
        assert board.getOneCell(5, 4).getUpperWall() != null;
    }
}
