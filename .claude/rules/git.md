# Git rules

## Branching

- `master` is the main branch. Never commit directly to it.
- Branch per unit of work: `feat/<slug>`, `fix/<slug>`, `docs/<slug>`,
  `chore/<slug>`, `refactor/<slug>` (e.g. `feat/nadoumi-web-public-site`).
- Commit or push **only when the user asks**.

## Commit messages

- Conventional Commits: `type(scope): summary` in the imperative, ≤ 72 chars.
  `scope` is the module (`university`, `web`, `auth`, `application`, ...).
- Body: what changed and why, wrapped at ~72 cols. Reference affected `docs/`.
- Trailers on every Claude-authored commit:

  ```
  Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
  Claude-Session: <current session URL, when one is provided>
  ```

- One logical change per commit. Don't mix a refactor with a feature.
- Never amend or force-push a branch that is shared or already reviewed.

## Pull requests

- Use the `gh` CLI. Target `master`.
- Body: problem, solution, affected modules, migration/rollback notes, test
  evidence (real output), doc updates.
- End the PR description with:

  ```
  🤖 Generated with [Claude Code](https://claude.com/claude-code)
  ```

## Before committing

- `git status` and review the diff. No unrelated files, no secrets, no
  build artifacts, no `.claude/rules/ecc/` churn unless intended.
- Run the relevant tests and state the result in the PR.

## Reference

`.claude/rules/ecc/common/git-workflow.md`. ECC skills: `git-workflow`,
`github` (installed), `pr`, `github-ops`.
