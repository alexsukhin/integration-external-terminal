package com.termbuffer;

import java.util.Arrays;

/** A single line — a fixed-width array of cells. */
public class Line {
    public final int width;
    private final Cell[] cells;

    public Line(int width) {
        this.width = width;
        this.cells = new Cell[width];
        Arrays.fill(cells, Cell.EMPTY);
    }

    public Cell get(int col) {
        return cells[col];
    }

    public void set(int col, Cell cell) {
        cells[col] = cell;
    }

    /** Replace every cell with the given cell. */
    public void fill(Cell cell) {
        Arrays.fill(cells, cell);
    }

    /** Replace every cell with an empty cell. */
    public void fill() {
        fill(Cell.EMPTY);
    }

    public String asString() {
        StringBuilder sb = new StringBuilder();
        for (Cell cell : cells) {
            if (!cell.wideRight) {
                sb.append(cell.ch != null ? cell.ch : ' ');
            }
        }
        return sb.toString();
    }
}
