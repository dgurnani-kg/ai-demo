# library-service

Spring Boot 3 + Java 21 backend, React + Vite + TypeScript client, H2 and Flyway.

This file is loaded on **every turn**, so it only contains rules that would otherwise be
guessed wrong. Anything that is a procedure lives in `.claude/skills/` and loads on demand.
Anything that must be enforced lives in a test or a hook, not here — see "What is actually
enforced" at the bottom.

## Layout

```
api/openapi.yaml            the contract — source of truth for every wire shape
src/main/java/com/konrad/library/
  <feature>/web/            controllers + DTOs. The only package that knows about HTTP.
  <feature>/service/        business rules. Returns *View records, never entities.
  <feature>/domain/         JPA entities + Spring Data repositories.
  common/                   errors, the Clock bean
src/main/resources/db/migration/   Flyway. Never edit an applied migration.
frontend/src/               React client. Types are generated from the contract.
```

Features are `catalog`, `membership`, `lending`. They depend in one direction only
(`lending -> catalog`, `lending -> membership`). To invert a dependency, declare an
interface in the package that needs it and implement it in the package that owns the data —
see `catalog/service/CopiesOnLoan`.

## Rules

- **The contract is read-only.** Do not edit `api/openapi.yaml` directly. It changes only
  through the `api-contract-change` skill. A hook blocks the edit if you try.
- **Never edit a migration that has been applied.** Add `V{n+1}__description.sql`.
- Controllers may call services. Controllers may not call repositories.
- Services return `*View` records. Entities never leave the `domain` package.
- Constructor injection only. No `@Autowired` on fields.
- `java.time` only. Inject the `Clock` bean rather than calling `Instant.now()`.
- Four spaces, 100 columns. Spotless decides; don't argue with it.
- Every new endpoint needs: a contract entry, a controller test, and a row in
  `ApiContractTest`.

## Commands

```bash
./gradlew check            # spotless + all tests. This is the gate.
./gradlew bootRun          # :8080, H2 console at /h2-console
cd frontend && npm run dev  # :5173, proxies /api to :8080
cd frontend && npm run check # generates types from the contract, then tsc --noEmit
```

Prefer `./gradlew test --tests '*ArchitectureTest'` while iterating — it is the fastest
signal that a change broke the structure.

## What is actually enforced

Everything above is persuasion. These run whether or not anyone read this file:

| Check | Where | Catches |
|---|---|---|
| `ArchitectureTest` | `src/test/.../architecture/` | layer violations, package cycles, field injection |
| `ApiContractTest` | `src/test/.../contract/` | responses that drift from `api/openapi.yaml` |
| `spotlessCheck` | Gradle | formatting |
| `guard-protected-paths` | `.claude/hooks/` | edits to the contract or to applied migrations |
| `npm run check` | frontend | UI reading fields the contract never promised |

If a rule here matters, move it into that table. A convention that only lives in prose is a
convention that will be broken by the third agent that reads it quickly.
