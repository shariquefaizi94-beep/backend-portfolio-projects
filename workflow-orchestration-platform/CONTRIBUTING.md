# Contributing

Thank you for your interest in contributing to the Workflow Orchestration Platform.

## Development Setup

1. Install Java 21+, Maven 3.9+, and Docker
2. Clone the repository
3. Run `make infra-up` to start infrastructure
4. Run `make build` to build the project
5. Run `make test` to verify everything works

## Code Standards

- Follow existing code style and naming conventions
- Write tests for new features — both unit and integration
- Use meaningful commit messages following conventional commits
- Add JavaDoc for public methods and classes
- Update documentation when changing behavior

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes with tests
3. Ensure `make verify` passes
4. Submit a pull request with a clear description

## Architecture Principles

- Domain logic in entities and enums, not in services
- Services coordinate; domain objects enforce invariants
- All state transitions go through the state machine
- Idempotency is non-negotiable for any consumer
- Every failure path must have a defined recovery strategy
