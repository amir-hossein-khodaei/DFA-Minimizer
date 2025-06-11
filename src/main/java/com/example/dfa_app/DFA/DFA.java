package com.example.dfa_app.DFA;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.util.*;
import java.util.stream.Collectors;
import java.util.StringJoiner;
import javafx.application.Platform;

import com.example.dfa_app.Application_Controler;



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


    public void addState(State state) {
        if (state != null && this.states.add(state)) {
            if (this.states.size() == 1) {
                setInitialState(state);
            }
            if (controllerInstance != null) {
                controllerInstance.updateUIComponents();
            }
        }
    }


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
                controllerInstance.updateUIComponents();
            }
        }
    }

    public void addSymbol(String symbol) {
        if (symbol != null && !symbol.trim().isEmpty()) {
            if (!"?".equals(symbol.trim())) {
               if (this.alphabet.add(symbol.trim())) {
                   if (controllerInstance != null) {


                        controllerInstance.updateTransitionTable();
                   }
               }
            }
        }
    }


    public void setInitialState(State newState) {
       if (newState != null && !this.states.contains(newState)) {
           // System.err.println("[ERROR] Attempted to set a non-member state as initial: " + newState.getName()); // Removed
           // Optionally, handle this as an internal log or throw an exception
           return;
       }

       State oldInitialState = this.initialState;

       if (Objects.equals(oldInitialState, newState)) {
           return;
       }


       if (oldInitialState != null) {
           oldInitialState.setAsInitial(false);
       }


       this.initialState = newState;
       if (this.initialState != null) {
           this.initialState.setAsInitial(true);
       }


       if (controllerInstance != null) {
           controllerInstance.handleInitialStateChange(oldInitialState, newState);
           controllerInstance.updateUIComponents();
       }
    }

   public void addAcceptingState(State state) {
       if (this.states.contains(state)) {
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
       if (this.acceptingStates.remove(state)) {
           state.setAccepting(false);
           if (controllerInstance != null) {
               controllerInstance.handleAcceptingStateChange(state);
               controllerInstance.updateUIComponents();
           }
       }
   }


   public void stateNameChanged(State state, String oldName, String newName) {
       if (controllerInstance != null) {

           controllerInstance.updateUIComponents();
       }
   }



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

                if (t.getSymbol() != null && t.getNextState() != null) {
                    sb.append(String.format("  δ(%s, %s) = %s\n", state.getName(), t.getSymbol(), t.getNextState().getName()));
                }
            }
        }
        sb.append("--------------------");
        return sb.toString();
    }


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


        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }



    public void configureDFA(List<String> stateNames,
                             Set<String> alphabet,
                             String initialStateName,
                             List<String> acceptingStateNames,
                             Map<String, Map<String, String>> transitionsData) {
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


        if (controllerInstance != null) {
            controllerInstance.updateTransitionTable();
        }
    }


    public void removeUnreachableStates() {
        Set<State> reachableStates = new HashSet<>();
        Queue<State> queue = new LinkedList<>();

        if (initialState != null) {
             reachableStates.add(initialState);
             queue.add(initialState);
        } else {
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


    /**
     * Represents a pair of states (q_i, q_j).
     * We make sure the states are always in the same order (like alphabetical)
     * so that the pair (Q1, Q0) is treated exactly the same as (Q0, Q1).
     */
    static class Pair {
        String state1, state2;
        public Pair(String s1, String s2) {
            if (s1.compareTo(s2) < 0) {
                this.state1 = s1; this.state2 = s2;
            } else {
                this.state1 = s2; this.state2 = s1;
            }
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair p = (Pair) o;
            return state1.equals(p.state1) && state2.equals(p.state2);
        }
        @Override public int hashCode() { return Objects.hash(state1, state2); }
        @Override public String toString() { return "(" + state1 + ", " + state2 + ")"; }
    }

    /**
     * A helper data structure to group states together.
     * Think of it as connecting dots. If we say union(A, B) and union(B, C),
     * this structure knows that {A, B, C} all belong to the same group.
     * This is perfect for forming our new merged states.
     */
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

        log("Starting DFA Minimization...");


        Set<State> originalDFAStatesSnapshot = new HashSet<>(states);
        Map<String, Map<String, String>> originalTransitionsDataSnapshot = new HashMap<>();
        for (State s : states) {
            Map<String, String> stateTransitions = new HashMap<>();
            for (String symbol : alphabet) {
                Transition t = s.getTransition(symbol);
                if (t != null && t.getNextState() != null) {
                    stateTransitions.put(symbol, t.getNextState().getName());
                }
            }
            originalTransitionsDataSnapshot.put(s.getName(), stateTransitions);
        }


        Set<String> currentStatesNames = originalDFAStatesSnapshot.stream().map(State::getName).collect(Collectors.toSet());
        List<String> currentAlphabetList = new ArrayList<>(alphabet);
        String currentInitialStateName = initialState.getName();
        Set<String> currentAcceptingStateNames = acceptingStates.stream().map(State::getName).collect(Collectors.toSet());


        Map<String, Map<String, String>> currentTransitionsData = originalTransitionsDataSnapshot;


        log("--- PHASE 1: Finding and Removing Unreachable States ---");
        Set<String> reachableStatesNames = findReachableStates(currentInitialStateName, currentTransitionsData);
        Set<String> unreachableStatesNames = new HashSet<>(currentStatesNames);
        unreachableStatesNames.removeAll(reachableStatesNames);

        currentStatesNames = reachableStatesNames;
        currentAcceptingStateNames.retainAll(reachableStatesNames);
        Map<String, Map<String, String>> filteredTransitionsData = new HashMap<>();
        for (String stateName : currentStatesNames) {
            Map<String, String> originalTransitions = currentTransitionsData.get(stateName);
            if (originalTransitions != null) {

                Map<String, String> filtered = originalTransitions.entrySet().stream()
                    .filter(entry -> reachableStatesNames.contains(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                filteredTransitionsData.put(stateName, filtered);
            }
        }
        log("  Unreachable states removed: " + unreachableStatesNames);
        log("  Reachable states: " + currentStatesNames);


        log("\n--- PHASE 2: Initial Marking (Finding Distinguishable Pairs) ---");
        Set<Pair> allPairs = createAllPossiblePairs(currentStatesNames);
        Set<Pair> distinguishablePairs = new HashSet<>();


        log("  Applying Rule 1: Mark pairs where one state is accepting and the other is not.");
        for (Pair p : allPairs) {
            boolean p1Accepting = currentAcceptingStateNames.contains(p.state1);
            boolean p2Accepting = currentAcceptingStateNames.contains(p.state2);
            if (p1Accepting != p2Accepting) {
                distinguishablePairs.add(p);
                log("    Marked " + p + " (One accepting, one non-accepting).");
            }
        }

        // =================================================================================
        // === START OF MODIFIED CODE BLOCK FOR PHASE 3 ====================================
        // =================================================================================
        log("\n--- PHASE 3: Iterative Marking (Refining Distinguishable Pairs - Video's Exact Logic) ---");

        // Set up the potentially mergeable pairs by removing the initially marked ones.
        Set<Pair> potentiallyMergeablePairs = new HashSet<>(allPairs);
        potentiallyMergeablePairs.removeAll(distinguishablePairs);

        // This flag controls the main loop. It stays true as long as any change was made in the last major pass.
        boolean changesMadeInLastPass = true;
        int iteration = 1;

        while (changesMadeInLastPass) { // Outer loop: Continues as long as the previous pass found at least one new distinguishable pair.
            changesMadeInLastPass = false; // Assume this will be a clean pass with no changes.
            log("  Iteration " + iteration + ": Starting a new major scan. Potentially mergeable pairs: " + potentiallyMergeablePairs.size());

            // This flag controls the immediate restart of the scan within a single major iteration.
            boolean restartScan;

            do {
                restartScan = false; // At the beginning of a scan, assume we won't need to restart.

                // We MUST use an Iterator to safely remove elements while looping.
                // A new iterator is created every time the scan restarts.
                Iterator<Pair> iterator = potentiallyMergeablePairs.iterator();

                while (iterator.hasNext()) {
                    Pair p = iterator.next();

                    // Check transitions for every symbol in the alphabet.
                    for (String symbol : currentAlphabetList) {
                        String r = p.state1;
                        String s = p.state2;

                        String nextR = filteredTransitionsData.getOrDefault(r, Collections.emptyMap()).get(symbol);
                        String nextS = filteredTransitionsData.getOrDefault(s, Collections.emptyMap()).get(symbol);

                        // If a transition is missing or they go to the same state, this symbol doesn't help.
                        if (nextR == null || nextS == null || nextR.equals(nextS)) {
                            continue;
                        }

                        // Create the successor pair.
                        Pair successorPair = new Pair(nextR, nextS);

                        // THE CORE LOGIC: Check if the pair of successor states is already known to be distinguishable.
                        if (distinguishablePairs.contains(successorPair)) {
                            log("    - Marked " + p + " because its successor pair " + successorPair + " on symbol '" + symbol + "' is marked.");

                            // 1. Mark the current pair as distinguishable.
                            distinguishablePairs.add(p);

                            // 2. Safely remove it from the potentially mergeable set.
                            iterator.remove();

                            // 3. Signal that a change has occurred in the overall algorithm.
                            changesMadeInLastPass = true;

                            // 4. Signal that this specific scan must restart from the beginning (as per the video's logic).
                            restartScan = true;

                            // 5. Break from the inner 'for' loop (checking symbols), as the pair's fate is sealed.
                            break;
                        }
                    }

                    if (restartScan) {
                        // 6. If a restart is needed, break from the main 'while' loop (iterating over pairs)
                        // to allow the outer 'do-while' loop to re-execute.
                        log("      --> A pair was marked. Restarting the scan from the beginning to ensure consistency.");
                        break;
                    }
                }
                // The 'do-while' loop will repeat if 'restartScan' is true.
                // This ensures the scan of 'potentiallyMergeablePairs' always starts from the beginning after a change.
            } while (restartScan);

            if (!changesMadeInLastPass) {
                log("  Iteration " + iteration + " completed without any new pairs being marked. The algorithm has converged.");
            }

            iteration++; // Increment major iteration counter.
        }

        // =================================================================================
        // === END OF MODIFIED CODE BLOCK FOR PHASE 3 ======================================
        // =================================================================================

        log("  Final distinguishable pairs: " + distinguishablePairs);
        Set<Pair> mergeablePairs = new HashSet<>(allPairs);
        mergeablePairs.removeAll(distinguishablePairs);
        log("  Mergeable (indistinguishable) pairs: " + mergeablePairs);


        log("\n--- PHASE 4: Constructing Minimized DFA ---");


        rebuildDFAFromMinimized(currentStatesNames, originalDFAStatesSnapshot, currentStatesNames, currentInitialStateName, currentAcceptingStateNames, filteredTransitionsData, currentAlphabetList, mergeablePairs);
    }



    public void log(String message) {
        if (controllerInstance != null) {
            controllerInstance.log(message);
        }
    }

    private Set<String> findReachableStates(String startState, Map<String, Map<String, String>> transitions) {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        if (startState != null) {
            queue.add(startState);
            reachable.add(startState);
        }

        while (!queue.isEmpty()) {
            String currentState = queue.poll();
            Map<String, String> currentTransitions = transitions.get(currentState);
            if (currentTransitions != null) {
                for (String nextState : currentTransitions.values()) {
                    if (nextState != null && !reachable.contains(nextState)) {
                        reachable.add(nextState);
                        queue.add(nextState);
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

    private Map<String, Set<String>> createNewStates(Set<String> originalStates, Set<Pair> mergeablePairs) {
        log("Grouping mergeable states into new super-states...");
        UnionFind uf = new UnionFind(originalStates);
        for (Pair p : mergeablePairs) {
            uf.union(p.state1, p.state2);
        }
        log("  Initial equivalence classes: " + uf.getGroups());
        return uf.getGroups();
    }

    private void rebuildDFAFromMinimized(Set<String> oldStatesNames, Set<State> originalDFAStatesSnapshot, Set<String> currentStatesNames, String oldStartStateName, Set<String> oldAcceptingStateNames, Map<String, Map<String, String>> oldTransitions, List<String> currentAlphabetList, Set<Pair> mergeablePairs) {
        log("\n--- Rebuilding DFA from Minimized State ---");
        log("  Old Start State Name: " + oldStartStateName);
        log("  Old Accepting State Names: " + oldAcceptingStateNames);
        log("  Mergeable Pairs: " + mergeablePairs);


        log("  Clearing old DFA model, UI visuals, and resetting static state properties...");
        if (controllerInstance != null && controllerInstance.getPane() != null) {
            Platform.runLater(() -> {
                controllerInstance.getPane().getChildren().clear();
                log("    UI Pane cleared.");
            });
        }

        this.states.clear();
        this.acceptingStates.clear();
        this.initialState = null;
        State.clearAllStateNames();
        State.resetIdCounter();
        State.setSelectedState(null);


        log("  Original DFA States Snapshot Contents: (Total: " + originalDFAStatesSnapshot.size() + ")");
        originalDFAStatesSnapshot.forEach(s ->
            log("    - State: " + s.getName() + ", Pos: (" + s.getLayoutX() + ", " + s.getLayoutY() +
                "), Fill: " + s.getMainCircle().getFill() + ", Stroke: " + s.getMainCircle().getStroke() +
                ", Radius: " + s.getMainCircle().getRadius() + ", StrokeWidth: " + s.getMainCircle().getStrokeWidth() +
                ", IsInitial: " + s.isInitial() + ", IsAccepting: " + s.isAccepting())
        );


        Map<String, Set<String>> newStatesGrouping = createNewStates(currentStatesNames, mergeablePairs);

        Map<Set<String>, String> groupToNewName = new HashMap<>();
        Map<String, Set<String>> oldStateToGroup = new HashMap<>();

        List<Set<String>> sortedGroups = newStatesGrouping.values().stream()
            .sorted(Comparator.comparing(g -> g.stream().sorted().collect(Collectors.joining("/"))))
            .collect(Collectors.toList());

        log("  Creating new state names from groups...");
        for (Set<String> group : sortedGroups) {
            String newName = group.stream().sorted().collect(Collectors.joining("/"));
            groupToNewName.put(group, newName);
            for (String oldStateName : group) {
                oldStateToGroup.put(oldStateName, group);
            }
            log("    Group " + group + " maps to new name: " + newName);
        }

        Set<State> newDFAStates = new HashSet<>();
        Map<String, State> newNameToStateMap = new HashMap<>();

        log("  Creating new State objects for minimized DFA...");
        for (Set<String> group : sortedGroups) {
            String newName = groupToNewName.get(group);
            log("    Processing group: " + group + " with new name: " + newName);

            double newX = 0;
            double newY = 0;
            Color newColor = Color.LIGHTGRAY;
            double newRadius = 15;
            Color newStrokeColor = Color.BLACK;
            double newStrokeWidth = 1.0;
            boolean isNewStateInitial = false;
            boolean isNewStateAccepting = false;


            int count = 0;
            State representativeOldState = null;

            for (String oldStateNameInGroup : group) {
                log("      Attempting to find original state: " + oldStateNameInGroup);

                Optional<State> originalStateOptional = originalDFAStatesSnapshot.stream()
                    .filter(s -> s.getName().equals(oldStateNameInGroup))
                    .findFirst();

                if (originalStateOptional.isPresent()) {
                    State oldState = originalStateOptional.get();
                    log("        Found original state: " + oldState.getName() + " at (" + oldState.getLayoutX() + ", " + oldState.getLayoutY() + ")");
                    newX += oldState.getLayoutX();
                    newY += oldState.getLayoutY();
                    count++;


                    if (representativeOldState == null) {
                        representativeOldState = oldState;
                    }


                    if (oldState.isInitial()) {
                        isNewStateInitial = true;
                    }
                    if (oldState.isAccepting()) {
                        isNewStateAccepting = true;
                    }
                } else {
                    log("        [DEBUG] Original state '" + oldStateNameInGroup + "' not found in snapshot.");
                }
            }

            if (count > 0 && representativeOldState != null) {
                newX /= count;
                newY /= count;
                newColor = (Color) representativeOldState.getMainCircle().getFill();
                newRadius = representativeOldState.getMainCircle().getRadius();
                newStrokeColor = (Color) representativeOldState.getMainCircle().getStroke();
                newStrokeWidth = representativeOldState.getMainCircle().getStrokeWidth();
                log("      Calculated position for '" + newName + "' (X: " + newX + ", Y: " + newY +
                    "), Derived Fill: " + newColor + ", Derived Stroke: " + newStrokeColor +
                    ", Derived Radius: " + newRadius + ", Derived StrokeWidth: " + newStrokeWidth +
                    ", Derived IsInitial: " + isNewStateInitial + ", Derived IsAccepting: " + isNewStateAccepting);
            } else {
                 log("[WARNING] Could not find any original states for group: " + group + ". Using default visual properties and position.");
                newX = 50 + newDFAStates.size() * 100;
                newY = 50;
            }

            State newState = new State(newX, newY, newRadius, newColor, newStrokeColor, newStrokeWidth, newName, this.controllerInstance);

            if (isNewStateInitial) {
                newState.setAsInitial(true);
            }
            if (isNewStateAccepting) {
                newState.setAccepting(true);
            }
            newDFAStates.add(newState);
            newNameToStateMap.put(newName, newState);
            log("      Created new state: " + newState.getName() + ", added to newDFAStates and newNameToStateMap.");
        }



        log("  Determining new initial state (redundant check, status set during creation)...");
        State newInitialState = null;
        if (oldStartStateName != null && oldStateToGroup.containsKey(oldStartStateName)) {
            Set<String> initialGroup = oldStateToGroup.get(oldStartStateName);
            String newInitialStateName = groupToNewName.get(initialGroup);
            newInitialState = newNameToStateMap.get(newInitialStateName);
            if (newInitialState != null) {

            }
            log("    New initial state identified: " + (newInitialState != null ? newInitialState.getName() : "null (error)"));
        } else {
            log("    Could not determine new initial state (old initial state name was null or not in groups).");
        }

        log("  Determining new accepting states (redundant check, status set during creation)...");
        Set<State> newAcceptingStates = new HashSet<>();
        for (Set<String> group : sortedGroups) {
            if (!Collections.disjoint(group, oldAcceptingStateNames)) {
                String nameOfNewAcceptingState = groupToNewName.get(group);
                State newAcceptingState = newNameToStateMap.get(nameOfNewAcceptingState);
                if (newAcceptingState != null) {

                    newAcceptingStates.add(newAcceptingState);
                    log("    Added new accepting state: " + newAcceptingState.getName() + " (from group: " + group + ")");
                }
            }
        }

        log("  Adding new minimized states and transitions to DFA model and UI...");


        for (State s : newDFAStates) {
            this.states.add(s);
            if (controllerInstance != null && controllerInstance.getPane() != null) {
                Platform.runLater(() -> {
                    controllerInstance.getPane().getChildren().add(s);
                    s.setSelectionListener(controllerInstance);
                    log("    Added state '" + s.getName() + "' to UI pane.");
                });
            }
        }




        if (newInitialState != null) {
            setInitialState(newInitialState);
            log("    Successfully set new initial state: " + newInitialState.getName());
        } else {
            log("[ERROR] New initial state is null, cannot set it.");
        }

        for (State s : newAcceptingStates) {
            addAcceptingState(s);
            log("    Successfully added new accepting state: " + s.getName());
        }


        for (State newState : newDFAStates) {
            String sampleOldStateName = null;
            for (Set<String> group : sortedGroups) {
                if (groupToNewName.get(group).equals(newState.getName())) {
                    sampleOldStateName = group.iterator().next();
                    break;
                }
            }
            if (sampleOldStateName == null) {
                log("[WARNING] No sample old state name found for new state '" + newState.getName() + "'. Cannot recreate transitions.");
                continue;
            }

            Map<String, String> oldStateTransitions = oldTransitions.get(sampleOldStateName);
            if (oldStateTransitions == null) {
                log("[WARNING] No old transitions found for sample old state '" + sampleOldStateName + "' for new state '" + newState.getName() + "'.");
                continue;
            }

            for (String symbol : currentAlphabetList) {
                String oldTargetStateName = oldStateTransitions.get(symbol);
                if (oldTargetStateName != null) {
                    Set<String> targetGroup = oldStateToGroup.get(oldTargetStateName);
                    if (targetGroup != null) {
                        String newTargetStateName = groupToNewName.get(targetGroup);
                        State newTargetState = newNameToStateMap.get(newTargetStateName);
                        if (newTargetState != null) {
                            newState.addTransitionDirect(symbol, newTargetState);
                            log("    Added transition: (" + newState.getName() + ", '" + symbol + "') --> " + newTargetState.getName());


                            if (controllerInstance != null && controllerInstance.getPane() != null) {
                                Platform.runLater(() -> {

                                    Transition newTransition = new Transition(newState, this, controllerInstance);
                                    newTransition.setSymbol(symbol);
                                    newTransition.completeTransition(newTargetState);
                                    newTransition.setSelectionListener(controllerInstance);
                                    log("      Created and added visual transition: " + newTransition);
                                });
                            } else {
                                log("[WARNING] controllerInstance or pane is null for adding visual transition.");
                            }
                        } else {
                            log("[WARNING] Target state '" + newTargetStateName + "' not found in newNameToStateMap for transition from '" + newState.getName() + "' on symbol '" + symbol + "'.");
                        }
                    } else {
                        log("[WARNING] Target group for '" + oldTargetStateName + "' not found for transition from '" + newState.getName() + "' on symbol '" + symbol + "'.");
                    }
                } else {
                    log("    No transition found for state '" + newState.getName() + "' on symbol '" + symbol + "' in old transitions.");
                }
            }
        }

        log("\n=============================================");
        log("      FINAL MINIMIZED DFA RESULTS");
        log("=============================================");

        log("\nMerged States (Equivalence Classes):");
        for (Set<String> group : sortedGroups) {
            log(groupToNewName.get(group) + ": " + group);
        }

        log("\nNew DFA Transitions:");
        for (State newState : newDFAStates) {
            String sourceNewName = newState.getName();
            for (String symbol : currentAlphabetList) {
                Transition transition = newState.getTransition(symbol);
                if (transition != null && transition.getNextState() != null) {
                    String targetNewName = transition.getNextState().getName();
                    log("(" + sourceNewName + ") --" + symbol + "--> (" + targetNewName + ")");
                }
            }
        }

        log("\nNew Start and Final States:");
        log("New Start State: " + (newInitialState != null ? newInitialState.getName() : "None"));
        log("New Final States: " + newAcceptingStates.stream().map(State::getName).collect(Collectors.toSet()));
        log("---------------------------------------------");

        if (controllerInstance != null) {
            controllerInstance.updateUIComponents();
            controllerInstance.updateTransitionTable();
        }
    }


    public void addOrUpdateTransition(State fromState, String symbol, State toState) {
        if (fromState == null || toState == null || symbol == null || symbol.isEmpty()) {
            return;
        }
        this.addSymbol(symbol);

        if (controllerInstance != null) {

            controllerInstance.updateTransitionTable();
        }
    }
}