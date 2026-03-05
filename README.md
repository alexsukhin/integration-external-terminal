# Terminal Buffer

A terminal text buffer implementation in Java — the core data structure used by terminal emulators to store and display text. When a shell sends output, the buffer updates and the UI renders it.

## Requirements

- JDK 21+
- Gradle 8.7+

## Setup

```bash
gradle wrapper --gradle-version 8.7
```

## Run

```bash
./gradlew run
```

## Test

```bash
./gradlew test
```

## How it works

The buffer is a 2D grid of character cells. Each cell holds:
- A character (or empty)
- A foreground and background color (one of 16 standard terminal colors, or DEFAULT)
- Style flags: bold, italic, underline

The buffer has two logical parts:

- **Screen** - the visible area (e.g. 80×24), fully editable. This is what the user sees.
- **Scrollback** - lines that have scrolled off the top of the screen, preserved as read-only history. Bounded by a configurable max size.

A cursor tracks where the next character will be written. Writing, inserting, and filling operations all use the current cursor position and active attributes.

## Features

### Attributes
Set the current foreground color, background color, and style flags. All subsequent edits use these attributes until changed.

### Cursor
Get and set the cursor position by column and row. Move it up, down, left, or right by N cells. The cursor is always clamped to screen bounds.

### Editing
- **writeText** - writes text at the cursor, overwriting existing content. Stops at the right edge.
- **insertText** - inserts text at the cursor, shifting existing content right. Wraps to the next line on overflow.
- **fillLine** - fills the entire current row with a character (or empty).

### Screen operations
- **insertEmptyLine** - scrolls the screen up by one line, pushing the top line into scrollback.
- **clearScreen** - blanks all cells on the screen without touching scrollback.
- **clearScreenAndScrollback** - blanks the screen and discards all scrollback history.

### Content access
All content-access methods use a unified row index across scrollback and screen:
- `getChar(col, row)` - get the character at a position
- `getCellAttributes(col, row)` - get the attributes at a position
- `getLine(row)` - get a row as a plain string
- `getScreenContent()` - get the full visible screen as a string
- `getFullContent()` - get scrollback + screen as a string

### Wide characters
Characters that occupy two terminal columns (CJK ideographs, fullwidth forms) are handled correctly. A wide character occupies a left cell and a right placeholder cell. Cursor movement accounts for the extra width.

### Resize
Change the screen dimensions at any time. Lines wider than the new width are truncated; shorter lines are padded. Shrinking height pushes top screen lines into scrollback. The cursor is clamped to the new bounds.

## Design decisions

- **ArrayDeque for screen and scrollback** - O(1) push and pop at both ends, good cache locality.
- **Immutable Cell and CellAttributes** - cells are never mutated in place, avoiding aliasing bugs and making the code easy to reason about.
- **Unified row index** - content-access methods address scrollback and screen with a single index, keeping the API simple.
- **writeText does not wrap** - it stops at the right edge. Wrapping is handled by insertText only, keeping the two operations predictable and distinct.

## Files

| File | Description |
|---|---|
| `TerminalBuffer.java` | Core buffer — screen, scrollback, cursor, all editing and access operations |
| `Line.java` | A single fixed-width row of cells |
| `Cell.java` | A single character slot: char, attributes, and wide-char flags |
| `CellAttributes.java` | Foreground color, background color, and style for a cell |
| `StyleFlags.java` | Bold, italic, and underline flags |
| `TermColor.java` | The 16 standard terminal colors plus a DEFAULT sentinel |
| `Main.java` | Demo that exercises all features and renders with ANSI escape codes |
| `TerminalBufferTest.java` | Unit tests covering all operations and edge cases |