package SlowModel;

import java.util.*;

public class Board {
    private final Cell[][] cells;
    private Player firstPlayer;
    private Player secondPlayer;

    public void setFirstPlayer(Player firstPlayer) {
        this.firstPlayer = firstPlayer;
    }

    public void setSecondPlayer(Player secondPlayer) {
        this.secondPlayer = secondPlayer;
    }

    public Board(int length) {
        cells = new Cell[length][length];
        for (int i = 0; i < length; i++)
            for (int j = 0; j < length; j++) {
                cells[i][j] = new Cell(i, j);
            }
        putBorderWalls();
    }

    private void putBorderWalls() {
        int dim = cells.length;

        Wall topWall = new Wall(true);
        Wall bottomWall = new Wall(true);
        Wall leftWall = new Wall(false);
        Wall righWall = new Wall(false);

        for (int i = 0; i < dim; i++) {
            topWall.addCell(cells[0][i]);
            bottomWall.addCell(cells[dim - 1][i]);
            leftWall.addCell(cells[i][0]);
            righWall.addCell(cells[i][dim - 1]);
            cells[0][i].setUpperWall(topWall);
            cells[dim - 1][i].setLowerWall(bottomWall);
            cells[i][0].setLeftWall(leftWall);
            cells[i][dim - 1].setRightWall(righWall);
        }
    }

    public int getBoardLength() {
        return cells.length;
    }

    public Cell getOneCell(int row, int col) {
        return cells[row][col];
    }

    public void placeWall(Move move) {
        int row = move.getTargetRow();
        int col = move.getTargetCol();
        if (!move.isHorizontal()) {
            Wall wall = new Wall(cells[row][col], cells[row + 1][col], cells[row][col + 1], cells[row + 1][col + 1], false);
            cells[row][col].setRightWall(wall);
            cells[row + 1][col].setRightWall(wall);
            cells[row][col + 1].setLeftWall(wall);
            cells[row + 1][col + 1].setLeftWall(wall);
        } else {
            Wall wall = new Wall(cells[row][col], cells[row + 1][col], cells[row][col + 1], cells[row + 1][col + 1], true);
            cells[row][col].setLowerWall(wall);
            cells[row + 1][col].setUpperWall(wall);
            cells[row][col + 1].setLowerWall(wall);
            cells[row + 1][col + 1].setUpperWall(wall);
        }
    }

    public void removeWall(Move move) {
        int row = move.getTargetRow();
        int col = move.getTargetCol();
        if (!move.isHorizontal()) {
            cells[row][col].setRightWall(null);
            cells[row + 1][col].setRightWall(null);
            cells[row][col + 1].setLeftWall(null);
            cells[row + 1][col + 1].setLeftWall(null);
        } else {
            cells[row][col].setLowerWall(null);
            cells[row + 1][col].setUpperWall(null);
            cells[row][col + 1].setLowerWall(null);
            cells[row + 1][col + 1].setUpperWall(null);
        }
    }

    public void movePawn(Move move) {
        Player player = move.getPlayer();
        cells[player.getRow()][player.getCol()].setPlayer(null);
        cells[move.getTargetRow()][move.getTargetCol()].setPlayer(player);
    }

    private boolean isLegalPawnMove(Move move) {
        Player player = move.getPlayer();
        Set<Cell> possibleMoves = getPossibleCellsForPawnMoves(player);
        Cell targetCell = cells[move.getTargetRow()][move.getTargetCol()];
        return possibleMoves.contains(targetCell);
    }

    private boolean isLegalWallPlace(Move move) {
        //player out of walls
        Player player = move.getPlayer();
        if (player.wallsLeft() <= 0) {
            return false;
        }

        int row = move.getTargetRow();
        int col = move.getTargetCol();

        //wall out of board
        if (row >= cells.length - 1 || col >= cells.length - 1 || row < 0 || col < 0)
            return false;

        //wall over existing wall
        Wall wall = new Wall(cells[row][col], cells[row + 1][col], cells[row][col + 1], cells[row + 1][col + 1], move.isHorizontal());
        Set<Cell> wallCellSet = wall.getCells();
        Set<Wall> horizontalNeighbourWalls = new HashSet<>();
        Set<Wall> verticalNeighbourWalls = new HashSet<>();
        for (Cell cell : wallCellSet) {
            horizontalNeighbourWalls.add(cell.getUpperWall());
            horizontalNeighbourWalls.add(cell.getLowerWall());
            verticalNeighbourWalls.add(cell.getLeftWall());
            verticalNeighbourWalls.add(cell.getRightWall());
        }

        horizontalNeighbourWalls.remove(null);
        horizontalNeighbourWalls.remove(null);
        verticalNeighbourWalls.remove(null);
        verticalNeighbourWalls.remove(null);

        if (wall.isHorizontal()) {
            for (Wall horizontalWall : horizontalNeighbourWalls) {
                if (horizontalWall.getCells().containsAll(Set.of(cells[row][col], cells[row + 1][col])))
                    return false;
                if (horizontalWall.getCells().containsAll(Set.of(cells[row][col + 1], cells[row + 1][col + 1])))
                    return false;
            }
            for (Wall verticalwall : verticalNeighbourWalls) {
                if (verticalwall.getCells().containsAll(wallCellSet))
                    return false;
            }
        } else {
            for (Wall verticalwall : verticalNeighbourWalls) {
                if (verticalwall.getCells().containsAll(Set.of(cells[row][col], cells[row][col + 1])))
                    return false;
                if (verticalwall.getCells().containsAll(Set.of(cells[row + 1][col], cells[row + 1][col + 1])))
                    return false;
            }
            for (Wall horizontalWall : horizontalNeighbourWalls) {
                if (horizontalWall.getCells().containsAll(wallCellSet))
                    return false;
            }
        }
        //wall blocks all paths
        placeWall(move);
        boolean stillPath = existsPathForPlayer(firstPlayer) && existsPathForPlayer(secondPlayer);
        removeWall(move);
        return stillPath;
    }

    public boolean existsPathForPlayer(Player player) {
        int startRow = player.getRow();
        int startCol = player.getCol();
        int targetRow = player.getFinishRow();
        Queue<Cell> queue = new LinkedList<>();
        queue.add(cells[startRow][startCol]);
        boolean[][] visited = new boolean[cells.length][cells.length];
        visited[startRow][startCol] = true;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            Cell currentCell = queue.remove();
            if (currentCell.getRow() == targetRow) {
                return true;
            }
            for (int[] dir : dirs) {
                int newRow = currentCell.getRow() + dir[0];
                int newCol = currentCell.getCol() + dir[1];
                if (newRow >= 0 && newRow < cells.length && newCol >= 0 && newCol < cells.length) {
                    if (!visited[newRow][newCol] && isNotWall(currentCell.getRow(), currentCell.getCol(), newRow, newCol)) {
                        queue.add(cells[newRow][newCol]);
                        visited[newRow][newCol] = true;
                    }
                }
            }
        }
        return false;
    }


    public Set<Cell> getPossibleCellsForPawnMoves(Player player) {
        Set<Cell> moves = new HashSet<>();
        int currentRow = player.getRow();
        int currentCol = player.getCol();

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : dirs) {
            int newRow = currentRow + dir[0];
            int newCol = currentCol + dir[1];

            if (isNotWall(currentRow, currentCol, newRow, newCol)) {
                if (cells[newRow][newCol].getPlayer() != null) {
                    int jumpRow = newRow + dir[0];
                    int jumpCol = newCol + dir[1];
                    if (isNotWall(newRow, newCol, jumpRow, jumpCol)) {
                        moves.add(cells[jumpRow][jumpCol]);
                    } else {
                        int[][] sideDirs = {{dir[1], dir[0]}, {-dir[1], -dir[0]}};
                        for (int[] sideDir : sideDirs) {
                            int sideRow = newRow + sideDir[0];
                            int sideCol = newCol + sideDir[1];
                            if (isNotWall(newRow, newCol, sideRow, sideCol)) {
                                moves.add(cells[sideRow][sideCol]);
                            }
                        }
                    }
                } else {
                    moves.add(cells[newRow][newCol]);
                }
            }
        }
        return moves;
    }

    private boolean isNotWall(int currentRow, int currentCol, int newRow, int newCol) {
        if (currentRow < newRow) {
            return cells[currentRow][currentCol].getLowerWall() == null;
        } else if (currentRow > newRow) {
            return cells[currentRow][currentCol].getUpperWall() == null;
        } else if (currentCol < newCol) {
            return cells[currentRow][currentCol].getRightWall() == null;
        } else {
            return cells[currentRow][currentCol].getLeftWall() == null;
        }
    }

    public boolean isLegalMove(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE)
            return isLegalPawnMove(move);
        else {
            return isLegalWallPlace(move);
        }
    }

    public void update(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE)
            movePawn(move);
        else {
            placeWall(move);
        }
    }

    public List<Move> getPossiblePawnMoves(Player player) {
        List<Move> moves = new ArrayList<>();
        Set<Cell> cells = getPossibleCellsForPawnMoves(player);
        for (Cell cell : cells)
            moves.add(new Move(player, MoveType.PAWN_MOVE, cell.getRow(), cell.getCol(), false));
        return moves;
    }
}

// Create Unit Tests for the SlowModel package
