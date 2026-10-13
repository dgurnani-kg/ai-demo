---
name: vertical-slice
description: The house procedure for adding a feature that crosses contract, backend, frontend and tests. Use when a request touches more than one layer. Sets the order of work and says when to fan out to agents and when not to.
---

# Adding a vertical slice

The order is not negotiable, because each step produces the input the next one needs.

## 1. Settle the contract first

Invoke the `api-contract-change` skill. Nothing else starts until `api/openapi.yaml` is
updated and `./gradlew test --tests '*ApiContractTest'` passes against it.

This step is sequential and it is done by one actor. A contract edited by two agents at
once is not a contract.

## 2. Fan out

If the change touches **all three** of `src/main/java/**`, `frontend/src/**` and
`src/test/java/**`, fan out to `backend-impl`, `frontend-impl` and `test-author` in
parallel. That is what this skill is for, and it is not a judgement call — check the paths
and act on the answer.

Do not weigh the size of the feature. A twelve-file slice and a forty-file slice both go
through the same three lanes; estimating whether the overhead "pays" is guesswork that
produces a different answer on different days, and an inconsistent orchestration is worse
than a slightly wasteful one.

Two things must be true before you spawn anything:

- the contract is settled and unambiguous,
- the three jobs touch genuinely disjoint paths.

If either fails, fix that first. Do not substitute inline work for a contract that is not
settled — that is the one case where going inline hides a real problem.

**If the change touches only one layer**, this is the wrong skill. Do it inline and do not
spawn anything; a single-layer change does not need three context windows, and spawning
them spends tokens in a burst the user did not ask for.

## 3. What each agent gets

Each agent gets: the feature brief, the relevant contract section, and nothing else. They
cannot see each other. Resist the urge to tell `frontend-impl` what `backend-impl` is doing
— if that information matters, it belongs in the contract.

## 4. Run the gate

```bash
./gradlew check
cd frontend && npm run check
```

Both must be green. A gate failure goes back to the agent that caused it with the failure
output attached — not to a new agent, and not fixed by hand in the orchestrator, which
loses the lesson.

If the failure is "the contract does not have this field", that goes back to step 1. It
never gets patched at the implementation layer.

## 5. Report

Say what changed, what the gate said, and what you assumed. Then stop. A human reads the
diff — nothing here merges itself.
