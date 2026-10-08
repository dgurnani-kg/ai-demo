export const meta = {
  name: 'vertical-slice',
  description: 'Implement a feature whose contract is already settled: backend, frontend and tests in parallel, then the gate, then a read-only review.',
  phases: [
    { title: 'Implement', detail: 'backend, frontend and tests in parallel against the frozen contract' },
    { title: 'Gate', detail: './gradlew check and npm run check' },
    { title: 'Review', detail: 'read-only pass for what the gate cannot catch' },
  ],
}

// PRECONDITION: api/openapi.yaml already describes this feature and ApiContractTest passes
// against it. This workflow does not change the contract. If the contract is not settled,
// stop and run the api-contract-change skill first.
//
// `args` is the feature brief — one or two sentences, the same text given to each agent.
const brief = typeof args === 'string' ? args : (args?.brief ?? '')

if (!brief) {
  return { error: 'No feature brief. Pass the brief as args, e.g. Workflow({name: "vertical-slice", args: "Members can reserve a book that is on loan"})' }
}

const CONTRACT_RULE = `
The contract at api/openapi.yaml is settled and READ-ONLY. A hook will block any edit to it.
If the feature needs something the contract does not have, STOP and report that as a blocker.
Do not invent the field, and do not work around the gap in your layer.
`.trim()

const LANES = [
  {
    key: 'backend',
    agent: 'backend-impl',
    task: `Implement the Spring side: migration if needed, domain, service, controller and DTOs.`,
    gate: `./gradlew check`,
  },
  {
    key: 'frontend',
    agent: 'frontend-impl',
    task: `Implement the React client: regenerate types from the contract, then the data access and the UI, including loading, empty and error states.`,
    gate: `cd frontend && npm run check`,
  },
  {
    key: 'tests',
    agent: 'test-author',
    task: `Write the tests: a row in ApiContractTest for each new endpoint and status, plus behaviour tests named after each rule in the brief.`,
    gate: `./gradlew test`,
  },
]

const REPORT_SCHEMA = {
  type: 'object',
  required: ['filesChanged', 'gatePassed', 'summary'],
  properties: {
    filesChanged: { type: 'array', items: { type: 'string' } },
    gatePassed: { type: 'boolean' },
    gateOutput: { type: 'string', description: 'The failing portion only, empty when green' },
    summary: { type: 'string' },
    assumptions: {
      type: 'array',
      items: { type: 'string' },
      description: 'Anything inferred because the brief or the contract did not say. The orchestrator cannot see your context and will not learn this any other way.',
    },
    contractGaps: {
      type: 'array',
      items: { type: 'string' },
      description: 'Fields or endpoints the work needed that the contract does not have.',
    },
  },
}

phase('Implement')

const reports = await parallel(
  LANES.map((lane) => () =>
    agent(
      [
        `Feature brief: ${brief}`,
        '',
        CONTRACT_RULE,
        '',
        `Your job: ${lane.task}`,
        '',
        `Before reporting, run: ${lane.gate}`,
        `Report honestly. A red gate reported as green costs more than a red gate.`,
      ].join('\n'),
      {
        label: `impl:${lane.key}`,
        phase: 'Implement',
        subagent_type: lane.agent,
        schema: REPORT_SCHEMA,
      },
    ).then((r) => ({ lane: lane.key, ...r })),
  ),
)

const gaps = reports.flatMap((r) => (r.contractGaps ?? []).map((g) => `${r.lane}: ${g}`))

// A contract gap is not something to patch downstream. It goes back to step 1.
if (gaps.length > 0) {
  return {
    blocked: 'contract',
    gaps,
    message: 'One or more lanes needed something the contract does not have. Re-run the api-contract-change skill, then re-run this workflow.',
    reports,
  }
}

phase('Gate')

const gate = await agent(
  [
    'Run the full gate and report the result verbatim. Do not fix anything.',
    '',
    '  ./gradlew check',
    '  cd frontend && npm run check',
    '',
    'For each failure, name the file, the check that failed, and which lane (backend, frontend or tests) owns it.',
  ].join('\n'),
  {
    label: 'gate',
    phase: 'Gate',
    schema: {
      type: 'object',
      required: ['green', 'failures'],
      properties: {
        green: { type: 'boolean' },
        failures: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              check: { type: 'string' },
              file: { type: 'string' },
              owner: { type: 'string', enum: ['backend', 'frontend', 'tests', 'unknown'] },
              output: { type: 'string' },
            },
          },
        },
      },
    },
  },
)

// Failures go back to the lane that caused them, with the output attached. Fixing them in
// the orchestrator would work and would also throw away the only signal about which agent
// brief was wrong.
if (!gate.green) {
  return {
    blocked: 'gate',
    failures: gate.failures,
    message: 'Gate is red. Send each failure back to its owning agent with the output attached, then re-run.',
    reports,
  }
}

phase('Review')

const review = await agent(
  [
    `Feature brief: ${brief}`,
    '',
    'The gate is green. Review `git diff` for what the gate cannot catch:',
    '  1. wire shapes that match the contract but not its intent',
    '  2. a rule in the brief with no test',
    '  3. an assumption a lane made that a human should confirm',
    '',
    'Assumptions the lanes reported:',
    ...reports.flatMap((r) => (r.assumptions ?? []).map((a) => `  - ${r.lane}: ${a}`)),
    '',
    'Findings most severe first. If there are none, say so in one line.',
  ].join('\n'),
  { label: 'review', phase: 'Review', subagent_type: 'contract-reviewer' },
)

return {
  brief,
  filesChanged: reports.flatMap((r) => r.filesChanged ?? []),
  assumptions: reports.flatMap((r) => (r.assumptions ?? []).map((a) => `${r.lane}: ${a}`)),
  gate: 'green',
  review,
  nextStep: 'A human reads the diff. Nothing here merges itself.',
}
