---
description: Investigate a Nadoumi bug or test failure — reproduce, root-cause, propose the minimal fix.
argument-hint: <symptom / failing test / error>
---

Investigate: **$ARGUMENTS**

Use the `nadoumi-debugger` agent and `superpowers:systematic-debugging`.

Reproduce first (failing test, `curl`, or log line). Isolate via `trace_path` /
git bisect. State the root cause with file:line evidence before proposing a fix.
Give the minimal fix plus the regression test that would have caught it, and note
any sibling code paths with the same latent bug. Apply the fix only if asked.
