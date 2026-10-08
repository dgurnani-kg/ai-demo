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

## 2. Decide whether to fan out

Fan out to `backend-impl`, `frontend-impl` and `test-author` in parallel **only when all
three are true**:

- the contract is settled and unambiguous,
- the three jobs touch disjoint paths,
- the feature is big enough that the slowest agent takes longer than the overhead.

Otherwise do it inline. A three-file change does not need three context windows, and
spawning them spends the user's tokens in a burst they did not ask for. When in doubt, do
it yourself — the fan-out is an optimisation, not a ritual.

## 3. If you do fan out

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
