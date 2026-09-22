# Package Structure

## Overview

The Fitness Platform backend is organized as a modular monolith.

The codebase follows a feature-first package organization. Business code should be grouped by domain capability rather than by technical layer.

The root package is:

`com.fitnessplatform`

## Business Modules

Business features should be introduced as top-level packages under the root package when the corresponding functionality is implemented.

Example:

```text
com.fitnessplatform
├── auth
├── program
├── training
├── coaching
└── messaging
```

These packages are examples only. Packages must not be created before the corresponding feature exists.

## Module Structure

Modules should start with the simplest structure that keeps related code together.

For example:

```text
com.fitnessplatform.exercise
├── Exercise.java
├── ExerciseRepository.java
├── ExerciseService.java
├── ExerciseController.java
├── ExerciseRequest.java
└── ExerciseResponse.java
```

Additional internal packages may be introduced when the module becomes large enough to justify them.

The project should not introduce architectural layers or abstractions before they solve an actual complexity problem.

## Shared Code

Shared packages must be introduced only when code is genuinely shared between multiple modules.

A generic `shared` package must not become a dumping ground for unrelated helpers, utilities, base classes, or abstractions.

Technical configuration that applies to the application as a whole may live under a dedicated root-level `config` package when such configuration exists.

## Dependency Guidelines

Modules should have clear responsibilities and avoid circular dependencies.

A module should not access another module's persistence implementation merely for convenience.

Cross-module interactions should remain explicit and should be refactored only when the actual domain requires stronger boundaries.

## Database Ownership

Flyway owns database schema evolution.

Hibernate must not create or mutate the database schema automatically.

Domain entities and repositories will be introduced only alongside the business modules that require them.

## General Rules

- Prefer feature-first organization over global technical-layer packages.
- Do not create empty packages for future features.
- Do not create abstractions without a concrete use case.
- Keep related business code close together.
- Introduce subpackages only when module complexity justifies them.
- Keep cross-module dependencies explicit.
- Preserve the modular monolith architecture as the application grows.