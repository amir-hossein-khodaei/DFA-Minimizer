package com.example.dfa_app.DFA;

import com.example.dfa_app.Application_Controler;
import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.stream.Collectors;

public class DFA {
    private Set<State> states;
    private Set<String> alphabet;
    private State initialState;
    private Set<State> acceptingStates;
    private Map<State, Map<String, State>> transitions;
    private final Application_Controler controllerInstance;

    public DFA(Application_Controler controller) {
        this.states = new HashSet<>();
        this.alphabet = new HashSet<>();
        this.acceptingStates = new HashSet<>();
        this.transitions = new HashMap<>();
        this.controllerInstance = controller;
        State.setDFAInstance(this);
    }

    public Application_Controler getControllerInstance() {
        return controllerInstance;
    }

    public Set<State> getStates() {
        return Collections.unmodifiableSet(this.states);
    }

    public Set<String> getAlphabet() {
        return Collections.unmodifiableSet(this.alphabet);
    }

    public State getInitialState() {
        return this.initialState;
    }

    public Set<State> getAcceptingStates() {
        return Collections.unmodifiableSet(this.acceptingStates);
    }

    public Map<State, Map<String, State>> getTransitions() {
        return Collections.unmodifiableMap(this.transitions);
    }

    public void addState(State state) {
        if (state == null) {
            return;
        }

        boolean isNewState = this.states.add(state);
        if (isNewState) {
            if (this.states.size() == 1) {
                setInitialState(state);
            }
            if (controllerInstance != null) {
                controllerInstance.updateUIComponents();
            }
        }
    }

    public void removeState(State state) {
        if (state == null) {
            return;
        }

        // Remove state from the general set of states
        boolean wasRemoved = this.states.remove(state);
        if (wasRemoved) {
            // Update initial state if the removed state was initial
            if (state.equals(this.initialState)) {
                setInitialState(null);
            }
            // Remove from accepting states if it was an accepting state
            this.acceptingStates.remove(state);

            // Remove all outgoing transitions from the removed state
            this.transitions.remove(state);

            // Collect all visual transitions that need to be removed from the pane
            List<Transition> transitionsToRemoveFromPane = new ArrayList<>();

            // Remove all incoming transitions to the removed state
            for (State s : this.states) {
                s.removeTransitionsToState(state, transitionsToRemoveFromPane);
            }
            // Also remove outgoing transitions from the state itself visually
            state.getTransitions().forEach(transitionsToRemoveFromPane::add);
            // Clear state's internal transition list as they are now handled/removed
            state.clearTransitions();

            // Remove the state from the UI pane
            if (controllerInstance != null && controllerInstance.getPane() != null) {
                controllerInstance.getPane().getChildren().remove(state);

                // Remove all collected visual transitions from the pane
                for (Transition t : transitionsToRemoveFromPane) {
                    controllerInstance.deleteTransition(t); // Use the controller's method to ensure proper cleanup
                }
            }

            // Remove name from static set in State class
            if (!state.getName().isEmpty()) { // Ensure name is not empty before removal
                State.stateNames.remove(state.getName());
            }

            // Update UI components after all removals
            if (controllerInstance != null) {
                controllerInstance.updateUIComponents();
            }
        }
    }

    public void addSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty() || "?".equals(symbol.trim())) {
            return;
        }

        String trimmedSymbol = symbol.trim();
        boolean isNewSymbol = this.alphabet.add(trimmedSymbol);

        if (isNewSymbol && controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }

    public void setInitialState(State newInitialState) {
        if (newInitialState != null && !this.states.contains(newInitialState)) {
            return;
        }

        State oldInitialState = this.initialState;
        if (Objects.equals(oldInitialState, newInitialState)) {
            return;
        }

        if (oldInitialState != null) {
            oldInitialState.setAsInitial(false);
        }

        this.initialState = newInitialState;
        if (this.initialState != null) {
            this.initialState.setAsInitial(true);
        }

        if (controllerInstance != null) {
            controllerInstance.handleInitialStateChange(oldInitialState, newInitialState);
            controllerInstance.updateUIComponents();
        }
    }

    public void addAcceptingState(State state) {
        if (state != null && this.states.contains(state)) {
            if (this.acceptingStates.add(state)) {
                state.setAccepting(true);
                if (controllerInstance != null) {
                    controllerInstance.handleAcceptingStateChange(state);
                    controllerInstance.updateUIComponents();
                }
            }
        }
    }

    public void removeAcceptingState(State state) {
        if (state != null) {
            if (this.acceptingStates.remove(state)) {
                state.setAccepting(false);
                if (controllerInstance != null) {
                    controllerInstance.handleAcceptingStateChange(state);
                    controllerInstance.updateUIComponents();
                }
            }
        }
    }

    public void stateNameChanged(State state, String oldName, String newName) {
        if (controllerInstance != null) {
            controllerInstance.updateUIComponents();
        }
    }

    public void addOrUpdateTransition(State fromState, String symbol, State toState) {
        if (fromState == null || toState == null || symbol == null || symbol.isEmpty()) {
            return;
        }

        this.addSymbol(symbol);
        this.transitions.computeIfAbsent(fromState, k -> new HashMap<>()).put(symbol, toState);

        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }

    public void clear() {
        this.states.clear();
        this.alphabet.clear();
        this.initialState = null;
        this.acceptingStates.clear();
        this.transitions.clear();

        State.clearAllStateNames();
        State.resetIdCounter();
        State.setSelectedState(null);
    }

    public void log(String message) {
        if (controllerInstance != null) {
            controllerInstance.log(message);
        }
    }

    // --- Configuration and Utility Methods ---

    public String toDotString() {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph DFA {\n");
        dot.append("    rankdir=LR;\n");
        dot.append("    node [shape = circle];\n");
        if (initialState != null) {
            dot.append("    start [shape = point, style = invis];\n");
            dot.append("    start -> \"").append(initialState.getName()).append("\";\n");
        }
        for (State state : states) {
            dot.append("    \"").append(state.getName()).append("\"");
            if (state.isAccepting()) {
                dot.append(" [shape = doublecircle]");
            }
            dot.append(";\n");
        }
        for (State state : states) {
            for (Transition transition : state.getTransitions()) {
                if (transition.getNextState() != null) {
                    dot.append("    \"").append(state.getName()).append("\" -> \"")
                       .append(transition.getNextState().getName()).append("\" [label = \"")
                       .append(transition.getSymbol()).append("\"];\n");
                }
            }
        }
        dot.append("}\n");
        return dot.toString();
    }

    public void configureDFA(List<State> stateList, Set<String> alphabet, State initialState, Set<State> acceptingStates, Map<State, Map<String, State>> transitions) {
        this.states = new HashSet<>(stateList);
        this.alphabet = new HashSet<>(alphabet);
        this.initialState = initialState;
        this.acceptingStates = new HashSet<>(acceptingStates);
        this.transitions = new HashMap<>(transitions);

        for (State s : stateList) {
            Map<String, State> stateTransitions = transitions.get(s);
            if (stateTransitions != null) {
                for (Map.Entry<String, State> entry : stateTransitions.entrySet()) {
                    s.addTransitionDirect(entry.getKey(), entry.getValue());
                }
            }
        }
        if (controllerInstance != null) {
            log("DFA configured with " + states.size() + " states, " + alphabet.size() + " symbols.");
            controllerInstance.updateUIComponents();
        }
    }

    public void configureDFA(List<String> stateNames, Set<String> alphabet, String initialStateName, List<String> acceptingStateNames, Map<String, Map<String, String>> transitionsData) {
        Map<String, State> stateMap = new HashMap<>();
        for (String name : stateNames) {
            State s = new State(50, 50, 15, Color.LIGHTGRAY, Color.BLACK, 1.0, name, this.controllerInstance);
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
        for (Map.Entry<String, Map<String, String>> entry : transitionsData.entrySet()) {
            State fromState = stateMap.get(entry.getKey());
            if (fromState == null) continue;
            for (Map.Entry<String, String> t : entry.getValue().entrySet()) {
                State nextState = stateMap.get(t.getValue());
                if (nextState != null) {
                    fromState.addTransitionDirect(t.getKey(), nextState);
                }
            }
        }
        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }

    // =================================================================================
    // =============== DFA MINIMIZATION LOGIC AND HELPER CLASSES =======================
    // =================================================================================

    static class Pair {
        String state1, state2;
        public Pair(String s1, String s2) {
            if (s1.compareTo(s2) < 0) { this.state1 = s1; this.state2 = s2; }
            else { this.state1 = s2; this.state2 = s1; }
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return state1.equals(((Pair)o).state1) && state2.equals(((Pair)o).state2);
        }
        @Override public int hashCode() { return Objects.hash(state1, state2); }
        @Override public String toString() { return "(" + state1 + ", " + state2 + ")"; }
    }

    static class UnionFind {
        private final Map<String, String> parent;
        public UnionFind(Set<String> states) {
            parent = new HashMap<>();
            for (String s : states) parent.put(s, s);
        }
        public String find(String s) {
            if (parent.get(s).equals(s)) return s;
            String root = find(parent.get(s));
            parent.put(s, root);
            return root;
        }
        public void union(String s1, String s2) {
            String root1 = find(s1);
            String root2 = find(s2);
            if (!root1.equals(root2)) parent.put(root1, root2);
        }
        public Map<String, Set<String>> getGroups() {
            Map<String, Set<String>> groups = new HashMap<>();
            for (String s : parent.keySet()) {
                String root = find(s);
                groups.computeIfAbsent(root, k -> new TreeSet<>()).add(s);
            }
            return groups;
        }
    }

    public void minimizeDFA() {
        if (initialState == null) {
            log("[ERROR] Cannot minimize DFA: No initial state defined.");
            return;
        }

        State.clearAllStateNames();
        State.resetIdCounter();
        State.setSelectedState(null);

        Set<State> originalStatesSnapshot = new HashSet<>(states);
        Map<String, Map<String, String>> transitionsSnapshot = new HashMap<>();
        for (State s : states) {
            Map<String, String> stateTransitions = new HashMap<>();
            for (String symbol : alphabet) {
                Transition t = s.getTransition(symbol);
                if (t != null && t.getNextState() != null) {
                    stateTransitions.put(symbol, t.getNextState().getName());
                }
            }
            transitionsSnapshot.put(s.getName(), stateTransitions);
        }

        Set<String> allStateNames = originalStatesSnapshot.stream().map(State::getName).collect(Collectors.toSet());
        List<String> alphabetList = new ArrayList<>(alphabet);
        String initialName = initialState.getName();
        Set<String> acceptingNames = acceptingStates.stream().map(State::getName).collect(Collectors.toSet());

        log("Starting DFA Minimization...");
        log("> States:\n" + allStateNames);
        log("Alphabet:\n" + alphabetList);
        log("> start:\n" + initialName);
        log("Accepting:\n" + acceptingNames);

        // --- PHASE 1: Removing Unreachable States ---
        log("\nPHASE 1: Unreachable States");
        Set<String> reachableNames = findReachableStates(initialName, transitionsSnapshot);
        Set<String> unreachableNames = new HashSet<>(allStateNames);
        unreachableNames.removeAll(reachableNames);

        if (unreachableNames.isEmpty()) {
            log("> All states are reachable.");
        } else {
            log("> Removed unreachable state(s):\n" + unreachableNames);
        }
        
        Set<String> stateNames = reachableNames;
        acceptingNames.retainAll(reachableNames);

        // Explicitly filter transitionsSnapshot to only include reachable states
        Map<String, Map<String, String>> filteredTransitionsSnapshot = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : transitionsSnapshot.entrySet()) {
            String fromStateName = entry.getKey();
            if (reachableNames.contains(fromStateName)) {
                Map<String, String> originalInnerMap = entry.getValue();
                Map<String, String> newInnerMap = new HashMap<>();
                for (Map.Entry<String, String> innerEntry : originalInnerMap.entrySet()) {
                    String symbol = innerEntry.getKey();
                    String targetStateName = innerEntry.getValue();
                    if (targetStateName != null && reachableNames.contains(targetStateName)) {
                        newInnerMap.put(symbol, targetStateName);
                    }
                }
                if (!newInnerMap.isEmpty() || !originalInnerMap.isEmpty()) { // Keep states with or without outgoing transitions
                    filteredTransitionsSnapshot.put(fromStateName, newInnerMap);
                }
            }
        }
        transitionsSnapshot = filteredTransitionsSnapshot; // Update transitionsSnapshot

        int statesBeforeMinimizing = stateNames.size();

        // --- PHASE 2: Initial Marking ---
        log("\nPHASE 2: Initial Marking");
        Set<Pair> allPairs = createAllPossiblePairs(stateNames);
        Set<Pair> distinguishablePairs = new HashSet<>();
        for (Pair p : allPairs) {
            boolean p1Accepting = acceptingNames.contains(p.state1);
            boolean p2Accepting = acceptingNames.contains(p.state2);
            if (p1Accepting != p2Accepting) {
                distinguishablePairs.add(p);
                log("> Marked\n" + p + "\n[Accepting/Non-Accepting]");
            }
        }
        log("> Total initially marked: " + distinguishablePairs.size());

        // --- PHASE 3: Iterative Marking ---
        log("\nPHASE 3: Iterative Marking");
        int iteration = 1;
        boolean changedInPass = true;
        while (changedInPass) {
            changedInPass = false;
            log("> Iteration " + iteration + ":");
            int markedThisIteration = 0;
            for (Pair p : allPairs) {
                if (distinguishablePairs.contains(p)) continue;
                for (String symbol : alphabetList) {
                    String next1 = transitionsSnapshot.getOrDefault(p.state1, Collections.emptyMap()).get(symbol);
                    String next2 = transitionsSnapshot.getOrDefault(p.state2, Collections.emptyMap()).get(symbol);
                    if (next1 != null && next2 != null && !next1.equals(next2)) {
                        Pair successorPair = new Pair(next1, next2);
                        if (distinguishablePairs.contains(successorPair)) {
                            log("> Check " + p + ":\non '" + symbol + "' -> " + successorPair + " [is marked] ==> Marking " + p);
                            distinguishablePairs.add(p);
                            changedInPass = true;
                            markedThisIteration++;
                            break;
                        }
                    }
                }
            }
            if(markedThisIteration == 0 && changedInPass == false){
                 log("> No new pairs were marked. Algorithm converged.");
            }
            iteration++;
        }

        // --- ANALYSIS COMPLETE ---
        Set<Pair> mergeablePairs = new HashSet<>(allPairs);
        mergeablePairs.removeAll(distinguishablePairs);
        log("\nANALYSIS COMPLETE");
        log("> Distinguishable:\n" + distinguishablePairs);
        log("> Mergeable:\n" + mergeablePairs);

        // --- PHASE 4: Construct Minimized DFA ---
        rebuildDFAFromMinimized(originalStatesSnapshot, stateNames, initialName, acceptingNames, transitionsSnapshot, mergeablePairs, statesBeforeMinimizing);
    }

    private Set<String> findReachableStates(String startState, Map<String, Map<String, String>> transitions) {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        if (startState != null) {
            queue.add(startState);
            reachable.add(startState);
        }
        while (!queue.isEmpty()) {
            String current = queue.poll();
            Map<String, String> currentTrans = transitions.get(current);
            if (currentTrans != null) {
                for (String next : currentTrans.values()) {
                    if (next != null && reachable.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return reachable;
    }

    private Set<Pair> createAllPossiblePairs(Set<String> states) {
        Set<Pair> pairs = new HashSet<>();
        List<String> sortedStates = new ArrayList<>(states);
        Collections.sort(sortedStates);
        for (int i = 0; i < sortedStates.size(); i++) {
            for (int j = i + 1; j < sortedStates.size(); j++) {
                pairs.add(new Pair(sortedStates.get(i), sortedStates.get(j)));
            }
        }
        return pairs;
    }

    private void rebuildDFAFromMinimized(Set<State> originalStates, Set<String> stateNames, String initialName, Set<String> acceptingNames, Map<String, Map<String, String>> transitions, Set<Pair> mergeablePairs, int statesBeforeMinimizing) {
        log("\nPHASE 4: Rebuilding Minimized DFA");
        
        this.clear();

        log("> Grouping states:");
        UnionFind uf = new UnionFind(stateNames);
        for (Pair p : mergeablePairs) {
            uf.union(p.state1, p.state2);
        }
        Map<String, Set<String>> groups = uf.getGroups();
        
        Map<String, State> newNameToStateMap = new HashMap<>();
        List<State> newStatesCreated = new ArrayList<>();
        
        for (Set<String> group : groups.values()) {
            String newName = String.join("/", new TreeSet<>(group));
            log("> > Group " + group + " -> new state \"" + newName + "\"");
            
            double avgX = 0, avgY = 0;
            State repr = null;
            int count = 0;
            for (String oldName : group) {
                for(State oldState : originalStates){
                    if(oldState.getName().equals(oldName)){
                        avgX += oldState.getLayoutX();
                        avgY += oldState.getLayoutY();
                        if(repr == null) repr = oldState;
                        count++;
                        break;
                    }
                }
            }
            if (count > 0) {
                avgX /= count;
                avgY /= count;
            }
            State newState = new State(avgX, avgY, repr.getMainCircle().getRadius(), (Color) repr.getMainCircle().getFill(), (Color) repr.getMainCircle().getStroke(), repr.getMainCircle().getStrokeWidth(), newName, this.controllerInstance);
            newStatesCreated.add(newState);
            newNameToStateMap.put(newName, newState);
        }

        this.states.addAll(newStatesCreated);
        
        for(State s : newStatesCreated){
            Set<String> group = groups.get(uf.find(s.getName().split("/")[0]));
            if(group.contains(initialName)) this.setInitialState(s);
            for(String name : acceptingNames){
                if(group.contains(name)){
                    this.addAcceptingState(s);
                    break;
                }
            }
        }
        
        log("> Rebuilding transitions:");
        Platform.runLater(() -> {
            if (controllerInstance != null && controllerInstance.getPane() != null) {
                controllerInstance.getPane().getChildren().clear();
            }
            for (State newState : newStatesCreated) {
                controllerInstance.getPane().getChildren().add(newState);
                newState.setSelectionListener(controllerInstance);

                String reprOldName = newState.getName().split("/")[0];
                Map<String, String> oldTransitions = transitions.get(reprOldName);
                if (oldTransitions != null) {
                    for (Map.Entry<String, String> entry : oldTransitions.entrySet()) {
                        String symbol = entry.getKey();
                        String targetOldName = entry.getValue();
                        String targetNewName = String.join("/", new TreeSet<>(groups.get(uf.find(targetOldName))));
                        State targetNewState = newNameToStateMap.get(targetNewName);

                        if (targetNewState != null) {
                            log("> > New transition: (\"" + newState.getName() + "\", '" + symbol + "') -> \"" + targetNewState.getName() + "\"");
                            Transition newT = new Transition(newState, this, controllerInstance);
                            newT.setSymbol(symbol);
                            newT.completeTransition(targetNewState);
                            newState.addTransition(newT);
                            newT.setSelectionListener(controllerInstance);
                            this.addOrUpdateTransition(newState, symbol, targetNewState);
                        }
                    }
                }
            }
            controllerInstance.updateUIComponents();
            log("\nDFA Minimization Complete.");
            log("Minimized from " + statesBeforeMinimizing + " to " + this.states.size() + " states.");
        });
    }
}