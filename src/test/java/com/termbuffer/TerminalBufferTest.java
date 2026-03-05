package com.termbuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TerminalBufferTest {

    @Nested
    class Construction {
        @Test void bufferHasCorrectDimensions() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            assertEquals(80, buf.getWidth());
            assertEquals(24, buf.getHeight());
        }

        @Test void cursorStartsAtOrigin() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test void screenInitialisedToEmptyCells() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            for (int row = 0; row < 5; row++)
                assertEquals("          ", buf.getLine(row));
        }

        @Test void scrollbackStartsEmpty() {
            assertEquals(0, new TerminalBuffer(80, 24).getScrollbackSize());
        }
    }

    @Nested
    class Attributes {
        @Test void defaultAttributesAreDefaultColorsAndNoStyle() {
            CellAttributes attr = new TerminalBuffer(10, 5).getAttributes();
            assertEquals(TermColor.DEFAULT, attr.foreground);
            assertEquals(TermColor.DEFAULT, attr.background);
            assertEquals(new StyleFlags(), attr.style);
        }

        @Test void setAttributesPersistsForegroundAndBackground() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.setAttributes(TermColor.RED, TermColor.BLUE, new StyleFlags());
            assertEquals(TermColor.RED, buf.getAttributes().foreground);
            assertEquals(TermColor.BLUE, buf.getAttributes().background);
        }

        @Test void setAttributesPersistsStyleFlags() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.setAttributes(TermColor.DEFAULT, TermColor.DEFAULT, new StyleFlags(true, true, false));
            assertTrue(buf.getAttributes().style.bold);
            assertTrue(buf.getAttributes().style.italic);
            assertFalse(buf.getAttributes().style.underline);
        }

        @Test void writtenCharactersCarryCurrentAttributes() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.setAttributes(TermColor.GREEN, TermColor.BLACK, new StyleFlags(false, false, true));
            buf.writeText("A");
            CellAttributes attr = buf.getCellAttributes(0, buf.getScrollbackSize());
            assertEquals(TermColor.GREEN, attr.foreground);
            assertEquals(TermColor.BLACK, attr.background);
            assertTrue(attr.style.underline);
        }
    }

    @Nested
    class Cursor {
        @Test void setCursorMovesToExactPosition() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(10, 5);
            assertEquals(10, buf.getCursorCol());
            assertEquals(5, buf.getCursorRow());
        }

        @Test void setCursorClampsColumnToZero() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(-5, 0);
            assertEquals(0, buf.getCursorCol());
        }

        @Test void setCursorClampsColumnToWidthMinus1() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(100, 0);
            assertEquals(79, buf.getCursorCol());
        }

        @Test void setCursorClampsRowToZero() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(0, -3);
            assertEquals(0, buf.getCursorRow());
        }

        @Test void setCursorClampsRowToHeightMinus1() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(0, 30);
            assertEquals(23, buf.getCursorRow());
        }

        @Test void moveCursorUpDecrementsRow() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(0, 5);
            buf.moveCursorUp(3);
            assertEquals(2, buf.getCursorRow());
        }

        @Test void moveCursorUpClampsAtTop() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(0, 2);
            buf.moveCursorUp(10);
            assertEquals(0, buf.getCursorRow());
        }

        @Test void moveCursorDownIncrementsRow() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.moveCursorDown(5);
            assertEquals(5, buf.getCursorRow());
        }

        @Test void moveCursorDownClampsAtBottom() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(0, 20);
            buf.moveCursorDown(100);
            assertEquals(23, buf.getCursorRow());
        }

        @Test void moveCursorLeftDecrementsColumn() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(10, 0);
            buf.moveCursorLeft(4);
            assertEquals(6, buf.getCursorCol());
        }

        @Test void moveCursorLeftClampsAtZero() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(3, 0);
            buf.moveCursorLeft(10);
            assertEquals(0, buf.getCursorCol());
        }

        @Test void moveCursorRightIncrementsColumn() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.moveCursorRight(7);
            assertEquals(7, buf.getCursorCol());
        }

        @Test void moveCursorRightClampsAtWidthMinus1() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(75, 0);
            buf.moveCursorRight(100);
            assertEquals(79, buf.getCursorCol());
        }
    }

    @Nested
    class WriteText {
        @Test void writesCharactersAtCursorPosition() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("Hello");
            assertEquals('H', buf.getChar(0, buf.getScrollbackSize()));
            assertEquals('e', buf.getChar(1, buf.getScrollbackSize()));
        }

        @Test void cursorAdvancesByTextLength() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("Hi");
            assertEquals(2, buf.getCursorCol());
        }

        @Test void writeOverwritesExistingContent() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("AAAAA");
            buf.setCursor(0, 0);
            buf.writeText("BB");
            assertTrue(buf.getLine(buf.getScrollbackSize()).startsWith("BB"));
        }

        @Test void writeStopsAtRightEdge() {
            TerminalBuffer buf = new TerminalBuffer(5, 5);
            buf.writeText("ABCDEFGH");
            assertEquals(4, buf.getCursorCol());
        }

        @Test void writeFromMidLine() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.setCursor(3, 0);
            buf.writeText("XY");
            assertEquals('X', buf.getChar(3, buf.getScrollbackSize()));
            assertEquals('Y', buf.getChar(4, buf.getScrollbackSize()));
        }
    }

    @Nested
    class InsertText {
        @Test void insertAtStartShiftsContentRight() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("BCDE");
            buf.setCursor(0, 0);
            buf.insertText("A");
            assertTrue(buf.getLine(buf.getScrollbackSize()).startsWith("ABCDE"));
        }

        @Test void insertInMiddleShiftsTailRight() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("ABDE");
            buf.setCursor(2, 0);
            buf.insertText("C");
            assertTrue(buf.getLine(buf.getScrollbackSize()).startsWith("ABCDE"));
        }

        @Test void insertCursorAdvancesPastInsertedText() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.insertText("AB");
            assertEquals(2, buf.getCursorCol());
        }

        @Test void overflowWrapsToNextLine() {
            TerminalBuffer buf = new TerminalBuffer(5, 5);
            buf.setCursor(3, 0);
            buf.insertText("ABCD");
            assertEquals(1, buf.getCursorRow());
        }
    }

    @Nested
    class FillLine {
        @Test void fillLineWithCharacter() {
            TerminalBuffer buf = new TerminalBuffer(5, 5);
            buf.fillLine('*');
            assertEquals("*****", buf.getLine(buf.getScrollbackSize()).stripTrailing());
        }

        @Test void fillLineWithNullClearsIt() {
            TerminalBuffer buf = new TerminalBuffer(5, 5);
            buf.writeText("HELLO");
            buf.setCursor(0, 0);
            buf.fillLine();
            assertEquals("     ", buf.getLine(buf.getScrollbackSize()));
        }

        @Test void fillLineDoesNotMoveCursor() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.setCursor(3, 2);
            buf.fillLine('-');
            assertEquals(3, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());
        }
    }

    @Nested
    class InsertEmptyLine {
        @Test void insertsBlankRowAtBottom() {
            TerminalBuffer buf = new TerminalBuffer(5, 3);
            buf.writeText("AAA");
            buf.setCursor(0, 1); buf.writeText("BBB");
            buf.setCursor(0, 2); buf.writeText("CCC");
            buf.insertEmptyLine();
            assertEquals("     ", buf.getLine(buf.getScrollbackSize() + 2));
        }

        @Test void topLineMoveToScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3);
            buf.writeText("FIRST");
            buf.insertEmptyLine();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("FIRST", buf.getLine(0).stripTrailing());
        }

        @Test void scrollbackRespectsMaxSize() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 3);
            for (int i = 0; i < 10; i++) buf.insertEmptyLine();
            assertEquals(3, buf.getScrollbackSize());
        }

        @Test void scrollbackDisabledWhenMaxIsZero() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 0);
            for (int i = 0; i < 5; i++) buf.insertEmptyLine();
            assertEquals(0, buf.getScrollbackSize());
        }
    }

    @Nested
    class ClearOperations {
        @Test void clearScreenBlanksAllCells() {
            TerminalBuffer buf = new TerminalBuffer(5, 3);
            buf.writeText("HELLO");
            buf.clearScreen();
            assertEquals("     ", buf.getLine(buf.getScrollbackSize()));
        }

        @Test void clearScreenPreservesScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            buf.writeText("DATA");
            buf.insertEmptyLine();
            int before = buf.getScrollbackSize();
            buf.clearScreen();
            assertEquals(before, buf.getScrollbackSize());
        }

        @Test void clearScreenDoesNotMoveCursor() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.setCursor(3, 2);
            buf.clearScreen();
            assertEquals(3, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());
        }

        @Test void clearScreenAndScrollbackRemovesScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            for (int i = 0; i < 5; i++) buf.insertEmptyLine();
            buf.clearScreenAndScrollback();
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test void clearScreenAndScrollbackAlsoClearsScreen() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            buf.writeText("HELLO");
            buf.clearScreenAndScrollback();
            assertEquals("     ", buf.getLine(0));
        }
    }

    @Nested
    class ContentAccess {
        @Test void getCharReturnsCorrectCharacter() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("XYZ");
            assertEquals('X', buf.getChar(0, buf.getScrollbackSize()));
            assertEquals('Y', buf.getChar(1, buf.getScrollbackSize()));
            assertEquals('Z', buf.getChar(2, buf.getScrollbackSize()));
        }

        @Test void getCharReturnsNullForEmptyCell() {
            assertNull(new TerminalBuffer(10, 5).getChar(0, 0));
        }

        @Test void getCharFromScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            buf.writeText("SB");
            buf.insertEmptyLine();
            assertEquals('S', buf.getChar(0, 0));
        }

        @Test void getLineReturnsFullLineString() {
            TerminalBuffer buf = new TerminalBuffer(5, 3);
            buf.writeText("HELLO");
            assertEquals("HELLO", buf.getLine(buf.getScrollbackSize()));
        }

        @Test void getScreenContentReturnsAllScreenLines() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            buf.writeText("AAAAA");
            buf.setCursor(0, 1); buf.writeText("BBBBB");
            String content = buf.getScreenContent();
            assertTrue(content.contains("AAAAA"));
            assertTrue(content.contains("BBBBB"));
        }

        @Test void getFullContentIncludesScrollbackAndScreen() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            buf.writeText("SCRB");
            buf.insertEmptyLine();
            buf.setCursor(0, 1);
            buf.writeText("SCRN");
            String content = buf.getFullContent();
            assertTrue(content.contains("SCRB"));
            assertTrue(content.contains("SCRN"));
        }

        @Test void rowOutOfRangeThrows() {
            TerminalBuffer buf = new TerminalBuffer(5, 3);
            assertThrows(IllegalArgumentException.class, () -> buf.getChar(0, 100));
        }

        @Test void colOutOfRangeThrows() {
            TerminalBuffer buf = new TerminalBuffer(5, 3);
            assertThrows(IllegalArgumentException.class, () -> buf.getChar(10, 0));
        }
    }

    @Nested
    class WideCharacters {
        @Test void cjkCharacterOccupiesTwoColumns() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("中");
            assertEquals(2, buf.getCursorCol());
        }

        @Test void narrowCharAfterWideIsAtCorrectColumn() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            buf.writeText("中A");
            assertEquals('A', buf.getChar(2, buf.getScrollbackSize()));
        }

        @Test void wideCharAtEndOfLineDoesNotOverflow() {
            TerminalBuffer buf = new TerminalBuffer(3, 5);
            buf.setCursor(2, 0);
            buf.writeText("中");
            assertTrue(buf.getCursorCol() <= 2);
        }
    }

    @Nested
    class Resize {
        @Test void resizeUpdatesDimensions() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.resize(100, 30);
            assertEquals(100, buf.getWidth());
            assertEquals(30, buf.getHeight());
        }

        @Test void growHeightAddsBlankLinesAtBottom() {
            TerminalBuffer buf = new TerminalBuffer(5, 2);
            buf.resize(5, 4);
            assertEquals(4, buf.getHeight());
        }

        @Test void shrinkHeightPushesTopLinesToScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 4);
            buf.resize(5, 2);
            assertEquals(2, buf.getScrollbackSize());
        }

        @Test void resizeNarrowsTruncatesContent() {
            TerminalBuffer buf = new TerminalBuffer(10, 3);
            buf.writeText("ABCDEFGHIJ");
            buf.resize(5, 3);
            assertEquals(5, buf.getLine(buf.getScrollbackSize()).length());
        }

        @Test void resizeClampsCursorToNewBounds() {
            TerminalBuffer buf = new TerminalBuffer(80, 24);
            buf.setCursor(79, 23);
            buf.resize(40, 10);
            assertTrue(buf.getCursorCol() <= 39);
            assertTrue(buf.getCursorRow() <= 9);
        }

        @Test void invalidResizeDimensionsThrow() {
            TerminalBuffer buf = new TerminalBuffer(10, 5);
            assertThrows(IllegalArgumentException.class, () -> buf.resize(0, 5));
            assertThrows(IllegalArgumentException.class, () -> buf.resize(10, -1));
        }
    }
}
