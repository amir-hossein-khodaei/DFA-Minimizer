# Contributing to dfa-minimizer-javafx

Thank you for your interest in this project! Contributions that improve the
educational value, UI clarity, or algorithmic correctness are especially welcome.

## Prerequisites

Before contributing, ensure your environment has:
- **JDK 21+** — [Download here](https://adoptium.net/)
- **Apache Maven** — [Download here](https://maven.apache.org/)
- An IDE with JavaFX support (IntelliJ IDEA recommended)

## How to Contribute

1. Fork the repository.
2. Clone your fork locally.
3. Create a descriptive branch:
   `git checkout -b feat/nfa-to-dfa-conversion`
4. Make your changes, then build and test locally:
   `mvn clean javafx:run`
5. Commit using Conventional Commits (see below).
6. Push to your fork and open a Pull Request against `main`.

## What to Contribute

- 🐛 **Bug fixes** — incorrect minimization output, UI rendering issues
- 🎓 **Educational improvements** — better step-by-step explanations in the UI
- ⚙️ **New algorithms** — e.g., NFA-to-DFA conversion, epsilon-closure
- 📚 **Documentation** — Javadoc for public classes and methods

## Code Style

- Follow standard **Java naming conventions**:
  CamelCase for classes, camelCase for methods and variables.
- Indentation: **4 spaces** (no tabs).
- All public interfaces and non-trivial algorithms **must** have Javadoc.
- No raw types — use generics (e.g., `List<State>` not `List`).
- Follow the existing **MVC architecture**: keep logic out of controllers.

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` A new feature or algorithm
- `fix:` A bug fix
- `docs:` Documentation-only changes
- `refactor:` Code restructuring without behavior change
- `style:` Formatting, whitespace (no logic change)
- `test:` Adding or correcting tests

## Pull Request Checklist

Before submitting, verify:
- [ ] Project builds cleanly with `mvn clean javafx:run`
- [ ] No new compiler warnings introduced
- [ ] Javadoc added for any new public methods
- [ ] PR description explains the *why*, not just the *what*
