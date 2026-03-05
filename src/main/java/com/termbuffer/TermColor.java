package com.termbuffer;

/**
 * The 16 standard terminal colors plus a DEFAULT sentinel,
 * meaning "use the terminal's own foreground/background default".
 */
public enum TermColor {
    DEFAULT,
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
    BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW,
    BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE
}
