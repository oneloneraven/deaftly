# 6. Wire dependencies by hand

## Status

Accepted

## Context

The app has a small dependency graph: a template store, a settings store,
a text recogniser and a few view models. Dependency-injection frameworks
(Hilt, Koin) add annotation processing or runtime lookup, build time and
concepts to learn.

## Decision

One `AppContainer`, created by the `Application`, constructs and holds the
app's dependencies. View models receive what they need through their
constructors, created with a `ViewModelProvider.Factory`. Tests construct
classes directly with fakes.

## Consequences

- No framework, no generated code, and dependencies are visible in plain
  Kotlin.
- If the graph grows enough that hand-wiring becomes error-prone, adopt a
  framework in a superseding ADR.
