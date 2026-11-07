# Campus ROom Booking System (CROBS)
## GUI Implementation Report (Bonus Feature BON1)

### 1. Executive Summary

This report documents the implementation of the Java desktop Graphical User Interface (BON1, 6 bonus points) for the PolyU COMP2021 Fall 2026 Group Project **Campus ROom Booking System (CROBS)**.

Key accomplishments:
- **Design Alignment**: Designed following the internal **CROBS V3 Full-width Desktop** workspace prototype (`CROBS_GUI_V3.html`) while strictly fulfilling all requirements in the official course specification (`project_description.pdf`).
- **Standard Java API Only**: Built 100% using Java SE JDK 25 standard library (`javax.swing.*`, `java.awt.*`). Zero third-party GUI dependencies.
- **Strict Model Separation (MVC)**: The GUI communicates exclusively through `CROBS` public model APIs. No duplicate booking/availability logic, no reflection, no private state bypass.
- **Integrated Model Evolution**: GUI, CLI, and model were evolved together to support both bonus features while preserving MVC boundaries.

---

### 2. Source Integration

BON1 and BON2 share the same domain model and command logging rules. The implementation keeps the model independent from Swing while allowing both CLI and GUI controllers to use the same validated operations.

---

### 3. Architecture Overview

The system strictly follows the Model-View-Controller (MVC) architectural pattern:

```text
                        ┌────────────────────────┐
                        │         CROBS          │
                        │    Java Domain Model   │
                        └───────────┬────────────┘
                                    │
                                    │ (public API calls only)
                                    ▼
                        ┌────────────────────────┐
                        │     GuiController      │
                        │   Presentation Facade  │
                        └─────┬────────────┬─────┘
                              │            │
            ┌─────────────────┴─┐        ┌─┴────────────────┐
            ▼                   ▼        ▼                  ▼
   ┌─────────────────┐ ┌──────────────┐ ┌─────────────┐ ┌─────────────┐
   │    MainFrame    │ │ View Panels  │ │ HTML Logger │ │ TXT Logger  │
   │  Top Navigation │ │ Screens A–E  │ │  Table log  │ │ Command log │
   └─────────────────┘ └──────────────┘ └─────────────┘ └─────────────┘
```

#### New Source File Map
```text
src/hk/edu/polyu/comp/comp2021/crobs/gui/
├── CrobsGuiApplication.java         # Desktop GUI main entry point (-html, -txt args)
├── MainFrame.java                  # Top nav bar, avatar account menu, CardLayout
├── GuiController.java              # Facade bridging Swing views to CROBS model
├── GuiSessionLogger.java           # REQ1 independent HTML and TXT CLI session logger
├── GuiTheme.java                   # CROBS V3 visual design tokens, fonts, and controls
├── view/
│   ├── FindRoomsPanel.java         # Screen A (Filter & results) + Screen B (Booking drawer)
│   ├── MyBookingsPanel.java        # Screen C (User reservations with edit/cancel)
│   ├── RoomSchedulePanel.java      # Screen D (7-day weekly schedule with privacy masking)
│   └── ManagePanel.java            # Screen E (Admin rooms and users management)
└── dialog/
    ├── LoginDialog.java            # Sign-in modal dialog
    ├── ConfirmationDialog.java     # Destructive confirmation modal
    ├── ModifyBookingDialog.java    # Edit booking modal (REQ11)
    └── RoomEditorDialog.java       # Add / edit room modal (REQ7, REQ8)
```

---

### 4. Specification Compliance Matrix

| Requirement | Description | GUI Implementation |
|---|---|---|
| **REQ1** | Session logging (`-html`, `-txt`) | `GuiSessionLogger` records canonical CLI commands into HTML table and TXT log. |
| **REQ2** | Add user (Admin) | `ManagePanel` (Users tab) & `GuiController.addUser(name)`. |
| **REQ3** | Remove user (Admin) | `ManagePanel` (Users tab) with confirmation modal. |
| **REQ4** | Change user role (Superadmin) | `ManagePanel` (Users tab) role selector with immediate model synchronization. |
| **REQ5** | Login | `LoginDialog` and Top Nav account pill menu. |
| **REQ6** | Logout | Top Nav account dropdown menu "Sign out". |
| **REQ7** | Add room (Admin) | `ManagePanel` "Add room" button & `RoomEditorDialog`. |
| **REQ8** | Modify room (Admin) | `ManagePanel` "Edit" button & `RoomEditorDialog` (single field change). |
| **REQ9** | Delete room (Admin) | `ManagePanel` "Delete" button with `ConfirmationDialog`. |
| **REQ10** | Book room | Screen B right drawer in `FindRoomsPanel`. |
| **REQ11** | Modify booking | `MyBookingsPanel` "Edit" button & `ModifyBookingDialog`. |
| **REQ12** | Cancel booking | `MyBookingsPanel` "Cancel" button with `ConfirmationDialog`. |
| **REQ13** | Search available rooms | `FindRoomsPanel` multi-criteria search toolbar & real-time filter. |
| **REQ14** | Weekly room schedule | `RoomSchedulePanel` 7-day card grid with REQ14 user privacy masking. |
| **REQ15** | Inspect single room | `ManagePanel` room inspector dialog. |
| **REQ16** | List all rooms | `ManagePanel` rooms table displaying full inventory. |
| **REQ17** | List user bookings | `MyBookingsPanel` interactive table with empty state handling. |
| **REQ18** | Exit program | Top nav bar "Quit" button and window close handler calling `controller.quit()`. |
| **BON1** | Java Swing GUI (6 pts) | Fully implemented, tested, and documented. |

---

### 5. Automated Verification Results

The complete 59-test suite passes (59 passed on desktop; 56 passed, 3 display tests skipped headless), including model invariants, command undo/redo authorization, GUI components, and the end-to-end lifecycle.
