# Terminal Buffer

A terminal text buffer implementation in Java — the core data structure used by terminal emulators to store and display text. A shell sends output, the buffer updates, and the UI renders it.

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

The buffer is a 2D grid of character cells. Each cell holds a character, a foreground/background color, and style flags (bold, italic, underline).

The buffer has two parts:
- **Screen** — the visible area (e.g. 80×24), fully editable
- **Scrollback** — lines that have scrolled off the top, preserved as read-only history

A cursor tracks where the next character will be written. Writing, inserting, and filling operations all use the current cursor position and active attributes.

## Files

| File | Description |
|---|---|
| `TerminalBuffer.java` | Core buffer — screen, scrollback, cursor, all editing operations |
| `Line.java` | A single fixed-width row of cells |
| `Cell.java` | A single character slot holding a char, attributes, and wide-char flags |
| `CellAttributes.java` | Foreground color, background color, and style for a cell |
| `StyleFlags.java` | Bold, italic, and underline flags |
| `TermColor.java` | The 16 standard terminal colors plus a DEFAULT sentinel |
| `Main.java` | Demo that renders coloured text using ANSI escape codes |
| `TerminalBufferTest.java` | Unit tests covering all operations and edge cases |