# CROBS Java Swing GUI (BON1) — Execution Guide

## Quick Start

### 1. Compile the Application

From the `Crobs` directory:

```bash
# Windows PowerShell / CMD
javac -encoding UTF-8 -cp "lib/*;src;test" -d out (Get-ChildItem -Recurse -Filter *.java src, test | Select-Object -ExpandProperty FullName)
```

Or on Unix/macOS:
```bash
javac -encoding UTF-8 -cp "lib/*:src:test" -d out $(find src test -name "*.java")
```

---

### 2. Launch the Desktop GUI Application

#### Standard Run
```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication
```

#### Run with REQ1 Session Logging
The application supports standard logging options:
- `-html <filepath>`: Records all user actions into an HTML table log.
- `-txt <filepath>`: Records all user actions into a plain text canonical command log.

```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication -html gui_session.html -txt gui_session.txt
```

---

### 3. Run Test Suites

The test suite runs both unit and end-to-end integration tests (17 total tests, 100% pass rate).

```bash
java -cp "out;lib/*" TestRunner
```

Included tests:
1. `hk.edu.polyu.comp.comp2021.crobs.model.CROBSTest` (7 tests - Model)
2. `hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiTest` (9 tests - GUI Components & Facade)
3. `hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiEndToEndTest` (1 test - 20-step complete lifecycle)

---

### 4. Running the Original CLI Application

The original CLI application remains 100% intact and untouched:

```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.Application -html cli_session.html -txt cli_session.txt
```
