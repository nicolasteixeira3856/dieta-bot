# SDD maintenance plans

Shared workflow maintenance follows [the SDD policy](../README.md) and [AGENTS.md](../../../AGENTS.md). These plans own agent instructions and supporting validation tooling, not client or server implementation. Product specifications and accepted ADRs retain their existing owners.

## Active

- [SD2 — Documentation authority and lifecycle](sd2-documentation-authority.md): `Aguardando aprovação`; single writer, rules-only specs, spec update at Completion, stale root docs removed.
- [SD3 — Documentation checker and search ignores](sd3-docs-checker-and-ignores.md): `Aguardando aprovação`; `tools/check-docs.mjs`, `.gitignore`/`.ignore` fixes, orphaned wire tools removed. Requires SD2.

## Completed

- [SD1 — Repository skill alignment](completed/sd1-skills-alignment.md): `Concluído`; fourteen local skills corrected, Google resources preserved, CLI guidance verified, four complete mirrors tracked and a read-only checker validated with fixtures and workflow scenarios.

## Lifecycle

Apply the states and completion rules in the SDD policy. This index records plan state; it does not authorize implementation. State directories are created only when a plan moves into them.
