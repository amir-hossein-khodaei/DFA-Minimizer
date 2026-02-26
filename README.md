
<!-- 
  SEO & METADATA STRATEGY:
  - Primary Keyword: "DFA Minimizer" (H1, Alt Text, Description)
  - Secondary Keywords: "JavaFX Automata Simulator", "Finite Automata Visualization", "Table-Filling Algorithm"
  - Visuals: Uses dynamic SVG generators for fresh content on every page load.
  - License: Assumed MIT based on "Open Source" intent; file generation scheduled for Phase 2.
-->

<div align="center">

<!-- HERO BANNER: Dynamic Capsule Render -->
![DFA Minimizer Header](https://capsule-render.vercel.app/api?type=waving&color=gradient&height=220&section=header&text=DFA%20Minimizer&fontSize=80&animation=fadeIn&fontAlignY=35&desc=Interactive%20Automata%20Visualization%20Engine&descAlignY=55&descSize=25)

<!-- TYPING SVG: Dynamic Value Propositions -->
<a href="https://github.com/Amkhodaei83/DFA-Minimizer">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=24&duration=3000&pause=1000&color=38BDF8&center=true&vCenter=true&width=500&lines=Visualize+Finite+Automata+in+Real-Time;Minimize+DFAs+with+Table-Filling+Logic;Educational+Tool+for+Computer+Science;Built+with+Modern+JavaFX+21" alt="Typing Animation of DFA Features" />
</a>

<br />

<!-- BADGES: High-Value Signals -->
<!-- Note: Build Status badge links to a workflow we will create in Phase 2 -->
<a href="https://github.com/Amkhodaei83/DFA-Minimizer/actions">
  <img src="https://img.shields.io/github/actions/workflow/status/Amkhodaei83/DFA-Minimizer/maven-build.yml?style=flat-square&logo=github&label=Build" alt="GitHub Build Status" />
</a>
<a href="https://github.com/Amkhodaei83/DFA-Minimizer/blob/main/LICENSE">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square&logo=opensourceinitiative" alt="MIT License" />
</a>
<a href="https://github.com/Amkhodaei83/DFA-Minimizer/releases">
  <img src="https://img.shields.io/github/v/release/Amkhodaei83/DFA-Minimizer?style=flat-square&logo=github&color=blue" alt="Latest Release" />
</a>
<a href="https://www.java.com">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
</a>
<a href="https://openjfx.io/">
  <img src="https://img.shields.io/badge/JavaFX-Modular-2A2A2A?style=flat-square&logo=java&logoColor=white" alt="JavaFX Modular Application" />
</a>

<br />
<br />

<!-- ACTION BUTTONS -->
<a href="https://github.com/Amkhodaei83/DFA-Minimizer/releases/latest">
  <kbd>⬇️ Download JAR</kbd>
</a>
&nbsp;&nbsp;
<a href="#-getting-started">
  <kbd>⚡ Quick Start</kbd>
</a>

</div>

<br />

---

## 📖 Table of Contents


<details>
<summary><strong>Expand to view full navigation</strong></summary>


- [About The Project](#-about-the-project)
  - [Key Features](#key-features)
  - [Built With](#built-with)
- [Demo & Visuals](#-demo--visuals)
- [Architecture](#architecture)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Usage Guide](#-usage-guide)
  - [Creating Automata](#creating-automata)
  - [Minimization Process](#minimization-process)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)

</details>

---

## 💡 About The Project

**DFA Minimizer** is a robust, interactive simulation platform designed to bridge the gap between abstract automata theory and visual understanding. Built for computer science students, educators, and language theory enthusiasts, this tool provides a hands-on environment to design, analyze, and optimize **Deterministic Finite Automata (DFA)**.

Unlike static textbook diagrams, DFA Minimizer offers a **dynamic canvas** where states and transitions come alive. Users can construct complex automata via drag-and-drop, configure transition logic with Bezier curves, and witness the mathematical rigor of the **Table-Filling Algorithm (Hopcroft’s/Moore’s logic derivative)** in real-time. The application logs every step of the minimization process—from identifying unreachable states to merging indistinguishable pairs—making it an essential debugging and learning companion.

### Key Features

*   **🎨 Interactive Vector Canvas:** Drag-and-drop state management with cubic Bezier curve transitions for complex graph layouts.
*   **📉 Algorithmic Minimization:** One-click execution of the table-filling algorithm to reduce DFAs to their most efficient form.
*   **🔍 Detailed Process Logging:** A transparent "console-like" log view that explains *why* states are merged or removed.
*   **📊 Real-Time Transition Table:** Automatically updates the formal definition ($Q, \Sigma, \delta, q_0, F$) as you draw.
*   **💾 State Persistence:** Save and load automata configurations (serialized graph data) to revisit complex problems.
*   **⚡ Modern JavaFX Stack:** Utilizes `ControlsFX` and `BootstrapFX` for a responsive, native-feeling UI.

### Built With

This project is engineered using a modern, modular Java ecosystem.

| Tech Stack | Description |
| :--- | :--- |
| [![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/) | **Core Language:** Leveraging records, pattern matching, and modularity. |
| [![JavaFX](https://img.shields.io/badge/JavaFX-21-2A2A2A?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/) | **GUI Framework:** Hardware-accelerated graphics and FXML-based layouts. |
| [![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/) | **Build System:** Dependency management and lifecycle automation. |
| [![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/) | **Testing:** Unit testing for state logic and minimization algorithms. |
| [![ControlsFX](https://img.shields.io/badge/ControlsFX-11.2-007396?style=for-the-badge&logo=java&logoColor=white)](https://controlsfx.github.io/) | **UI Components:** Advanced controls and dialogs. |















## 📸 Demo & Visuals

Experience the **DFA Minimizer** in action. The interface is designed to provide immediate visual feedback as you manipulate automata states.

### Before & After Minimization

<div align="center">
  <table>
    <tr>
      <td align="center">
        <b>1. Designing the Automaton</b><br/>
        <em>Complex initial state with redundant transitions.</em><br/><br/>
        <img src="image2.png" alt="Screenshot of DFA Minimizer showing a complex initial automaton design canvas" width="400" />
      </td>
      <td align="center">
        <b>2. Minimized Result</b><br/>
        <em>Optimized output after applying the table-filling algorithm.</em><br/><br/>
        <img src="image1.png" alt="Screenshot of DFA Minimizer showing the minimized result and log output" width="400" />
      </td>
    </tr>
  </table>
</div>

### System Architecture

 The application strictly follows the **Model-View-Controller (MVC)** architectural pattern to ensure separation of concerns and maintainability.

<div align="center">
  <img src="UML.png" alt="Screenshot of DFA Minimizer showing the minimized result and log output"  /> 
</div>

---

## 🚀 Getting Started

Follow these steps to set up the project locally. This application is built with **Java 21** and relies on **Maven** for dependency management.

### Prerequisites

*   **Java Development Kit (JDK) 21** or higher.
*   **Maven 3.8+** (or use the included `mvnw` wrapper).
*   **Git** for version control.

### Installation

1.  **Clone the repository**
    ```bash
    git clone https://github.com/Amkhodaei83/DFA-Minimizer.git
    cd DFA-Minimizer
    ```

2.  **Build the project**
    Use the Maven wrapper to ensure the correct version is used. This command cleans the target directory and packages the application into a JAR.
    ```bash
    ./mvnw clean package
    # On Windows, use: .\mvnw.cmd clean package
    ```

3.  **Run the application**
    You can run the app directly via the JavaFX Maven plugin:
    ```bash
    ./mvnw javafx:run
    ```
    
    *Alternatively, run the built JAR from the `target/` directory:*
    ```bash
    java -jar target/DFA_app-1.0-SNAPSHOT.jar
    ```

---

## 🎮 Usage Guide

The **DFA Minimizer** canvas is interactive. Master the controls to build automata efficiently.

### ⌨️ Controls & Shortcuts

| Action | Shortcut / Gesture | Description |
| :--- | :--- | :--- |
| **New State** | <kbd>Ctrl</kbd> + <kbd>N</kbd> | Enters "State Placement Mode". Click anywhere on the canvas to drop a new state. |
| **Add Transition** | <kbd>Ctrl</kbd> + **Drag** | Hold <kbd>Ctrl</kbd>, click a source state, and drag to a target state to create a connection. |
| **Minimize DFA** | <kbd>Ctrl</kbd> + <kbd>R</kbd> | Triggers the minimization algorithm. Watch the log panel for step-by-step details. |
| **Delete Item** | <kbd>Ctrl</kbd> + <kbd>D</kbd> | Removes the currently selected state or transition. |
| **Edit Label** | **Double Click** | Edit the name of a state or the symbol of a transition. |
| **Context Menu** | **Right Click** | Selects an object (State/Transition) for property editing in the left panel. |

### 🛠️ Core Workflow

1.  **Define States:**
    *   Use the **New State** button or shortcut.
    *   Select a state to toggle its properties: **Initial** (Green Arrow) or **Accepting** (Double Circle).
    
2.  **Create Transitions:**
    *   Connect states using the drag gesture.
    *   **Curved Lines:** The application uses Bezier curves automatically. Drag the control point (red handle) to adjust the curve's arc.
    *   **Symbols:** Ensure every transition has a defined symbol from your alphabet.

3.  **Execute Minimization:**
    *   Click **Start Process** (or <kbd>Ctrl</kbd> + <kbd>R</kbd>).
    *   The application will:
        1.  Identify and remove unreachable states.
        2.  Group indistinguishable states using the table-filling method.
        3.  Re-render the optimized DFA on the canvas.
    *   *Check the log panel on the right for a mathematical breakdown of the process.*
















---

## 🔮 Roadmap

We are actively working to expand the capabilities of DFA Minimizer. Current development goals include:

- [x] **Core Minimization Engine** (Table-Filling Algorithm)
- [x] **Interactive Canvas** (Draggable states, Bezier curves)
- [x] **Basic Persistence** (Save/Load DFA structure)
- [ ] **Undo/Redo Stack** — Full history management for all canvas actions.
- [ ] **NFA Support** — Native support for Non-Deterministic Finite Automata and NFA-to-DFA conversion.
- [ ] **Step-by-Step Animation** — Visualizing the token consumption process on the graph.
- [ ] **Export Options** — Export diagrams to PNG, SVG, or LaTeX (TikZ).
- [ ] **Regex Integration** — Convert Regular Expressions directly to DFAs.

See the [open issues](https://github.com/Amkhodaei83/DFA-Minimizer/issues) for a full list of proposed features (and known bugs).

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

### Workflow

1.  **Fork the Project**
2.  **Create your Feature Branch** (`git checkout -b feature/AmazingFeature`)
3.  **Commit your Changes** (`git commit -m 'Add some AmazingFeature'`)
4.  **Push to the Branch** (`git push origin feature/AmazingFeature`)
5.  **Open a Pull Request**

### Development Setup

The project uses standard Maven directory structures. 
- **Controller Logic:** Located in `src/main/java/com/example/dfa_app/Application_Controler.java`.
- **DFA Logic:** Located in `src/main/java/com/example/dfa_app/DFA/`.
- **Styles:** Modify `src/main/resources/com/example/dfa_app/styles2.css` for UI theming.

*Please ensure all new logic includes corresponding JUnit tests where applicable.*

---

## 📝 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

## 📧 Contact & Acknowledgments

**Project Maintainer:** [Amkhodaei83](https://github.com/Amkhodaei83)

### Acknowledgments

*   **[ControlsFX](https://github.com/controlsfx/controlsfx)** — For the advanced JavaFX UI components.
*   **[BootstrapFX](https://github.com/kordamp/bootstrapfx)** — For the modern look and feel.
*   **Hopcroft, Motwani, & Ullman** — For the foundational automata theory concepts utilized in this tool.
*   **Capsule Render** — For the dynamic header art.





<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&height=100&section=footer" width="100%" alt="Footer Wave">
</div>
