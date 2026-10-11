# AI-Ready Repo — library-service

A deliberately small Spring Boot + React application, set up the way we think a repo should
be set up before anyone points an AI client at it. Built as the demo for the internal
session on agents, skills, workflows and guardrails.

**The app is not the point.** It is three entities and one screen. The point is everything
in `.claude/`, `CLAUDE.md`, and the two gates.

## Run it

```bash
./gradlew bootRun                 # :8080  (H2 console at /h2-console)
cd frontend && npm ci && npm run dev   # :5173, proxies /api to :8080
./check.sh                        # the gate — backend + frontend
```

Java 21 and Node 20+ required. The Gradle wrapper is committed; no local Gradle needed.

## What is where

| Path | What it is |
|---|---|
| `CLAUDE.md` | Standing orders. Loaded every turn, so kept short on purpose. |
| `api/openapi.yaml` | The contract. Read-only everywhere except one skill. |
| `.claude/agents/` | Four scoped workers, each with explicit writable paths. |
| `.claude/skills/` | Four procedures, loaded on demand. |
| `.claude/hooks/` | What actually blocks things. Not editable from a session. |
| `.claude/workflows/` | The fan-out script for a multi-layer feature. |
| `src/test/.../architecture/` | ArchUnit rules — layering, cycles, injection style. |
| `src/test/.../contract/` | Every response validated against the contract. |
| `presentation/` | The animated flow explainer for the talk. |
| `DEMO.md` | The 20-minute live runsheet. |

## The four primitives, in one line each

- **CLAUDE.md** — knowledge that is always loaded, and therefore always paid for.
- **Skill** — a procedure loaded only when relevant. Costs nothing until it is needed.
- **Agent** — a scoped worker with its own context window and restricted tools.
- **Workflow** — deterministic orchestration, with an explicit cost in tokens.
- **Hook** — the only one of these that enforces anything.

> Skills are knowledge. Agents are labour. Workflows are management. Hooks are the law.

## The guardrails, concretely

Four things fail the build, regardless of what any model decided:

1. **`ArchitectureTest.layersAreRespected`** — a controller that calls a repository.
2. **`ApiContractTest`** — a response with a field the contract never promised. Every
   schema sets `additionalProperties: false`, which is what makes this bite.
3. **`npm run check`** — UI code reading a field the contract never promised. The
   TypeScript types are generated from `api/openapi.yaml`, never hand-written.
4. **`guard-protected-paths.sh`** — edits to the contract, to an applied Flyway migration,
   or to the hooks themselves.

The first three are ordinary tests your team already knows how to write. That is the
argument: you do not need new tooling to make this safe, you need the checks you already
believe in, wired so that nothing can route around them.

## The one rule worth taking away

If a convention matters, it belongs in a test or a hook. A convention that only lives in
prose is a convention that will be broken by the third agent that reads it quickly.
