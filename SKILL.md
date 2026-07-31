SKILL
=====

Enterprise Coding Standards & AI-Agent Guidance
---------------------------------------------
This document provides repository-wide coding standards, formatting rules, and guidance for AI agents to work reliably in this Java/Spring Boot project.

Goals
-----
- Maintain consistent code style and formatting.
- Ensure high test coverage and reproducible builds.
- Make changes traceable, reviewable, and safe for production.
- Enable AI agents to contribute high-quality changes with minimal human rework.

Repository Overview
-------------------
- Build: Maven (`pom.xml`).
- App: Spring Boot (see `RupeeXApplication.java`).
- DB schema: `src/main/resources/schema.sql`.
- CI: `Jenkinsfile` and related pipeline files.

Formatting and Style
--------------------
- Java formatting: Use Google Java Format (via Spotless Maven plugin) or an enterprise-approved formatter. Configure in `pom.xml`.
- Imports: Organize and remove unused imports automatically via IDE or Spotless.
- Naming: `camelCase` for methods/variables, `PascalCase` for classes, `UPPER_SNAKE_CASE` for constants and env vars.
- Javadoc: Public classes and APIs must include Javadoc summaries and `@param`/`@return` tags for non-trivial methods.

Static Analysis & Quality Gates
-------------------------------
- Checkstyle: Enforce style rules.
- SpotBugs: Detect common bugs.
- PMD: Detect code smells.
- Enforce these in CI; PRs must pass these checks before merge.

Testing
-------
- Unit tests: Use JUnit 5. Keep tests isolated and fast.
- Integration tests: Use Testcontainers for DB-backed integration tests, or a dedicated Docker compose test profile.
- Coverage: Aim for meaningful coverage around business logic; CI should fail on large regressions.

Bootstrapping
-------------
- During initial bootstrap or fast onboarding, teams and agents may skip running the full test suite to iterate quickly. If tests are skipped:
	- Document the reason in the branch/PR and include the exact commands used to skip tests (example: `mvn -DskipTests=true -DskipITs=true clean package`).
	- Mark the branch as a bootstrap or work-in-progress branch (e.g., `agent/bootstrap-...`).
	- Ensure a follow-up change or CI job runs the full test suite before merging into protected branches (`main`, `prod`).
	- Do not skip tests for security-sensitive, payment, or schema-migration changes.

Dependency & Security Management
-------------------------------
- Keep dependencies up-to-date with Dependabot or an equivalent process.
- Run dependency vulnerability scans in CI (dependency-check, Snyk).
- Do not store secrets in repo. Use `.env` for local examples only; production secrets must use secret managers.

Branching, Commits & PRs
------------------------
- Branch names: `feature/*`, `bugfix/*`, `hotfix/*`, `agent/*` (for AI agents).
- Commit messages: Follow Conventional Commits (type(scope): summary). Example: `fix(payments): handle null payerId`.
- PR description: Purpose, summary of changes, verification steps (commands), test results, and any migration steps.

CI/CD Expectations
------------------
- Build: `mvn -DskipTests=false clean package`.
- Tests: `mvn test` then integration tests.
- Lint & Format: `mvn spotless:check` (or configured commands).
- Security: dependency and SCA scanning steps.

Database Migrations
-------------------
- Use Flyway or Liquibase for migrations (recommended). Keep `schema.sql` as reference and add migrations under `src/main/resources/db/migration`.
- Migration PRs must include a rollback plan and a migration test (integration test or staged rollout plan).

AI-Agent Specific Guidance
--------------------------
- Context Window: Agents should include only relevant files and tests in prompts. Large diffs should be broken into multiple, reviewable patches.
- Reproducibility: Agents must provide exact commands to reproduce changes and test results.
- Non-destructive edits: Prefer adding tests and small refactors over sweeping changes.
- Tests First: When changing behavior, agents should add failing tests first, then implement the fix.
- Explain assumptions: Agent PRs must list assumptions and potential side effects.

Prompt Templates (example)
--------------------------
Task prompt (agent -> codegen):
1. Goal: "Fix bug where payment status isn't persisted when DB connection times out."
2. Files to inspect: `src/main/java/com/rupeex/main/repository/PaymentsRepository.java`, `src/main/java/com/rupeex/main/model/Payments.java`.
3. Constraints: Keep existing public API, add unit test reproducing issue, and pass all CI checks.
4. Verify locally: `mvn -DskipTests=false test` and `mvn -DskipITs=false verify`.

Verification Commands
---------------------
- Unit tests: `mvn test`.
- Full build: `mvn -DskipTests=false clean package`.
- Integration with DB (docker-compose):
```
docker-compose up -d db
mvn -Dspring.profiles.active=dev verify
```

Skip tests (bootstrap example):
```
mvn -DskipTests=true -DskipITs=true clean package
```

Tooling Recommendations
----------------------
- Spotless (Maven) with Google Java Format.
- SpotBugs, PMD, Checkstyle plugins configured in `pom.xml`.
- Add a `mvn fmt:format` or `mvn spotless:apply` hook in developer workflow.

PR Checklist (agent-created PRs must include)
-------------------------------------------
- Summary and rationale.
- Files changed list.
- Verification steps and commands.
- Test results and new/updated tests.
- Security impact assessment (if applicable).
- Migration and rollback plan (if DB/API changes).

Appendix: Example Maven Commands
-------------------------------
- Run unit tests: `mvn test`.
- Run full build: `mvn -DskipTests=false clean package`.
- Run Spotless check: `mvn spotless:check`.

Files
-----
See `AGENTS.md` for agent workflow and human-in-the-loop requirements.
