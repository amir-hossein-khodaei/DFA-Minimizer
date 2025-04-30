# DFA Minimizer

A visual application for creating, editing, and minimizing Deterministic Finite Automata (DFAs).

## Project Overview

DFA Minimizer is a JavaFX application that allows users to:

- Create and edit DFAs through an intuitive graphical interface
- Add and modify states and transitions
- Set initial and accepting states
- Automatically minimize DFAs using the partitioning algorithm
- Remove unreachable states
- Visualize DFA structure and transitions

This tool is useful for students learning about automata theory, computer scientists working with formal languages, and educators teaching these concepts.

## Features

- **Interactive DFA Editor**: Create and manipulate DFA components with drag-and-drop functionality
- **Visual State Management**: Add, remove, and reposition states with intuitive controls
- **Transition Creation**: Create and edit transitions between states with custom symbols
- **Minimization Algorithm**: Automatically minimize DFAs to their simplest equivalent form
- **State Properties**: Configure states as initial or accepting states
- **Transition Table**: View and edit DFA transitions in a convenient table format
- **Dynamic Visualization**: Real-time visual updates as you modify the automaton
- **Multi-panel Interface**: Separate panels for editing, visualization, and feedback

## Installation Instructions

### Prerequisites

- Java 11 or higher
- JavaFX SDK 11 or higher
- Maven (for building from source)

### Setup

1. **Clone the repository**
   ```
   git clone https://github.com/yourusername/dfa-minimizer.git
   cd dfa-minimizer
   ```

2. **Build with Maven**
   ```
   mvn clean package
   ```

3. **Run the application**
   ```
   java -jar target/dfa-minimizer.jar
   ```

   Alternatively, if using an IDE:
   ```
   mvn javafx:run
   ```

## Usage Guide

### Creating a DFA

1. **Add States**: Click the "New State" button or press Ctrl+N, then click on the canvas to place a state
2. **Set State Properties**: Select a state and use the State Settings panel to:
   - Name the state
   - Set it as an initial state
   - Set it as an accepting state

3. **Add Transitions**: Click the "New Transition" button, then:
   - Click on the source state
   - Click on the destination state
   - Enter the transition symbol

### Minimizing a DFA

1. Create or load a complete DFA
2. Press the "Start Process" button or press Ctrl+R
3. The application will automatically:
   - Remove unreachable states
   - Group equivalent states
   - Display the minimized DFA

### Keyboard Shortcuts

- **Ctrl+N**: Create a new state
- **Ctrl+T**: Create a new transition
- **Ctrl+R**: Minimize the DFA
- **Delete**: Remove selected state or transition

## Code Architecture

The application follows the Model-View-Controller (MVC) architecture:

### Model
- `DFA`: Core class representing the automaton with its states and alphabet
- `State`: Represents individual states with properties (initial, accepting)
- `Transition`: Manages connections between states with their symbols

### View
- JavaFX FXML layouts define the user interface
- Custom components visualize the automaton elements
- CSS styling for consistent appearance

### Controller
- `Application_Controler`: Manages UI interactions and updates
- Handles selection events, state changes, and minimization operations

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add some amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines

When contributing, please follow these guidelines:

- Maintain the existing code style and naming conventions
- Add comments for complex algorithms or behaviors
- Update unit tests for any changed functionality
- Update documentation for new features or changes

## Troubleshooting

Common issues and solutions:

- **JavaFX not found**: Ensure you have the correct JavaFX SDK installed and properly referenced
- **States can't be selected**: Make sure you're not in "New State" or "New Transition" mode
- **Minimization not working**: Verify your DFA is complete (transitions for all alphabet symbols from each state)

## Future Enhancements

Planned features for future releases:

- Undo/redo functionality for all operations
- Save/load DFAs to common formats
- Animation of DFA execution on input strings
- Export as image or PDF
- Multiple automata comparison

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built with JavaFX for the graphical user interface
- Uses the partitioning algorithm for DFA minimization
- Inspired by automata theory and formal language concepts
