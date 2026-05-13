package SlowModel;

import java.util.HashSet;
import java.util.Set;

public class Wall {
    private Set<Cell> cells;
    private final Boolean isHorizontal;

    public Wall(Boolean isHorizontal) {
        cells = new HashSet<>();
        this.isHorizontal = isHorizontal;
    }

    public Wall(Cell cell1, Cell cell2, Cell cell3, Cell cell4, Boolean isHorizontal) {
        cells = new HashSet<>();
        cells.add(cell1);
        cells.add(cell2);
        cells.add(cell3);
        cells.add(cell4);
        this.isHorizontal = isHorizontal;
    }

    public Boolean isHorizontal() {
        return isHorizontal;
    }

    public Set<Cell> getCells() {
        return cells;
    }

    public void addCell(Cell cell){
        cells.add(cell);
    }
}
