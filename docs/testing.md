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

## Black-Box API Regression Test

Run the API smoke/regression test against a live Stella API without importing backend classes:

```bash
STELLA_API_BASE_URL=http://localhost:8080 \
STELLA_API_USERNAME=admin \
STELLA_API_PASSWORD=admin \
./scripts/api-blackbox-test.sh
```

The same script can point to the validation environment:

```bash
STELLA_API_BASE_URL=https://stella.gebaralabs.dev \
STELLA_API_USERNAME=admin \
STELLA_API_PASSWORD=admin \
./scripts/api-blackbox-test.sh
```

If a token is already available, use it instead of username/password:

```bash
STELLA_API_BASE_URL=http://localhost:8080 \
STELLA_API_TOKEN='ey...' \
./scripts/api-blackbox-test.sh
```

The script requires `curl` and `python3`. It validates health, authentication, item-master creation, lookup, listing, search by name, optional semantic search, and cleanup. Any item created by the test is deleted at the end through a shell trap whenever possible.

Configuration:

- `STELLA_API_BASE_URL`: target API base URL. Defaults to `http://localhost:8080`.
- `STELLA_API_PREFIX`: API prefix. Defaults to `/api/v0`.
- `STELLA_API_TOKEN`: bearer token for authenticated endpoints.
- `STELLA_API_USERNAME` and `STELLA_API_PASSWORD`: credentials used with `/api/public/login` when no token is provided.
- `STELLA_RUN_SEMANTIC_SEARCH`: set to `false` to skip semantic search. Defaults to `true`.
- `STELLA_RUN_REINDEX`: set to `true` to call semantic reindexing before semantic search. Defaults to `false`.

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
