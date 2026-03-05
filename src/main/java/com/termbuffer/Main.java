package com.termbuffer;

public class Main {
    public static void main(String[] args) {
        TerminalBuffer buf = new TerminalBuffer(50, 20);

        buf.setAttributes(TermColor.BRIGHT_WHITE, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 0);
        buf.writeText("=== TerminalBuffer Demo ===");

        buf.setAttributes(TermColor.BRIGHT_RED, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 2);
        buf.writeText("writeText: bold red!");

        buf.setAttributes(TermColor.BRIGHT_GREEN, TermColor.DEFAULT, new StyleFlags(false, true, false));
        buf.setCursor(0, 3);
        buf.writeText("writeText: italic green!");

        buf.setAttributes(TermColor.BRIGHT_YELLOW, TermColor.DEFAULT, new StyleFlags(false, false, true));
        buf.setCursor(0, 4);
        buf.writeText("writeText: underline yellow!");

        buf.setAttributes(TermColor.BRIGHT_CYAN, TermColor.DEFAULT, new StyleFlags(true, true, true));
        buf.setCursor(0, 5);
        buf.writeText("writeText: bold+italic+underline cyan!");

        buf.setAttributes(TermColor.BRIGHT_YELLOW, TermColor.DEFAULT, new StyleFlags(false, false, false));
        buf.setCursor(0, 6);
        buf.writeText("insertText: [WORLD]");
        buf.setCursor(12, 6);
        buf.insertText("HELLO ");

        buf.setAttributes(TermColor.CYAN, TermColor.DEFAULT, new StyleFlags());
        buf.setCursor(0, 8);
        buf.fillLine('-');

        buf.setAttributes(TermColor.BRIGHT_MAGENTA, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 9);
        buf.writeText("cursor movement: ");
        buf.moveCursorRight(3);
        buf.writeText("<-- moved right 3");

        buf.setAttributes(TermColor.BRIGHT_CYAN, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 11);
        buf.writeText("wide chars: 中文日本語");

        buf.setAttributes(TermColor.BRIGHT_GREEN, TermColor.DEFAULT, new StyleFlags());
        buf.setCursor(0, 13);
        buf.writeText("line about to scroll into scrollback...");
        buf.insertEmptyLine();

        buf.setAttributes(TermColor.BRIGHT_WHITE, TermColor.DEFAULT, new StyleFlags());
        buf.setCursor(0, 14);
        buf.writeText("(line above scrolled off into scrollback)");

        buf.setAttributes(TermColor.BRIGHT_YELLOW, TermColor.DEFAULT, new StyleFlags(true, false, false));
        buf.setCursor(0, 16);
        buf.writeText("before resize: " + buf.getWidth() + "x" + buf.getHeight());
        buf.resize(60, 22);
        buf.setCursor(0, 17);
        buf.writeText("after resize:  " + buf.getWidth() + "x" + buf.getHeight());

        buf.setAttributes(TermColor.BRIGHT_CYAN, TermColor.DEFAULT, new StyleFlags());
        buf.setCursor(0, 19);
        buf.writeText("scrollback lines: " + buf.getScrollbackSize());

        buf.setAttributes(TermColor.CYAN, TermColor.DEFAULT, new StyleFlags());
        buf.setCursor(0, 20);
        buf.fillLine('-');

        System.out.println("\n--- scrollback ---");
        for (int i = 0; i < buf.getScrollbackSize(); i++) {
            System.out.println(buf.getLine(i).stripTrailing());
        }
        System.out.println("--- screen ---");
        renderScreen(buf);
    }

    static void renderScreen(TerminalBuffer buf) {
        for (int i = buf.getScrollbackSize(); i < buf.getTotalRows(); i++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < buf.getWidth(); col++) {
                Character ch = buf.getChar(col, i);
                CellAttributes attr = buf.getCellAttributes(col, i);
                line.append(ansiStyle(attr))
                    .append(ch != null ? ch : ' ')
                    .append("\u001B[0m");
            }
            String trimmed = line.toString().stripTrailing();
            if (!trimmed.isEmpty()) System.out.println(trimmed);
        }
    }

    static String ansiStyle(CellAttributes attr) {
        StringBuilder sb = new StringBuilder();
        if (attr.style.bold)      sb.append("\u001B[1m");
        if (attr.style.italic)    sb.append("\u001B[3m");
        if (attr.style.underline) sb.append("\u001B[4m");
        sb.append(ansiColor(attr.foreground));
        return sb.toString();
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