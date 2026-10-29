# CROBS — Campus ROom Booking System

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![JUnit 5](https://img.shields.io/badge/Tests-18%2F18%20Passing-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![GUI](https://img.shields.io/badge/GUI-Swing%20Desktop%20(BON1)-007ACC)]()
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20Pattern-orange)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![CI](https://github.com/lighthouse16/crobs/actions/workflows/ci.yml/badge.svg)](https://github.com/lighthouse16/crobs/actions/workflows/ci.yml)

> **PolyU COMP2021: Object-Oriented Programming (Fall 2026)**  
> Department of Computing, The Hong Kong Polytechnic University  
> Comprehensive Command-Line and Modern Desktop GUI Campus Room Booking System.

---

## 📖 Overview

**CROBS** (*Campus ROom Booking System*) is an enterprise-grade room scheduling and resource management system implemented in Java adhering strictly to Object-Oriented Design principles and the **Model-View-Controller (MVC)** architectural pattern.

The platform provides role-based access control (Superadmin, Admin, Regular User) to manage university facilities, search room availability across time slots and physical capabilities, make conflict-free reservations, inspect 7-day room schedules with privacy masking, and log all session transactions into dual HTML and canonical TXT formats.

---

## 🏛️ System Architecture

CROBS strictly follows the **Model-View-Controller (MVC)** pattern as mandated by the course specifications:

```
                            ┌─────────────────────────┐
                            │    Presentation Layer   │
                            │   (CLI & Desktop GUI)   │
                            └────────────┬────────────┘
                                         │ User Input / Events
                                         ▼
                            ┌─────────────────────────┐
                            │    Controller Layer     │
                            │  Application / GuiCtrl  │
                            └────────────┬────────────┘
                                         │ Command Execution
                                         ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                         Model Layer (Domain Core)                           │
 │                 hk.edu.polyu.comp.comp2021.crobs.model                      │
 │                                                                             │
 │   ┌───────────────┐        ┌───────────────┐        ┌───────────────────┐   │
 │   │     User      │◄───────┤     CROBS     ├───────►│       Room        │   │
 │   │ (Role-based)  │        │(Business Core)│        │  (Types/Capacity) │   │
 │   └───────┬───────┘        └───────┬───────┘        └─────────┬─────────┘   │
 │           │                        │                          │             │
 │           └───────────────┐        │        ┌─────────────────┘             │
 │                           ▼        ▼        ▼                               │
 │                       ┌─────────────────────────┐                           │
 │                       │         Booking         │                           │
 │                       │  (Conflict Validation)  │                           │
 │                       └─────────────────────────┘                           │
 └─────────────────────────────────────────────────────────────────────────────┘
```

### Architectural Highlights
- **Model Decoupling (`hk.edu.polyu.comp.comp2021.crobs.model`)**: Pure domain models containing business logic, validation rules, conflict detection, and privilege guards. 100% decoupled from any view or UI dependencies.
- **Dual Presentation**: 
  - **CLI View/Controller (`Application.java`)**: Headless command-line parser and executor with session logging.
  - **GUI View/Controller (`hk.edu.polyu.comp.comp2021.crobs.gui`)**: Modern Swing desktop interface with reactive state binding and responsive dialogs.
- **Strict Encapsulation**: Entity mutability is strictly controlled; defensive copies and unmodifiable collections protect system state against accidental mutation.

---

## 🎯 Requirements & Features Matrix

| Req ID | Command Syntax | Min Role | Description | Status |
|:-------|:---------------|:---------|:------------|:------:|
| **REQ1** | `-html <path> -txt <path>` | System | Dual session logging to HTML table and plain TXT | ✅ Done |
| **REQ2** | `addUser <name>` | Admin | Create a new regular user with auto-generated ID | ✅ Done |
| **REQ3** | `removeUser <targetUserId>` | Admin | Remove regular user & cascade cancel their bookings | ✅ Done |
| **REQ4** | `changeRole <targetUserId> <newRole>` | Superadmin | Update user role (`regularUser` / `adminUser`) | ✅ Done |
| **REQ5** | `login <userId>` | Anyone | Authenticate as existing user | ✅ Done |
| **REQ6** | `logout` | Anyone | End current user session | ✅ Done |
| **REQ7** | `addRoom <type> <capacity> <features>` | Admin | Create room (`lectureTheatre`, `computerLab`, etc.) | ✅ Done |
| **REQ8** | `modifyRoom <roomId> <field> <newValue>` | Admin | Update room capacity, type, or features | ✅ Done |
| **REQ9** | `deleteRoom <roomId>` | Admin | Delete room & cascade cancel future reservations | ✅ Done |
| **REQ10** | `book <roomId> <date> <start> <end> <purpose>` | Logged-in | Reserve room with strict overlap & conflict check | ✅ Done |
| **REQ11** | `modifyBooking <bId> <rId> <d> <s> <e> <purp>` | Owner | Modify room/slot/purpose of user's own booking | ✅ Done |
| **REQ12** | `deleteBooking <bookingId>` | Owner | Cancel user's own reservation | ✅ Done |
| **REQ13** | `search <date> <start> <end> <cap> <features>` | Anyone | Filter available rooms meeting slot, cap, and features | ✅ Done |
| **REQ14** | `schedule <roomId> <weekStartDate>` | Anyone | View 7-day room schedule (privacy-masked for non-admins) | ✅ Done |
| **REQ15** | `listRoom <roomId>` | Anyone | Inspect single room metadata | ✅ Done |
| **REQ16** | `listAllRooms` | Anyone | List all registered rooms sorted by ID | ✅ Done |
| **REQ17** | `listBookings` | Logged-in | List all active reservations owned by current user | ✅ Done |
| **REQ18** | `quit` | Anyone | Gracefully terminate program session | ✅ Done |
| **BON1** | Desktop GUI Mode | All | Full-featured Swing UI with live matrix & session logging | ✅ Done |

---

## 🖥️ Graphical User Interface (BON1)

CROBS includes a custom modern desktop GUI implementing a clean design language:

- **Adaptive Drawer & App Bar**: Instant switching between `Find Rooms`, `My Bookings`, and `Manage` (role-gated for Admin/Superadmin).
- **Interactive 7-Day Schedule Matrix**: Visual week planner for any room, displaying booking timeslots with instant owner masking for privacy.
- **Search & Quick Book Workflow**: Live filtering by capacity, date, start/end hours, and comma-delimited equipment features.
- **Visual Feedback & Modals**: Custom dialogs for room creation, user modification, and toast notifications.
- **Dual Session Logging Integration**: Seamlessly mirrors all GUI interactions into valid REQ1 HTML and TXT logs.

---

## 🚀 Quick Start

### Prerequisites
- **JDK 17 or higher** (JDK 17, 21, or 25 recommended)
- **Git**

### 1. Clone the Repository
```bash
git clone https://github.com/lighthouse16/crobs.git
cd crobs
```

### 2. Build the Project

#### On Windows:
```cmd
build.bat
```

#### On Linux / macOS:
```bash
chmod +x *.sh
./build.sh
```

---

## 💻 Running the Application

### Running Desktop GUI (BON1)
```bash
# Windows
run-gui.bat

# Linux / macOS
./run-gui.sh
```
*Or with explicit session logging:*
```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication -html gui_session.html -txt gui_session.txt
```

### Running Command-Line Interface (CLI)
```bash
# Windows
run-cli.bat

# Linux / macOS
./run-cli.sh
```
*Or with explicit parameters:*
```bash
java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.Application -html cli_session.html -txt cli_session.txt
```

---

## 🧪 Testing & Verification

The project includes unit and integration tests executing across both domain model and presentation components:

```bash
# Windows
test.bat

# Linux / macOS
./test.sh
```

### Test Coverage Highlights
- **`CROBSTest`**: 100% coverage of domain model business logic, conflict detection, user privilege boundaries, cascading cancellations, and entity immutability.
- **`CrobsGuiTest`**: Component lifecycle tests, drawer state transitions, schedule privacy masking, and session logger integration.
- **`CrobsGuiEndToEndTest`**: Comprehensive 20-step end-to-end integration scenario validating real user workflows from superadmin bootstrap to room booking and session report generation.
- **IntelliJ Code Inspection (`COMP2021_PROJECT.xml`)**: 0 errors against the official course code quality profile.

---

## 📂 Project Structure

```
crobs/
├── .github/
│   └── workflows/
│       └── ci.yml               # Automated GitHub Actions cross-platform CI
├── .idea/                       # Shared IntelliJ IDEA configuration & inspection profiles
│   ├── inspectionProfiles/
│   │   ├── COMP2021_PROJECT.xml # Official PolyU COMP2021 inspection rules
│   │   └── profiles_settings.xml
│   ├── misc.xml
│   └── modules.xml
├── lib/                         # Vendored JUnit 5 testing dependencies
│   ├── apiguardian-api-1.1.2.jar
│   ├── hamcrest-core-1.3.jar
│   ├── junit-jupiter-5.14.0.jar
│   ├── junit-jupiter-api-5.14.0.jar
│   ├── junit-jupiter-engine-5.14.0.jar
│   ├── junit-jupiter-params-5.14.0.jar
│   ├── junit-platform-commons-1.14.0.jar
│   ├── junit-platform-engine-1.14.0.jar
│   └── opentest4j-1.3.0.jar
├── src/
│   └── hk/edu/polyu/comp/comp2021/crobs/
│       ├── Application.java     # CLI Entry Point & Command Loop
│       ├── gui/                 # Desktop GUI Implementation (BON1)
│       │   ├── CrobsGuiApplication.java
│       │   ├── DrawerPanel.java
│       │   ├── GuiController.java
│       │   ├── GuiSessionLogger.java
│       │   ├── GuiTheme.java
│       │   ├── MainFrame.java
│       │   ├── dialog/          # Modal Dialogs (Login, Room, Booking, Confirm)
│       │   └── view/            # Views (FindRoomsPanel, ManagePanel, etc.)
│       └── model/               # Core MVC Domain Model
│           ├── Booking.java
│           ├── CROBS.java
│           ├── CrobsException.java
│           ├── Room.java
│           ├── RoomType.java
│           ├── User.java
│           └── UserRole.java
├── test/
│   ├── TestRunner.java          # Standalone CLI JUnit 5 reflection runner
│   └── hk/edu/polyu/comp/comp2021/crobs/
│       ├── gui/
│       │   ├── CrobsGuiEndToEndTest.java
│       │   └── CrobsGuiTest.java
│       └── model/
│           └── CROBSTest.java
├── build.bat                    # Windows build script
├── build.sh                     # Unix build script
├── run-cli.bat                  # Windows CLI launcher
├── run-cli.sh                   # Unix CLI launcher
├── run-gui.bat                  # Windows GUI launcher
├── run-gui.sh                   # Unix GUI launcher
├── test.bat                     # Windows test runner
├── test.sh                      # Unix test runner
├── Crobs.iml                    # IntelliJ IDEA module definition
├── GUI_IMPLEMENTATION_REPORT.md # Technical report for GUI architecture
├── GUI_RUN.md                   # Step-by-step GUI guide
├── LICENSE                      # MIT License
└── README.md                    # Project documentation
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
