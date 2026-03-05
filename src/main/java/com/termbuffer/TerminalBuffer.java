package com.termbuffer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * TerminalBuffer — core data structure for a terminal emulator.
 *
 * Logical layout:
 *   scrollback  (index 0 ... scrollback.size-1)   oldest to newest, read-only
 *   screen      (index 0 ... height-1)             top to bottom, editable
 *
 * Content-access methods use a unified row index:
 *   0 ... scrollbackSize-1                       -> scrollback rows
 *   scrollbackSize ... scrollbackSize+height-1   -> screen rows
 */
public class TerminalBuffer {
    private int width;
    private int height;
    private final int maxScrollbackLines;

    private final Deque<Line> screen     = new ArrayDeque<>();
    private final Deque<Line> scrollback = new ArrayDeque<>();

    private int cursorCol = 0;
    private int cursorRow = 0;

    private CellAttributes currentAttributes = new CellAttributes();

    public TerminalBuffer(int width, int height) {
        this(width, height, 1000);
    }

    public TerminalBuffer(int width, int height, int maxScrollbackLines) {
        this.width = width;
        this.height = height;
        this.maxScrollbackLines = maxScrollbackLines;
        for (int i = 0; i < height; i++) screen.addLast(new Line(width));
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public int getScrollbackSize() { return scrollback.size(); }
    public int getTotalRows()      { return scrollback.size() + height; }

    /** Set the attributes that will be applied to all subsequent edits. */
    public void setAttributes(TermColor foreground, TermColor background, StyleFlags style) {
        currentAttributes = new CellAttributes(foreground, background, style);
    }

    public void setAttributes(TermColor foreground) {
        currentAttributes = new CellAttributes(foreground, TermColor.DEFAULT, new StyleFlags());
    }

    public void setAttributes(CellAttributes attributes) {
        currentAttributes = attributes;
    }

    public CellAttributes getAttributes() {
        return currentAttributes;
    }

    /** Move cursor to an absolute position, clamping to screen bounds. */
    public void setCursor(int col, int row) {
        cursorCol = Math.max(0, Math.min(col, width - 1));
        cursorRow = Math.max(0, Math.min(row, height - 1));
    }

    public void moveCursorUp(int n)    { cursorRow = Math.max(0, cursorRow - n); }
    public void moveCursorDown(int n)  { cursorRow = Math.min(height - 1, cursorRow + n); }
    public void moveCursorLeft(int n)  { cursorCol = Math.max(0, cursorCol - n); }
    public void moveCursorRight(int n) { cursorCol = Math.min(width - 1, cursorCol + n); }

    public int getCursorCol() { return cursorCol; }
    public int getCursorRow() { return cursorRow; }

    /**
     * Write text at the current cursor position, overwriting existing cells.
     * Stops at the right edge — no wrapping, no scrolling.
     */
    public void writeText(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (cursorCol >= width) break;
            char ch = text.charAt(i);
            screenLine(cursorRow).set(cursorCol, new Cell(ch, currentAttributes, false, false));
            cursorCol++;
        }
        cursorCol = Math.min(cursorCol, width - 1);
    }

    private List<Line> screenAsList() { return new ArrayList<>(screen); }

    private Line screenLine(int row) {
        return screenAsList().get(row);
    }

    @Override
    public String toString() {
        return "TerminalBuffer(" + width + "x" + height +
               ", scrollback=" + scrollback.size() + "/" + maxScrollbackLines +
               ", cursor=(" + cursorCol + "," + cursorRow + "))";
    }
}
