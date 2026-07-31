AGENTS
======

Purpose
-------
This document defines how automated agents (human-assistants, AI copilots, CI bots) should interact with this repository. It standardizes behavior, permissions, prompt conventions, and expected outputs so agents can operate safely and productively in an enterprise codebase.

Agent Roles
-----------
- Human-in-the-loop: Suggest changes, run tests, review PRs. Must approve any agent-created change before merge.
- Assistive AI Agent: Proposes code edits, generates tests, and creates draft PRs. Must follow the `SKILL.md` standards and include reproducible commands and tests.
- CI/Automation Agent: Runs builds, linters, security scans, and merges when gated policies pass.

Safe Operation Rules
--------------------
- Never commit secrets. Use environment variables or secret stores (Docker secrets, Jenkins credentials). `.env` is for local examples only.
- Agents must run the full test suite (unit + integration) after code changes before opening PRs.
- Agents must include a short rationale in the PR description and list files modified.
- Changes that affect data model, DB schema, or APIs require a migration plan and a rollback plan in the PR.

Bootstrapping
-------------
- During initial repository bootstrap or onboarding, agents may skip running the full test suite to accelerate iteration. When skipping tests, always:
	- Note in the branch/PR title and description that tests were skipped and why.
	- Include exact commands used to build without tests (example: `mvn -DskipTests=true -DskipITs=true clean package`).
	- Ensure a follow-up PR or CI job runs the full test suite before merging to protected branches (e.g., `main`, `prod`).
- Skipping tests is allowed only for short-lived bootstrap branches; all production merges must pass full CI checks.

Prompt & Output Conventions
--------------------------
- Provide a concise goal statement, desired files to change, and exact commands to verify locally.
- For code edits, return a patch or a list of concrete file diffs; avoid only high-level descriptions.
- Include unit tests and/or integration tests for substantive changes.
- Use deterministic, reproducible commands. Example verification: `mvn -DskipITs=false test` or `docker-compose up --build -d && mvn -DskipTests=false verify`.

Repository Interaction Pattern
----------------------------
1. Agent generates a branch named: `agent/<short-desc>-YYYYMMDD`.
2. Agent runs formatting and linters locally: `mvn fmt:format` or configured formatter (see `SKILL.md`).
3. Agent runs tests: `mvn test` then integration tests. During bootstrap agents MAY skip tests (see Bootstrapping), but any branch that will be merged into protected branches MUST run full tests before merge.
4. Agent opens a draft PR with description, test results, and verification steps.
5. A human reviewer inspects and merges after approvals.

Checks and Tooling
------------------
- Formatting: Use the repository's formatter configured in `SKILL.md` (Google Java Format / Spotless via Maven).
- Static analysis: Run SpotBugs/PMD/Checkstyle in CI.
- Security scanning: Run dependency-check or Snyk in CI.
- DB changes: Use `src/main/resources/schema.sql` plus migration tooling (Flyway/Liquibase recommended).

When to Escalate to Humans
--------------------------
- Any change touching security, secrets, authentication, or payment flows.
- Schema migrations or backwards-incompatible API changes.
- Performance-sensitive code or operational runbooks.

See also: [SKILL.md](SKILL.md)
