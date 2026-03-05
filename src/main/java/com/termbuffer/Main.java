package com.termbuffer;

public class Main {
    public static void main(String[] args) {
        TerminalBuffer buf = new TerminalBuffer(40, 10);

        buf.setAttributes(TermColor.BRIGHT_RED, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 0);
        buf.writeText("Hello in RED!");

        buf.setAttributes(TermColor.BRIGHT_GREEN, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 2);
        buf.writeText("Hello in GREEN!");

        buf.setAttributes(TermColor.BRIGHT_BLUE, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 4);
        buf.writeText("Hello in BLUE!");

        buf.setAttributes(TermColor.BRIGHT_YELLOW, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 6);
        buf.writeText("Hello in YELLOW!");

        renderScreen(buf);
    }

    static void renderScreen(TerminalBuffer buf) {
        for (int i = buf.getScrollbackSize(); i < buf.getTotalRows(); i++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < buf.getWidth(); col++) {
                Character ch = buf.getChar(col, i);
                CellAttributes attr = buf.getCellAttributes(col, i);
                line.append(ansiColor(attr.foreground))
                    .append(ch != null ? ch : ' ')
                    .append("\u001B[0m");
            }
            String trimmed = line.toString().stripTrailing();
            if (!trimmed.isEmpty()) System.out.println(trimmed);
        }
    }

    static String ansiColor(TermColor color) {
        return switch (color) {
            case DEFAULT        -> "\u001B[0m";
            case BLACK          -> "\u001B[30m";
            case RED            -> "\u001B[31m";
            case GREEN          -> "\u001B[32m";
            case YELLOW         -> "\u001B[33m";
            case BLUE           -> "\u001B[34m";
            case MAGENTA        -> "\u001B[35m";
            case CYAN           -> "\u001B[36m";
            case WHITE          -> "\u001B[37m";
            case BRIGHT_BLACK   -> "\u001B[90m";
            case BRIGHT_RED     -> "\u001B[91m";
            case BRIGHT_GREEN   -> "\u001B[92m";
            case BRIGHT_YELLOW  -> "\u001B[93m";
            case BRIGHT_BLUE    -> "\u001B[94m";
            case BRIGHT_MAGENTA -> "\u001B[95m";
            case BRIGHT_CYAN    -> "\u001B[96m";
            case BRIGHT_WHITE   -> "\u001B[97m";
        };
    }
}
