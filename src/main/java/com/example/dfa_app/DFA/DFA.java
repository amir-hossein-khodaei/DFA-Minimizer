package com.example.dfa_app.DFA;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.util.*;
import java.util.stream.Collectors;
import java.util.StringJoiner;

import com.example.dfa_app.Application_Controler;

// - Core class representing a Deterministic Finite Automaton (DFA)
// - Maintains states, alphabet, transitions, and visualization components
public class DFA {
    private Set<State> states;
    private Set<String> alphabet;
    private State initialState;
    private Set<State> acceptingStates;
    private Pane pane;
    private Application_Controler controllerInstance;

    public DFA(Application_Controler controller) {
        states = new HashSet<>();
        alphabet = new HashSet<>();
        acceptingStates = new HashSet<>();
        this.controllerInstance = controller;
        State.setDFAInstance(this); // Set the static DFA instance reference in State class
    }

    // - Basic getter methods with immutability protection
    public Set<State> getStates() {
        // - Return unmodifiable view to prevent external modification
        return Collections.unmodifiableSet(this.states);
    }

    public Set<String> getAlphabet() {
        // - Return unmodifiable view
        return Collections.unmodifiableSet(this.alphabet);
    }

    public State getInitialState() {
        return this.initialState;
    }

    public Set<State> getAcceptingStates() {
        // - Return unmodifiable view
        return Collections.unmodifiableSet(this.acceptingStates);
    }

    // - Methods for modifying DFA structure
    public void addState(State state) {
        if (state != null && this.states.add(state)) {
            if (this.states.size() == 1) {
                setInitialState(state);
            }
            if (controllerInstance != null) {
                controllerInstance.updateUIComponents(); // Ensure this is called
            }
        }
    }

    // - Remove a state and handle related updates
    public void removeState(State state) {
        if (state != null && this.states.remove(state)) {
            if (state.equals(this.initialState)) {
                setInitialState(null);
            }
            this.acceptingStates.remove(state);
            for (State s : this.states) {
                s.removeTransition(null, state);
            }
            if (controllerInstance != null) {
                controllerInstance.updateUIComponents(); // Ensure this is called
            }
        }
    }

    public void addSymbol(String symbol) {
        if (symbol != null && !symbol.trim().isEmpty()) {
            if (!"?".equals(symbol.trim())) {
               if (this.alphabet.add(symbol.trim())) {
                   if (controllerInstance != null) {
                        // No need to update combo boxes just for alphabet change usually
                        // controllerInstance.updateUIComponents(); 
                        controllerInstance.updateTransitionTable(); // Only table needs alphabet usually
                   }
               }
            }
        }
    }

    // - Sets the initial state, ensuring only one state is marked as initial
    public void setInitialState(State newState) {
       if (newState != null && !this.states.contains(newState)) {
           System.err.println("[ERROR] Attempted to set a non-member state as initial: " + newState.getName());
           return;
       }

       State oldInitialState = this.initialState; // Store reference to the old initial state

       if (Objects.equals(oldInitialState, newState)) {
           return; // No change needed
       }

       // Unset the old initial state if it exists
       if (oldInitialState != null) {
           oldInitialState.setAsInitial(false); // Update flag and visual
       }

       // Set the new initial state
       this.initialState = newState;
       if (this.initialState != null) {
           this.initialState.setAsInitial(true); // Update flag and visual
       }
       
       // Notify controller about the specific change
       if (controllerInstance != null) {
           controllerInstance.handleInitialStateChange(oldInitialState, newState);
           controllerInstance.updateUIComponents(); // Ensure this is called for combos/table
       }
    }

   public void addAcceptingState(State state) {
       if (this.states.contains(state)) {
           if (this.acceptingStates.add(state)) {
               state.setAccepting(true);
               if (controllerInstance != null) {
                   controllerInstance.handleAcceptingStateChange(state);
                   controllerInstance.updateUIComponents(); // Table/Combos might need update
               }
           }
       }
   }

   public void removeAcceptingState(State state) {
       if (this.acceptingStates.remove(state)) {
           state.setAccepting(false);
           if (controllerInstance != null) {
               controllerInstance.handleAcceptingStateChange(state);
               controllerInstance.updateUIComponents(); // Table/Combos might need update
           }
       }
   }
   
   // - Called by State when its name changes
   public void stateNameChanged(State state, String oldName, String newName) {
       if (controllerInstance != null) {
           // Name change definitely affects combo boxes and table
           controllerInstance.updateUIComponents(); 
       }
   }

    // - Generate a string representation of the complete DFA
    // - Includes states, alphabet, and transition function
    public String getDFAData() {
        StringBuilder sb = new StringBuilder();

        sb.append("----- DFA Data -----\n");
        sb.append("States:\n");
        for (State state : states) {
            sb.append("  ");
            sb.append(state.getName());
            if (state.equals(initialState)) {
                sb.append(" [initial]");
            }
            if (state.isAccepting()) {
                sb.append(" [accepting]");
            }
            sb.append("\n");
        }

        sb.append("\nAlphabet: ").append(alphabet).append("\n\n");

        sb.append("Transitions:\n");
        for (State state : states) {
            for (Transition t : state.getTransitions()) {
                // - Skip incomplete transitions
                if (t.getSymbol() != null && t.getNextState() != null) {
                    sb.append(String.format("  δ(%s, %s) = %s\n", state.getName(), t.getSymbol(), t.getNextState().getName()));
                }
            }
        }
        sb.append("--------------------");
        return sb.toString();
    }

    // - Configure DFA from existing State objects and transition mappings
    public void configureDFA(List<State> stateList,
                             Set<String> alphabet,
                             State initialState,
                             Set<State> acceptingStates,
                             Map<State, Map<String, State>> transitionsMap) {
        this.states = new HashSet<>(stateList);
        this.alphabet = new HashSet<>(alphabet);
        this.initialState = initialState;
        this.acceptingStates = new HashSet<>(acceptingStates);

        for (Map.Entry<State, Map<String, State>> entry : transitionsMap.entrySet()) {
            State state = entry.getKey();
            Map<String, State> stateTransitions = entry.getValue();
            for (Map.Entry<String, State> t : stateTransitions.entrySet()) {
                String symbol = t.getKey();
                State nextState = t.getValue();
                state.addTransitionDirect(symbol, nextState);
            }
        }
        
        // - Notify controller about structural change
        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }

    // - Alternative configuration method using state names instead of objects
    // - Creates states and transitions based on provided name mappings
    public void configureDFA(List<String> stateNames,
                             Set<String> alphabet,
                             String initialStateName,
                             List<String> acceptingStateNames,
                             Map<String, Map<String, String>> transitionsData) {
        Map<String, State> stateMap = new HashMap<>();
        for (String name : stateNames) {
            // - Create states with default visual properties
            State s = new State(50, 50, 15, Color.LIGHTGRAY, name, this.controllerInstance);
            stateMap.put(name, s);
            states.add(s);
        }
        this.alphabet.addAll(alphabet);
        this.initialState = stateMap.get(initialStateName);
        for (String name : acceptingStateNames) {
            State s = stateMap.get(name);
            if (s != null) {
                s.setAccepting(true);
                acceptingStates.add(s);
            }
        }
        // - Create transitions based on name mappings
        for (Map.Entry<String, Map<String, String>> entry : transitionsData.entrySet()) {
            String stateName = entry.getKey();
            State fromState = stateMap.get(stateName);
            if (fromState == null) continue;
            Map<String, String> transForState = entry.getValue();
            for (Map.Entry<String, String> t : transForState.entrySet()) {
                String symbol = t.getKey();
                String nextStateName = t.getValue();
                State nextState = stateMap.get(nextStateName);
                if (nextState != null) {
                    fromState.addTransitionDirect(symbol, nextState);
                }
            }
        }
        
        // - Notify controller about structural change
        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }

    // - Remove states that cannot be reached from the initial state
    public void removeUnreachableStates() {
        Set<State> reachableStates = new HashSet<>();
        Queue<State> queue = new LinkedList<>();
        
        if (initialState != null) { // Check if initialState is null
             reachableStates.add(initialState);
             queue.add(initialState);
        } else {
            // If no initial state, no states are reachable (or handle differently)
            states.clear();
            acceptingStates.clear();
             if (controllerInstance != null) {
                controllerInstance.updateTransitionTable();
            }
            return;
        }

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                State nextState = (State) t.getNextState();
                if (nextState != null && reachableStates.add(nextState)) {
                    queue.add(nextState);
                }
            }
        }

        states.retainAll(reachableStates);
        acceptingStates.retainAll(reachableStates);

        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }

    // - Implement DFA minimization algorithm using equivalence classes
    public void minimizeDFA() {
        if (initialState == null) { // Cannot minimize without an initial state
            System.err.println("[ERROR] Cannot minimize DFA: No initial state defined.");
            // Optionally show an alert to the user
            return;
        }
        removeUnreachableStates(); // Ensure we start with reachable states
        if (states.isEmpty() || states.size() == 1) return; // Nothing to minimize

        List<Set<State>> partitions = new ArrayList<>();
        Set<State> acceptingPartition = new HashSet<>(acceptingStates);
        Set<State> nonAcceptingPartition = new HashSet<>(states);
        nonAcceptingPartition.removeAll(acceptingPartition);

        if (!acceptingPartition.isEmpty()) partitions.add(acceptingPartition);
        if (!nonAcceptingPartition.isEmpty()) partitions.add(nonAcceptingPartition);

        boolean partitionsChanged;
        do {
            partitionsChanged = false;
            List<Set<State>> newPartitions = new ArrayList<>();

            for (Set<State> partition : partitions) {
                if (partition.size() <= 1) { // Cannot split single-element partitions
                    newPartitions.add(partition);
                    continue;
                }
                Map<String, Set<State>> blockMap = new HashMap<>();
                for (State state : partition) {
                    StringBuilder signature = new StringBuilder();
                    // Sort alphabet to ensure consistent signature generation
                    List<String> sortedAlphabet = new ArrayList<>(this.alphabet);
                    Collections.sort(sortedAlphabet);
                    
                    for (String symbol : sortedAlphabet) {
                        Transition transition = state.getTransition(symbol);
                        State nextState = (transition != null) ? (State) transition.getNextState() : null;
                        int targetPartitionIndex = getPartitionIndex(partitions, nextState);
                        signature.append(symbol).append("-P").append(targetPartitionIndex).append(";");
                    }
                    String sig = signature.toString();
                    blockMap.computeIfAbsent(sig, k -> new HashSet<>()).add(state);
                }
                if (blockMap.size() > 1) {
                    partitionsChanged = true;
                }
                newPartitions.addAll(blockMap.values());
            }
            partitions = newPartitions;
        } while (partitionsChanged);

        rebuildDFA(partitions);
        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable(); 
        }
    }

    // - Reconstruct the DFA based on state partitions from minimization
    private void rebuildDFA(List<Set<State>> partitions) {
        Map<State, State> stateMapping = new HashMap<>();
        Set<State> newStates = new HashSet<>();
        Set<State> newAcceptingStates = new HashSet<>();
        State newInitialState = null;
        int index = 0;

        for (Set<State> partition : partitions) {
             if (partition.isEmpty()) continue; // Skip empty partitions
            State representative = partition.iterator().next(); // Pick one state from the partition
            
            // Create a new state, potentially averaging positions or using representative's
            double avgX = partition.stream().mapToDouble(State::getLayoutX).average().orElse(representative.getLayoutX());
            double avgY = partition.stream().mapToDouble(State::getLayoutY).average().orElse(representative.getLayoutY());
            
            State newState = new State(
                    avgX,
                    avgY,
                    representative.getMainCircle().getRadius(), // Use representative's radius/color
                    (Color) representative.getMainCircle().getFill(),
                    "P" + index, // Simple name based on partition index
                    this.controllerInstance
            );
            newStates.add(newState);

            boolean isAcceptingPartition = false;
            boolean containsInitial = false;
            for (State oldState : partition) {
                stateMapping.put(oldState, newState);
                if (acceptingStates.contains(oldState)) {
                    isAcceptingPartition = true;
                }
                if (oldState.equals(initialState)) {
                    containsInitial = true;
                }
            }

            if (containsInitial) {
                newInitialState = newState;
            }
            if (isAcceptingPartition) {
                newState.setAccepting(true);
                newAcceptingStates.add(newState);
            }
            index++;
        }

        // Re-wire transitions for the new states
        for (State newState : newStates) {
            // Find an old state that maps to this new state to check its transitions
            State oldRepresentative = null;
            for (Map.Entry<State, State> entry : stateMapping.entrySet()) {
                if (entry.getValue().equals(newState)) {
                    oldRepresentative = entry.getKey();
                    break;
                }
            }
            if (oldRepresentative == null) continue; // Should not happen

            for (String symbol : alphabet) {
                Transition oldTransition = oldRepresentative.getTransition(symbol);
                if (oldTransition != null) {
                    State oldTarget = (State) oldTransition.getNextState();
                    if (oldTarget != null) {
                        State newTarget = stateMapping.get(oldTarget); // Find the new state the old target belongs to
                        if (newTarget != null) {
                            // Create a direct transition in the new state
                            newState.addTransitionDirect(symbol, newTarget);
                        }
                    }
                }
            }
        }

        // Update the DFA's fields
        states = newStates;
        acceptingStates = newAcceptingStates;
        setInitialState(newInitialState); // Use the setter to handle visuals/updates
        
        // Explicitly trigger full UI update after rebuild
        if (controllerInstance != null) {
            controllerInstance.updateUIComponents(); 
        }
    }

    // - Helper method to find a state's partition during minimization
    private int getPartitionIndex(List<Set<State>> partitions, State state) {
        if (state == null) return -1; // Handle null next state (incomplete DFA)
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).contains(state)) {
                return i;
            }
        }
        return -1; // Should not happen for reachable states in complete partitions
    }

    // - Debug helper to print current partitions during minimization
    private void printPartitions(List<Set<State>> partitions) {
        int index = 0;
        System.out.println("--- Partitions ---");
        for (Set<State> partition : partitions) {
            System.out.print(" P" + index + ": { ");
            StringJoiner joiner = new StringJoiner(", ");
            partition.stream().map(State::getName).sorted().forEach(joiner::add);
            System.out.println(joiner.toString() + " }");
            index++;
        }
        System.out.println("------------------");
    }

    // - Display information about the minimized DFA
    public void printMinimizedDFA() {
        System.out.println(getDFAData()); // Use the existing data printer
    }

    // - Create or update a transition between states
    public void addOrUpdateTransition(State fromState, String symbol, State toState) {
        if (fromState == null || toState == null || symbol == null || symbol.isEmpty()) {
            return;
        }
        this.addSymbol(symbol); // This might trigger table update if symbol is new
        // Transition logic is mainly within State
        if (controllerInstance != null) {
             // Usually only the table needs update for transition changes
            controllerInstance.updateTransitionTable(); 
        }
    }
}
