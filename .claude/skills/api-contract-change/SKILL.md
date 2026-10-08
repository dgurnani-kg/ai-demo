---
name: api-contract-change
description: The only sanctioned way to change api/openapi.yaml. Use whenever a feature needs a new endpoint, a new schema, or a change to an existing wire shape. Covers versioning, additionalProperties, error responses, and regenerating the frontend types.
---

# Changing the contract

`api/openapi.yaml` is the boundary between the backend, the frontend, and every agent that
works on either. It is read-only everywhere else in this repo. This is the procedure that
is allowed to change it.

## Before you edit

Answer these, in the response, before touching the file:

1. **What is the smallest change that serves the feature?** A new optional field beats a new
   endpoint. A new endpoint beats a change to an existing one.
2. **Is it breaking?** Removing a field, making an optional field required, changing a type
   or narrowing an enum breaks existing clients. If the answer is yes, say so explicitly and
   bump the minor version in `info.version`. We are pre-1.0 internally, so a breaking change
   is allowed — it is just not allowed to be silent.
3. **What is the error case?** Every operation that can fail needs the status documented and
   pointed at `#/components/responses/NotFound` or `Conflict`.

## Editing

- Every schema sets `additionalProperties: false`. Keep it. It is what makes
  `ApiContractTest` catch invented fields, which is the whole reason the contract is worth
  having.
- Every schema lists `required` explicitly. A field that is genuinely optional is marked
  `nullable: true` and left out of `required`.
- Reuse `components/schemas` and `components/responses`. Do not inline a shape that already
  exists.
- `operationId` is camelCase and unique — it becomes the generated client's function name.
- Paths are plural nouns. Sub-resources nest: `/api/books/{bookId}/reservations`.
- Integer ids are `type: integer, format: int64`. Timestamps are
  `type: string, format: date-time`.

## After you edit

Run all three. The contract is not changed until they pass:

```bash
./gradlew test --tests '*ApiContractTest'   # the spec parses, and nothing already drifted
cd frontend && npm run gen:api              # regenerate src/api/schema.d.ts
cd frontend && npm run check                # tsc against the new types
```

`npm run gen:api` will show you exactly what the frontend now expects. If the generated
diff surprises you, the spec edit was wrong — fix the spec, not the generated file. Never
hand-edit `src/api/schema.d.ts`.

## Then stop

Changing the contract is a separate step from implementing it. Report what changed and what
the three commands said. Implementation happens afterwards, in agents that treat this file
as read-only.
