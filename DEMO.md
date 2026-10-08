# Demo runsheet

Twenty minutes. The app is the stage, not the act — nobody will remember the library
system. They will remember whether `.claude/` looked like something they could copy into
their own repo on Monday.

The feature you add live:

> **Members can reserve a book that is currently on loan.**

One sentence, and it forces a migration, an entity, a service rule, a controller, a
contract change, a UI, and tests. That is the smallest ask that still crosses every
boundary.

---

## Before the session

- [ ] `./check.sh` green on the meeting-room machine, on wifi, on battery
- [ ] Gradle daemon warm — run `./gradlew check` twice, the second is the one they'll see
- [ ] `npm install` already done in `frontend/`
- [ ] Backend and frontend already running in two panes (`./gradlew bootRun`, `npm run dev`)
- [ ] Terminal font size up. Notifications off. `git status` clean.
- [ ] Tags in place (below), and you have practised `git checkout` to each
- [ ] **A recorded run of the whole thing, queued and ready to play**

### Checkpoint tags

Tag the repo at each state so a derailed agent costs you ten seconds, not four minutes:

```bash
git tag demo-0-clean          # before anything
git tag demo-1-contract       # after the contract change
git tag demo-2-implemented    # after the fan-out
git tag demo-3-broken         # with the invented field in place
git tag demo-4-green          # everything passing
```

Recover with `git checkout demo-2-implemented -- .` and carry on talking.

---

## The run

### 1 · Tour the repo — 2 min

Open three files and nothing else:

- `CLAUDE.md` — 66 lines. Point at the table at the bottom: *everything above this is
  persuasion, everything in this table is enforcement.*
- `.claude/agents/backend-impl.md` — point at the **Boundaries** section. "It cannot edit
  the contract. Not 'should not' — cannot."
- `.claude/skills/api-contract-change/SKILL.md` — "this is a procedure, and it is not in
  context until it is needed."

Do not tour the Java. They can read Spring.

### 2 · The unguarded run — 2 min

In an empty directory, with no CLAUDE.md:

```
Add a reservation feature to a Spring Boot library service
```

Let it run for sixty seconds and stop it. It will invent a package layout, a persistence
approach and a wire shape, all plausible, none of them yours. Say one sentence: *"That is
what the tool does with no repo context. Everything from here is about removing that
freedom."*

This two minutes buys you the next fifteen. Do not skip it.

### 3 · The contract change — 3 min

Back in the real repo:

```
Members should be able to reserve a book that is currently on loan.
Start with the contract.
```

Watch it load the `api-contract-change` skill. Point out:

- it asked the breaking-change question before editing
- it kept `additionalProperties: false`
- it ran `npm run gen:api` and the frontend types changed with the spec

Then try to edit the contract by hand in a second session and let the **hook block it**.
That is the moment people understand the difference between a convention and a gate.

```bash
git tag demo-1-contract
```

### 4 · The fan-out — 5 min

```
Use the vertical-slice skill to implement it.
```

Three agents, three context windows, disjoint paths. **Narrate while it runs** — silence
here is the most common way this demo loses the room. Talk about:

- each agent sees the contract and its own brief, nothing else
- `backend-impl` cannot see what `frontend-impl` is writing, and does not need to
- the cost: three context windows at once, which is why this is not the default

Have the portal (`presentation/prompt-to-merge.html`, step 5 and 6) on a second screen if
you have one.

```bash
git tag demo-2-implemented
```

### 5 · The money shot — 4 min

This is the most valuable four minutes of your session. Break it deliberately:

```
In the reservations table, also show when the reservation expires.
```

There is no `expiresAt` in the contract. One of two good things happens:

- the agent **stops and says the contract does not have that field** — which is the
  behaviour you wrote into `frontend-impl.md`, and is worth pointing at; or
- it adds the field anyway, and `npm run check` fails on `tsc`, because the generated types
  come from the spec.

Either way, run the gate and show it red:

```bash
./check.sh
```

Then show the second gate too — add a repository call directly in a controller and run:

```bash
./gradlew test --tests '*ArchitectureTest'
```

`layersAreRespected` fails. Say it plainly: *"Nobody noticed this. A rule did. A reviewer
skimming 400 lines would have let it through."*

```bash
git tag demo-3-broken
```

### 6 · Green, then a human — 3 min

Fix it the right way — the field goes back through the contract skill, not around it. Run
`./check.sh`, get green, then open the diff and read it like a PR.

Close on the one line worth remembering:

> The guardrails did not remove the review. They made it short enough that you will
> actually do it.

```bash
git tag demo-4-green
```

---

## If it goes wrong

| What happens | What you do |
|---|---|
| An agent stalls or loops | `Esc`, `git checkout demo-N -- .`, keep talking |
| The fan-out takes too long | Cut to the recorded run, narrate over it |
| A gate passes when you wanted it red | You are on the wrong tag. `git checkout demo-3-broken -- .` |
| Wifi dies | The recorded run. This is why you made it. |

Never debug live. The audience learns nothing from watching you fix it, and you lose the
thread.

---

## What to say if someone asks "won't this slow us down?"

The honest answer, which lands better than a defence: *yes, for the first feature.* The
contract step is real overhead. It pays for itself the first time two people — or two
agents — work on the same endpoint at once, and it pays again every time the gate catches
something a reviewer would have waved through at 5pm on a Friday.
