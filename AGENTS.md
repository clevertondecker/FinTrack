# FinTrack — Agent Instructions

## Project context

- Read `.cursor/docs/fintrack-context` before making product or business-rule changes.
- Preserve the established domain behavior for invoices, item shares, payments, installment projections, and invoice imports.
- Treat production data and financial amounts as sensitive. Do not alter existing records or deploy changes without explicit user authorization.

## Implementation and validation

- Inspect the affected code and existing tests before changing behavior.
- Keep changes scoped to the requested task and avoid unrelated refactors.
- Add or update tests when behavior changes, then run the relevant backend and/or frontend validation commands.
- Do not log credentials, tokens, full financial documents, or raw API/server errors.
- Never commit secrets, private keys, `.env` files, or local machine configuration.

## Git workflow

- Commit messages must be in English and follow Conventional Commits when applicable, for example: `fix: display import history`.
- Before committing, inspect `git status` and stage only files related to the requested task. Never use `git add .` or `git add -A` by default.
- Preserve pre-existing local changes. Do not include `.cursor/docs/fintrack-context` in a commit unless the user explicitly requests it.
- For this personal repository, use the GitHub account `clevertondecker` for commits and pushes. Verify the commit author before pushing.
- Do not force-push, rewrite published history, or delete branches without explicit user approval.

## External systems

- Use read-only investigation first for databases and servers.
- Ask for explicit confirmation before data repairs, migrations that alter existing data, or deployments.
- Report the commit hash, validation performed, and any remaining deployment step when work is complete.
