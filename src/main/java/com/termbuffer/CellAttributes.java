package com.termbuffer;

/** The full set of display attributes for a cell: colors and style. */
public final class CellAttributes {
    public final TermColor foreground;
    public final TermColor background;
    public final StyleFlags style;

    public CellAttributes() {
        this(TermColor.DEFAULT, TermColor.DEFAULT, new StyleFlags());
    }

    public CellAttributes(TermColor foreground, TermColor background, StyleFlags style) {
        this.foreground = foreground;
        this.background = background;
        this.style = style;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CellAttributes a)) return false;
        return foreground == a.foreground && background == a.background && style.equals(a.style);
    }

    @Override
    public int hashCode() {
        int result = foreground.hashCode();
        result = 31 * result + background.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }
}
