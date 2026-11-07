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

#### Standard Run (Clean State per Official Specification)
By default, the application starts with one default Superadmin (`u1`), no user logged in, and 0 preloaded rooms or bookings:
```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication
```

#### Run with Prototype Demonstration Data
To launch the GUI with pre-seeded campus rooms, users, and reservations:
```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication --demo
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

The current suite contains 70 model, command-pattern, GUI, and end-to-end tests (70 passed on desktop; 67 passed, 3 display tests skipped headless). All tests pass in the provided runner.

```bash
java -cp "out;lib/*" TestRunner
```

Included suites:
1. `hk.edu.polyu.comp.comp2021.crobs.model.CROBSTest`
2. `hk.edu.polyu.comp.comp2021.crobs.command.UndoRedoTest`
3. `hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiTest`
4. `hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiEndToEndTest`

---

### 4. Running the Original CLI Application

The CLI and GUI remain separate presentation modes over the same model:

```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.Application -html cli_session.html -txt cli_session.txt
```
