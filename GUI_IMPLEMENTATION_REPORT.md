# Campus ROom Booking System (CROBS)
## GUI Implementation Report (Bonus Feature BON1)

### 1. Executive Summary

This report documents the implementation of the Java desktop Graphical User Interface (BON1, 6 bonus points) for the PolyU COMP2021 Fall 2026 Group Project **Campus ROom Booking System (CROBS)**.

Key accomplishments:
- **Design Alignment**: Built strictly to the **CROBS V3 Full-width Desktop** workspace standard (`CROBS_GUI_V3.html`).
- **Standard Java API Only**: Built 100% using Java SE JDK 25 standard library (`javax.swing.*`, `java.awt.*`). Zero third-party GUI dependencies.
- **Strict Model Separation (MVC)**: The GUI communicates exclusively through `CROBS` public model APIs. No duplicate booking/availability logic, no reflection, no private state bypass.
- **Zero Modifications to Existing Code**: All 9 original Java source and test files remain byte-for-byte identical.

---

### 2. Baseline Source Integrity Verification

Every original Java file in the project was verified via SHA-256 checksums before and after implementation:

| Original Source File | SHA-256 Hash | Status |
|---|---|---|
| `src/hk/edu/polyu/comp/comp2021/crobs/Application.java` | `F84D5D2E2B3650748EBE875073AF9D541C852B0A783D075C4AA52EAE0D056BBF` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/Booking.java` | `153A8D28774C925FEFAC3EA5B71FA6A4C44AE52C55744725864BDCB29F3E140C` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/CROBS.java` | `8D4C9B947C89CDE025B5BDB79727E21991B1E929621F0DA290634A238B41037F` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/CrobsException.java` | `4403C63F489418BD225E8C2DC1198E86E6CD0316C1A78ED594EB50A24276F2B9` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/Room.java` | `5A1A17367810867F2E7D86D2BA3F3E9486A212EE7DCFC55DA3F9024EBD14917E` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/RoomType.java` | `E3908FE255ADD0601E24C853AD71F34150BF4CFAF731C75B438B50287D358514` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/User.java` | `A5A0AE24B942CCED5FA76B2082D502B16D3FE15B39470C29F09D40C6DBD67D25` | Verified Unchanged |
| `src/hk/edu/polyu/comp/comp2021/crobs/model/UserRole.java` | `63352834B19F61518B0CBA9602AC4F1160D8B7DBD1CD7D8E95384B763AA2CE85` | Verified Unchanged |
| `test/hk/edu/polyu/comp/comp2021/crobs/model/CROBSTest.java` | `F7B93E75A23EF31BA47CBFD83678EA64FD31D85C99567F1F1BAB4B751F6445FE` | Verified Unchanged |

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

All 17 tests passed:
- `CROBSTest` (Model unit tests): 7 / 7 passed
- `CrobsGuiTest` (GUI unit tests): 9 / 9 passed
- `CrobsGuiEndToEndTest` (Full 20-step lifecycle): 1 / 1 passed
- **Total**: 17 / 17 passed (100%)
