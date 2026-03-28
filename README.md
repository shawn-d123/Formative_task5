# Formative Task 5 – Code Integration

**CS1704 Group Project** · Group B-27 · Brunel University London · 2025/26

## What This Is

This project brings together the individual SwiftBot Java programs built by each group member into a single, unified application. The user picks a task from a command-line menu and runs it — no switching between separate programs.

## How to Run

1. Clone the repository
2. Open the project in your Java IDE (or use the terminal)
3. Compile the Java source files
4. Run `MainMenu.java`
5. Pick a task from the menu

> Compile and run steps may change once the final launcher is locked in.

## Project Structure

```
Formative-Task-5-Code-Integration/
│
├── src/main/java/
│   ├── integration/
│   │   └── MainMenu.java        # Entry point — runs the task menu
│   ├── task1/ ... task10/        # Individual task packages
│
├── docs/
│   ├── task-allocation-sheet
│   ├── testing-notes
│   └── demo-preparation
│
├── assets/                       # Screenshots, support files
│
└── README.md
```

This structure may shift as integration progresses.

## Current Focus

- Uploading and fixing individual task code
- Integrating all tasks into one unified program
- Making the UI consistent across tasks
- Testing the integrated system
- Preparing for the tutorial demo

## Contributing

Each group member should:

1. Upload their individual code to the correct task package
2. Make sure their task compiles and runs on its own
3. Fix any errors in their own task
4. Help with integration and testing
5. Keep the team updated on progress

Use clear commit messages and push regularly.

## Demo Goal

The final application should run as a single Java program with a clear main menu, letting the user select and run any of the group's tasks. It needs to be ready for the tutorial presentation.
