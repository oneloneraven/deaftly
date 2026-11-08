# 9. Ship the MVP on 0.x

## Status

Accepted

## Context

Versions are cut from each PR's release label, and `release: major` means
a breaking change. Cutting `1.0.0` for the MVP would mean labelling a
non-breaking PR as major.

## Decision

The MVP ships on whatever `0.x` minor version it lands on. `1.0.0` is cut
by hand through the release workflow's manual run, once testing with deaf
users confirms the MVP.

## Consequences

- Release labels keep their meaning.
- "1.0" signals that real users have validated the app, not just that the
  backlog was finished.
