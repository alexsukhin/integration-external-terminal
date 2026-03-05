package com.termbuffer;

/** Visual style flags that can be combined freely. */
public final class StyleFlags {
    public final boolean bold;
    public final boolean italic;
    public final boolean underline;

    public StyleFlags() {
        this(false, false, false);
    }

    public StyleFlags(boolean bold, boolean italic, boolean underline) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StyleFlags s)) return false;
        return bold == s.bold && italic == s.italic && underline == s.underline;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(bold);
        result = 31 * result + Boolean.hashCode(italic);
        result = 31 * result + Boolean.hashCode(underline);
        return result;
    }
}
