---
name: code-review
description: Review a change in this repo — run the local gate first, then read the diff for what the gate cannot catch. Use before opening a PR, before asking for review, or when a change is ready but unverified. Reports findings; does not fix them.
---

# Reviewing a change

The order matters. Running the gate first is not a formality — it decides what is worth
reading. There is no point spending attention on formatting or layer violations that a
test already catches, and no point reviewing a diff that does not compile.

## 1. Run the gate first

```bash
./check.sh
```

Or the halves separately while iterating:

```bash
./gradlew check                      # spotless, ArchitectureTest, ApiContractTest, behaviour tests
cd frontend && npm run check         # regenerates types from the contract, then tsc --noEmit
```

**If it is red, stop reviewing.** Report the failures and what owns each one. A red gate is
not a review finding; it is a change that is not ready to be read. Send it back.

**If it is green, say so with numbers** — how many tests ran — and move on. Do not restate
what passed.

## 2. Read the diff for what the gate cannot catch

```bash
git diff                             # uncommitted
git diff main...HEAD                 # a branch
```

This is the whole job. The gate covers formatting, layering, cycles, field injection and
contract drift. You are looking for what is green and still wrong:

**Contract intent.** The shape matches `api/openapi.yaml`, but does the field mean what the
spec says it means? A `dueAt` that is actually a borrow date passes every check.

**Commented-out violations.** ArchUnit analyses bytecode, so a layer violation that is
commented out is invisible to it. Commented-out code that reaches around the service layer
is a landmine — flag it, and say it should be deleted rather than commented.

**Migrations.** A new `V{n+1}__` file, never an edit to an applied one. Does the entity
mapping match the DDL? `ddl-auto: validate` catches a mismatch at startup, but only for
tables a test actually touches.

**Rules with no test.** A business rule named in the ticket or the commit message that no
test encodes. This is the most valuable finding and the one the gate structurally cannot
make.

**Generated files edited by hand.** `frontend/src/api/schema.d.ts` is generated from the
contract. A manual edit there is always wrong — the fix belongs in the spec, through the
`api-contract-change` skill.

**Clock and time.** `Instant.now()` instead of the injected `Clock` bean. Compiles, passes,
and makes the test unpinnable later.

**Scope.** Files in the diff that have nothing to do with the stated change. Say so and
move on — do not review them in depth.

## 3. For a read that is not anchored on yours

```
Use the contract-reviewer agent to review this diff.
```

It gets a fresh context window and read-only tools, so it cannot be steered by whatever
conclusion you have already reached, and it cannot quietly fix what it finds. Give it the
diff and the brief — not your opinion of the diff.

Worth doing when the change is large, when you wrote it yourself, or when the gate is green
and something still feels off.

## 4. Report

Findings most severe first. For each one:

- the file and line
- what is wrong, in one sentence
- the concrete input or scenario that makes it fail

If there are none, say so in one line. Do not pad a clean review.

**Do not fix anything.** A review that silently repairs what it finds teaches nobody which
brief was wrong, and the next change makes the same mistake. Report, then let the author
or the owning agent fix it.
