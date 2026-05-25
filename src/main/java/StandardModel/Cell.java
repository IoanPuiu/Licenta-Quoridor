package StandardModel;

public class Cell {

    private final int row;
    private final int col;
    private Player player = null;
    private Wall leftWall = null;
    private Wall rightWall = null;
    private Wall upperWall = null;
    private Wall lowerWall = null;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Wall getLeftWall() {
        return leftWall;
    }

    public void setLeftWall(Wall wall) {
        leftWall = wall;
    }

    public Wall getRightWall() {
        return rightWall;
    }

    public void setRightWall(Wall wall) {
        rightWall = wall;
    }

    public Wall getUpperWall() {
        return upperWall;
    }

    public void setUpperWall(Wall wall) {
        upperWall = wall;
    }

    public Wall getLowerWall() {
        return lowerWall;
    }

    public void setLowerWall(Wall wall) {
        lowerWall = wall;
    }

    public Player getPlayer() {
        return player;
    }
}

