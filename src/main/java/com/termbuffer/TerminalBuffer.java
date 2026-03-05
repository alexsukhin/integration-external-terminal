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
            boolean isWide = isWideChar(ch);
            if (isWide && cursorCol + 1 >= width) break;

            screenLine(cursorRow).set(cursorCol, new Cell(ch, currentAttributes, isWide, false));
            cursorCol++;

            if (isWide) {
                if (cursorCol < width) {
                    screenLine(cursorRow).set(cursorCol, new Cell(null, currentAttributes, false, true));
                }
                cursorCol++;
            }
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
                boolean isWide = isWideChar(ch);
                toInsert.add(new Cell(ch, currentAttributes, isWide, false));
                if (isWide) toInsert.add(new Cell(null, currentAttributes, false, true));
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

    /**
     * Resize the screen to newWidth x newHeight.
     *
     * Lines wider than newWidth are truncated; shorter lines are padded.
     * Shrinking height pushes top screen lines into scrollback.
     * Growing height appends blank lines at the bottom.
     * The cursor is clamped to the new bounds.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0)
            throw new IllegalArgumentException("Dimensions must be positive");

        List<Line> sbList     = new ArrayList<>(scrollback);
        List<Line> screenList = new ArrayList<>(screen);

        sbList.replaceAll(l     -> adaptLine(l, newWidth));
        screenList.replaceAll(l -> adaptLine(l, newWidth));

        scrollback.clear();
        scrollback.addAll(sbList);
        screen.clear();
        screen.addAll(screenList);

        if (newHeight > height) {
            for (int i = 0; i < newHeight - height; i++) screen.addLast(new Line(newWidth));
        } else if (newHeight < height) {
            for (int i = 0; i < height - newHeight; i++) pushToScrollback(screen.removeFirst());
        }

        width  = newWidth;
        height = newHeight;
        cursorCol = Math.min(cursorCol, width - 1);
        cursorRow = Math.min(cursorRow, height - 1);
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

    private Line adaptLine(Line src, int newWidth) {
        Line dst = new Line(newWidth);
        for (int c = 0; c < Math.min(src.width, newWidth); c++) dst.set(c, src.get(c));
        return dst;
    }

    /**
     * Returns true for characters that occupy two terminal columns.
     * Covers CJK Unified Ideographs and common fullwidth ranges.
     * A full implementation would use Unicode East Asian Width data.
     */
    private boolean isWideChar(char ch) {
        int cp = ch;
        return (cp >= 0x1100 && cp <= 0x115F) ||
               (cp >= 0x2E80 && cp <= 0x303E) ||
               (cp >= 0x3040 && cp <= 0x33FF) ||
               (cp >= 0x3400 && cp <= 0x4DBF) ||
               (cp >= 0x4E00 && cp <= 0x9FFF) ||
               (cp >= 0xAC00 && cp <= 0xD7AF) ||
               (cp >= 0xF900 && cp <= 0xFAFF) ||
               (cp >= 0xFE10 && cp <= 0xFE1F) ||
               (cp >= 0xFE30 && cp <= 0xFE4F) ||
               (cp >= 0xFF00 && cp <= 0xFF60) ||
               (cp >= 0xFFE0 && cp <= 0xFFE6);
    }

    @Override
    public String toString() {
        return "TerminalBuffer(" + width + "x" + height +
               ", scrollback=" + scrollback.size() + "/" + maxScrollbackLines +
               ", cursor=(" + cursorCol + "," + cursorRow + "))";
    }
}