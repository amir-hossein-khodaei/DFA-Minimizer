# DFA Minimizer

An interactive JavaFX application for creating, visualizing, and minimizing Deterministic Finite Automata (DFAs). This tool is designed as an educational resource for students, computer scientists, and educators to understand the principles of automata theory through a hands-on, graphical interface.

This application allows users to:

- Create and edit DFAs through an intuitive graphical canvas.
- Visualize DFA structure and transitions in real-time.
- Automatically minimize DFAs using a table-filling algorithm.
- Learn from a detailed log of the entire minimization process.

## Features

- **Interactive Canvas:** A user-friendly drag-and-drop interface to create and arrange DFA states and transitions.
- **Visual State Management:** Add, rename, delete, and reposition states. Designate states as initial or accepting with simple checkbox controls.
- **Intuitive Transition Editing:** Create transitions between states with a `Ctrl+Click` gesture. Edit transition symbols and adjust the curvature of transition arrows for clarity.
- **Real-time Transition Table:** The application automatically generates and updates a transition table that reflects the current state of the DFA on the canvas.
- **One-Click DFA Minimization:** Implements the table-filling algorithm to minimize the DFA by removing unreachable states and merging indistinguishable states.
- **Detailed Process Logging:** The minimization process is fully logged, showing each step from identifying unreachable states to marking distinguishable pairs and grouping mergeable states.

## Screenshots

**1. Designing a complex DFA (Initial State).**
![DFA Minimizer Initial State](image2.png)

**2. The resulting minimized DFA and the detailed process log.**
![DFA Minimizer Minimized DFA](image1.png)

## Installation Instructions

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Maven (for building from source)

### Running the Application

#### From GitHub Releases (Recommended)

1. Go to the [Releases page](https://github.com/Amkhodaei83/DFA/releases) of this repository.
2. Download the latest `DFA_app.zip` file.
3. extract the file.
4. run the `run.bat` file.

#### Building and Running from Source

1. **Clone the repository:**
   
   ```bash
   git clone https://github.com/Amkhodaei83/DFA.git
   cd DFA
   ```

2. **Build with Maven:**
   
   ```bash
   mvn clean install
   ```
   
   This will create the runnable JAR file in the `target/` directory.

3. **Run the application from source build:**
   
   ```bash
   java -jar target/DFA_app-1.0-SNAPSHOT.jar
   ```

## Usage Guide

### Creating a DFA

1. **Add a State**: Click the "New State" button or press `Ctrl+N`, then click on the canvas to place a state.
2. **Set State Properties**: Select a state to open the **State Settings** panel. Here you can:
   - Name the state.
   - Set it as the initial state.
   - Set it as an accepting state.
3. **Add a Transition**:
   - Hold `Ctrl` and `Primary Click` on the source state.
   - Drag your cursor to the destination state and release.
   - Select the new transition to edit its symbol.

### Minimizing a DFA

1. Ensure your DFA is complete (i.e., every state has an outgoing transition for each symbol in the alphabet).
2. Press the **Minimize DFA** button or press `Ctrl+R`.
3. The application will automatically remove unreachable states, group equivalent states, and display the minimized DFA on the canvas.

### Keyboard Shortcuts

- **`Ctrl+N`**: Create a new state.
- **`Ctrl+R`**: Minimize the DFA.
- **`Ctrl+D`** or **`Delete`**: Remove the selected state or transition.

## Code Architecture

The application follows the Model-View-Controller (MVC) design pattern:

- **Model**:
  
  - `DFA`: The core class representing the automaton, its states, alphabet, and transitions. Contains the minimization logic.
  - `State`: Represents an individual state with properties (name, initial, accepting) and its outgoing transitions.
  - `Transition`: Manages a connection from a source state to a destination state for a given symbol.

- **View**:
  
  - `Main_DFA.fxml`: Defines the layout and components of the user interface.
  - `styles2.css`: Provides the styling for a consistent and modern appearance.
  - Custom visual nodes (`State.java`, `Transition.java`, `CurvedArrow.java`) render the DFA on the canvas.

- **Controller**:
  
  - `Application_Controler.java`: Manages all UI interactions, handles user input, and coordinates updates between the model (DFA) and the view (canvas and settings panels).
  
  
  
  <img title="" src="file:///E:/user/Desktop/DFA-Minimizer/New%20folder/DFA-Minimizer/UML.png" alt="UML.png" data-align="center" width="657">

## Contributing

Contributions are welcome!

## Future Enhancements

- Undo/redo functionality for all operations.
- Save/load DFAs to and from a custom file format.
- Animation of DFA execution on input strings.
- Support for Non-deterministic Finite Automata (NFAs) and NFA-to-DFA conversion.
- Export the canvas as an image (PNG, SVG).

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.

## Acknowledgments

- Built with **JavaFX** for the graphical user interface.
- Inspired by concepts from formal language and automata theory.
