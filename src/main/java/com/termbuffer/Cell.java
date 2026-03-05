package com.termbuffer;

/**
 * A single cell in the terminal grid.
 *
 * ch is null for an empty/blank cell.
 * wide marks the left half of a 2-cell wide character (e.g. CJK).
 * wideRight marks the right placeholder half of a wide character.
 */
public final class Cell {
    public static final Cell EMPTY = new Cell(null, new CellAttributes(), false, false);

    public final Character ch;
    public final CellAttributes attributes;
    public final boolean wide;
    public final boolean wideRight;

    public Cell(Character ch, CellAttributes attributes, boolean wide, boolean wideRight) {
        this.ch = ch;
        this.attributes = attributes;
        this.wide = wide;
        this.wideRight = wideRight;
    }
}
