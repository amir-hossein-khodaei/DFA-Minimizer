package com.example.dfa_app;

// - Interface for handling selection events in the DFA application
// - Implemented by components that need to respond to object selection changes
public interface SelectionListener {
    void onSelected(Object obj);
    void onDeselected(Object obj);
}
