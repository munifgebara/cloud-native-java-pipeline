# Testing and Quality

## Main Verification Command

```bash
./mvnw clean verify
```

This command is the default backend verification gate. It runs the frontend build integrated into Maven, compiles the backend, executes tests, generates the JaCoCo report, checks configured coverage thresholds, and packages the application.

## Focused Test Commands

Run a single test class:

```bash
./mvnw -Dtest=PessoaServiceTest test
```

Run the Cucumber BDD suite:

```bash
./mvnw -Dtest=CucumberBddTest test
```

Generate JaCoCo reports:

```bash
./mvnw jacoco:report
```

Check JaCoCo thresholds:

```bash
./mvnw jacoco:check
```

## Frontend Build

When frontend code changes, run:

```bash
cd frontend
npm install
npm run build
```

The Maven build also runs the frontend build as part of the integrated package flow.

## Coverage Goal

The project currently enforces coverage through JaCoCo. The near-term target is to keep raising coverage toward 80%, with a later backlog item to move toward 90%. Production-critical backend flows should receive integration or service-level tests before UI polish work.

## Pull Request Expectations

Every implementation PR should include:

- concise summary of behavior changed
- test commands executed
- coverage result when relevant
- linked issue using `Closes #N`

Do not include secrets, tokens, personal production data, or unrelated refactors in a PR.
