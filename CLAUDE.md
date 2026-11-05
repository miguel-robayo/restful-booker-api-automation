# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

REST API test automation framework for the public Restful-Booker API (technical test). Java 21, Maven, Cucumber 7 (Gherkin in Spanish) on JUnit Platform, own lightweight Screenplay, API Object pattern, Rest Assured 5, Allure 2.

Approved design lives in `docs/` and is the source of truth — read it before changing behavior:
- `docs/analysis/requirements-analysis.md` — requirements (RF/RC/RNF), findings, traceability.
- `docs/strategy/test-strategy.md` — 24 scenarios (TC-xx, FW-xx), REQ/MON levels, Allure categories, decisions D-01..D-22.
- `docs/architecture/framework-architecture.md` — layers, Given/When/Then rule, ADR-01..19.
- `docs/known-defects.md` — API divergences KD-xx and how they are tested.

## Commands

JDK 21 is mandatory (Maven Enforcer fails otherwise). Use the wrapper.

```bash
./mvnw clean verify                         # main job: framework tests + scenarios "not @known-defect"
./mvnw clean test -Denv=dev                 # select environment (dev|qa, default qa)
./mvnw test -Pknown-defects                 # separate job: only @known-defect scenarios, own result dirs
./mvnw test -Dtest=ConfigResolverTest       # single JUnit test class
./mvnw test -Dtest=ApiScenariosSuite -Dcucumber.filter.tags="@TC-NEG-01"   # single Cucumber scenario (runner arrives in phase 1)
./mvnw allure:serve                         # open report for target/allure-results
./mvnw allure:serve -Pknown-defects         # open report for the known-defects job
```

Credentials come only from environment variables `RB_USERNAME` / `RB_PASSWORD` (or `-Dauth.username` / `-Dauth.password`). Never commit them.

## Architecture rules (do not break)

- Features → Step Definitions → Actor → Abilities → Tasks (writes) / Questions (reads) → API Objects → Rest Assured.
- `Dado` = `actor.wasAbleTo(...)` (precondition, 2xx required); `Cuando` = `attemptsTo` (write) or `asksFor` + `remember` (read); `Entonces` = `Expect` / `ResponseSpecs` on stored results — **no HTTP in Then steps**.
- Step definitions are thin adapters: no Rest Assured, no API Objects, no business rules.
- Only API Objects build HTTP requests; no generic HTTP "Interactions" layer.
- Never write to static `RestAssured.*`; build a new spec per request. No mutable static state; one `ScenarioContext` per scenario (PicoContainer).
- Cucumber hooks are the only lifecycle mechanism for scenarios. JUnit Jupiter is only for technical framework tests (FW-xx).
- REQ/MON is decided by the requirement, never by what the API currently does. `@known-defect` = REQ failing on an open KD (separate job); `@kd-monitor` = MON (main job).
- Assertion message prefixes drive Allure categories: `KD-xx:`, `SLA:`, `PRECONDITION:`, `INFRA:`.
- Single Allure adapter: `allure-cucumber7-jvm`. Do not add `allure-junit5`.

## Working agreement

- Communicate in Spanish. Workflow: ANALYZE → PLAN → IMPLEMENT → VERIFY → REVIEW → DOCUMENT; present plan, affected files, risks and acceptance criteria before implementing.
- Do not invent business rules; expected results come from the test document, then the API docs; observed behavior only identifies divergences.
- Do not modify approved documents without explicit authorization.
- No commit, push, merge or rebase without showing the diff and getting explicit authorization.
- Do not add dependencies without justification; pinned versions must be at least two weeks old.
- Never claim a test passed without real execution output.
