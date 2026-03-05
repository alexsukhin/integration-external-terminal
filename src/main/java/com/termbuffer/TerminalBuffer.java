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

    /**
     * Insert text at the current cursor position, shifting existing content right.
     * Overflow wraps to the next line, scrolling if at the bottom.
     */
    public void insertText(String text) {
        String remaining = text;
        while (!remaining.isEmpty()) {
            Line line = screenLine(cursorRow);

            List<Cell> toInsert = new ArrayList<>();
            int i = 0;
            while (i < remaining.length()) {
                char ch = remaining.charAt(i);
                toInsert.add(new Cell(ch, currentAttributes, false, false));
                i++;
                if (cursorCol + toInsert.size() >= width) break;
            }
            remaining = remaining.substring(i);

            int insertCount = toInsert.size();
            Line newLine = new Line(width);
            for (int c = 0; c < cursorCol; c++) newLine.set(c, line.get(c));
            for (int c = 0; c < toInsert.size(); c++) {
                if (cursorCol + c < width) newLine.set(cursorCol + c, toInsert.get(c));
            }
            for (int c = cursorCol; c < width - insertCount; c++) {
                int dst = c + insertCount;
                if (dst < width) newLine.set(dst, line.get(c));
            }
            setScreenLine(cursorRow, newLine);
            cursorCol = Math.min(cursorCol + insertCount, width - 1);

            if (!remaining.isEmpty()) {
                if (cursorRow < height - 1) {
                    cursorRow++;
                    cursorCol = 0;
                } else {
                    insertEmptyLine();
                    cursorCol = 0;
                    remaining = "";
                }
            }
        }
    }

    /**
     * Fill the entire row the cursor is on with ch (or empty if null),
     * using the current attributes.
     */
    public void fillLine(Character ch) {
        screenLine(cursorRow).fill(new Cell(ch, currentAttributes, false, false));
    }

    /** Fill the cursor row with empty cells. */
    public void fillLine() {
        fillLine(null);
    }

    /**
     * Scroll the screen up by one line: the top line moves into scrollback
     * and a blank line is appended at the bottom.
     */
    public void insertEmptyLine() {
        pushToScrollback(screen.removeFirst());
        screen.addLast(new Line(width));
    }

    /** Blank every cell on every screen row. Cursor position is unchanged. */
    public void clearScreen() {
        for (Line line : screen) line.fill();
    }

    /** Blank the screen and discard all scrollback history. */
    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    public Character getChar(int col, int row) {
        if (col < 0 || col >= width) throw new IllegalArgumentException("Column " + col + " out of range");
        return lineAt(row).get(col).ch;
    }

    public CellAttributes getCellAttributes(int col, int row) {
        if (col < 0 || col >= width) throw new IllegalArgumentException("Column " + col + " out of range");
        return lineAt(row).get(col).attributes;
    }

    public String getLine(int row) {
        return lineAt(row).asString();
    }

    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (Line line : screen) sb.append(line.asString()).append('\n');
        return sb.toString();
    }

    public String getFullContent() {
        StringBuilder sb = new StringBuilder();
        for (Line line : scrollback) sb.append(line.asString()).append('\n');
        for (Line line : screen)     sb.append(line.asString()).append('\n');
        return sb.toString();
    }

    private void pushToScrollback(Line line) {
        if (maxScrollbackLines <= 0) return;
        scrollback.addLast(line);
        while (scrollback.size() > maxScrollbackLines) scrollback.removeFirst();
    }

    private List<Line> screenAsList() { return new ArrayList<>(screen); }

    private Line screenLine(int row) {
        return screenAsList().get(row);
    }

    private void setScreenLine(int row, Line line) {
        List<Line> list = screenAsList();
        list.set(row, line);
        screen.clear();
        screen.addAll(list);
    }

    private Line lineAt(int row) {
        int total = getTotalRows();
        if (row < 0 || row >= total)
            throw new IllegalArgumentException("Row " + row + " out of range 0..<" + total);
        if (row < scrollback.size()) return new ArrayList<>(scrollback).get(row);
        return screenAsList().get(row - scrollback.size());
    }

    @Override
    public String toString() {
        return "TerminalBuffer(" + width + "x" + height +
               ", scrollback=" + scrollback.size() + "/" + maxScrollbackLines +
               ", cursor=(" + cursorCol + "," + cursorRow + "))";
    }
}
