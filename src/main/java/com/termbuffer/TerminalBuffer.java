package com.termbuffer;

import java.util.ArrayDeque;
import java.util.Deque;

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

    @Override
    public String toString() {
        return "TerminalBuffer(" + width + "x" + height +
               ", scrollback=" + scrollback.size() + "/" + maxScrollbackLines +
               ", cursor=(0,0))";
    }
}
