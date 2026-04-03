# CI/CD Pipeline — File-by-File Description

## Overview

The pipeline automates testing and deployment: every push to `master` runs tests, builds a Docker image, transfers it to an AWS EC2 instance, and starts the application. Four files make this work: a Dockerfile, a production Docker Compose, a GitHub Actions workflow, and a production Spring Boot profile.

```
Developer pushes to master
        │
        ▼
┌─────────────────────┐
│  GitHub Actions:     │
│  1. Run mvn verify   │  ← Test job (runs on every push & PR)
│     (92 tests)       │
└────────┬────────────┘
         │ Tests pass
         ▼
┌─────────────────────┐
│  GitHub Actions:     │
│  2. Generate SSL     │  ← Deploy job (only on push to master)
│  3. Build jar        │
│  4. Build Docker     │
│     image            │
│  5. SCP to EC2       │
│  6. SSH: docker      │
│     compose up       │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  AWS EC2 (t3.micro)  │
│  ┌───────────────┐   │
│  │ PostgreSQL 16  │   │
│  │ (Docker)       │   │
│  └───────┬───────┘   │
│          │ port 5432  │
│          │ (internal) │
│  ┌───────┴───────┐   │
│  │ Spring Boot    │   │
│  │ App (Docker)   │──────► https://<Elastic-IP>:443
│  └───────────────┘   │
└─────────────────────┘
```

---

## File 1: `Dockerfile`

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Purpose

Packages the Spring Boot application into a Docker image using a **multi-stage build**. The result is a slim image (~200 MB) containing only the JRE and the application JAR — no build tools, no source code.

### Line-by-line explanation

**Stage 1 — Build (maven:3.9-eclipse-temurin-17)**

- `FROM maven:3.9-eclipse-temurin-17 AS build` — Base image with Maven 3.9 and JDK 17. The `AS build` names this stage so we can reference it later. This image is ~800 MB but is only used during build, not in the final image.
- `WORKDIR /app` — Sets the working directory inside the container. All subsequent commands run from `/app`.
- `COPY pom.xml .` — Copies only `pom.xml` first (not the source code). This is a **Docker layer caching** optimization — dependencies change rarely, so this layer is cached and reused across builds.
- `RUN mvn dependency:go-offline -B` — Downloads all Maven dependencies. The `-B` flag enables batch mode (non-interactive, cleaner logs). Because this step only depends on `pom.xml`, Docker caches it. If dependencies haven't changed, this step is skipped on subsequent builds — saving 2-3 minutes.
- `COPY src ./src` — Now copies the actual source code. This layer changes on every code change, so it and all subsequent layers are rebuilt.
- `RUN mvn package -DskipTests -B` — Compiles the code and creates the Spring Boot fat JAR in `target/`. Tests are skipped (`-DskipTests`) because they already ran in the CI test job — no need to run them twice.

**Stage 2 — Run (eclipse-temurin:17-jre)**

- `FROM eclipse-temurin:17-jre` — Starts a **fresh** image with only the Java Runtime Environment (JRE), not the full JDK. This is ~200 MB vs ~800 MB — no compiler, no Maven, no source code. Smaller image = faster deployment and smaller attack surface.
- `WORKDIR /app` — Working directory in the runtime container.
- `COPY --from=build /app/target/*.jar app.jar` — Copies **only the built JAR** from Stage 1. Everything else (Maven cache, source code, build tools) is discarded. This is the key benefit of multi-stage builds — the final image has no trace of the build environment.
- `EXPOSE 8443` — Documents that the app listens on port 8443 (HTTPS). This is metadata only — the actual port mapping happens in `docker-compose.prod.yml`.
- `ENTRYPOINT ["java", "-jar", "app.jar"]` — Starts the Spring Boot application. Uses exec form (JSON array) instead of shell form so the Java process receives OS signals (SIGTERM for graceful shutdown) directly, without a shell wrapper process.

### Why multi-stage?

| Aspect | Single-stage | Multi-stage |
|---|---|---|
| Final image size | ~800 MB (JDK + Maven + deps + source) | ~200 MB (JRE + JAR) |
| Security | Build tools and source code in production | Only runtime — no compiler, no source |
| Build caching | Dependencies and code in same layer | Dependencies cached separately |
| Attack surface | Larger — more tools for attackers | Minimal — only what's needed to run |

---

## File 2: `docker-compose.prod.yml`

```yaml
services:
  postgres:
    image: postgres:16
    container_name: travel-agency-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      TZ: UTC
      PGTZ: UTC
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}" ]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    image: ${DOCKER_IMAGE:-travel-agency:latest}
    container_name: travel-agency-app
    restart: unless-stopped
    ports:
      - "443:8443"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      SSL_KEY_STORE_PASSWORD: ${SSL_KEY_STORE_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      OAUTH2_REDIRECT_URI: ${OAUTH2_REDIRECT_URI:-https://localhost:8443/login/oauth2/code/google}

volumes:
  postgres_data:
```

### Purpose

Defines the production infrastructure: two containers (PostgreSQL + Spring Boot app) running together on the EC2 instance. This is the orchestration file — it declares how services connect, restart, and exchange configuration.

### Section-by-section explanation

**postgres service**

- `image: postgres:16` — Official PostgreSQL 16 image from Docker Hub. Same version used in local development — ensures SQL compatibility between environments.
- `container_name: travel-agency-db` — Fixed name so we can reference it in logs and commands (`docker logs travel-agency-db`). Without this, Docker generates a random name.
- `restart: unless-stopped` — Automatically restarts the container if it crashes or the EC2 instance reboots. Only stops if explicitly stopped with `docker compose down`. This ensures the database survives unexpected failures.
- `environment` — PostgreSQL initialization variables:
    - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` — On first startup, PostgreSQL creates this database and user automatically. On subsequent startups, these are ignored (data already exists in the volume). Values come from environment variables set by the GitHub Actions deploy script from secrets.
    - `TZ: UTC`, `PGTZ: UTC` — Forces UTC timezone. Prevents the `FATAL: invalid value for parameter "TimeZone": "Europe/Kiev"` error that PostgreSQL 16 throws on Ukrainian systems. PostgreSQL 16 renamed the timezone to "Europe/Kyiv" but Java's JVM still sends "Europe/Kiev". Setting UTC avoids the conflict entirely.
- `volumes: postgres_data:/var/lib/postgresql/data` — **Named volume** for data persistence. This is the most critical line in the file. Without it, all data is lost when the container is recreated. The volume survives `docker compose down` — only `docker compose down -v` (the `-v` flag) deletes it. On redeploy, only the `app` container is recreated — `postgres` keeps running with all existing data untouched.
- `healthcheck` — Runs `pg_isready` (a PostgreSQL utility) every 10 seconds to verify the database is accepting connections. After 5 consecutive failures (50 seconds), the container is marked unhealthy. The `app` service uses this health status to know when it's safe to start.

**app service**

- `image: ${DOCKER_IMAGE:-travel-agency:latest}` — Uses the Docker image built and transferred by the CI pipeline. The `:-` syntax provides a default value (`travel-agency:latest`) if the environment variable isn't set.
- `restart: unless-stopped` — Same auto-restart behavior as PostgreSQL. If the Spring Boot app crashes (OutOfMemoryError, unhandled exception), Docker restarts it automatically.
- `ports: "443:8443"` — Maps host port 443 (standard HTTPS) to container port 8443 (Spring Boot's configured SSL port). Users access `https://<IP>` without specifying a port — browsers use 443 by default for HTTPS.
- `depends_on: postgres: condition: service_healthy` — The app container only starts **after** PostgreSQL passes its health check. Without this, Spring Boot would try to connect to the database during startup, fail because PostgreSQL takes 5-10 seconds to initialize, and crash. The `condition: service_healthy` (not just `depends_on: postgres`) waits for the actual health check, not just container creation.
- `environment` — Spring Boot configuration via environment variables (Spring Boot automatically converts `SPRING_PROFILES_ACTIVE` to `spring.profiles.active`):
    - `SPRING_PROFILES_ACTIVE: prod` — Activates `application-prod.yml` which configures PostgreSQL datasource, OAuth2, and production SQL initialization.
    - `DB_HOST: postgres` — This is the key to Docker networking. Docker Compose creates a bridge network and a DNS entry for each service. The app connects to `jdbc:postgresql://postgres:5432/travel_agency` — Docker's internal DNS resolves `postgres` to the database container's IP address. No hardcoded IPs needed.
    - `DB_PORT: 5432` — The **internal** PostgreSQL port, not a mapped host port. Container-to-container communication uses internal ports directly — the database doesn't expose any port to the host.
    - `SSL_KEY_STORE_PASSWORD` — Password for the PKCS12 keystore generated during CI. Spring Boot reads it via `${SSL_KEY_STORE_PASSWORD:changeit}` in `application.yml`.
    - `JWT_SECRET` — 64-character hex string for signing JWT tokens. Used by the REST API authentication layer.
    - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` — Google OAuth2 credentials for "Login with Google" functionality.
    - `OAUTH2_REDIRECT_URI` — The full redirect URI that Spring Boot sends to Google during OAuth2 login. Defaults to `https://localhost:8443/login/oauth2/code/google` for local development. On production, this is overridden to `https://52-28-160-199.nip.io/login/oauth2/code/google` via the GitHub Secret. This is necessary because the app runs on port 8443 internally but is exposed on port 443 externally — without this override, Spring Boot would generate a redirect URI with port 8443 which Google would reject as a mismatch.

**volumes section**

- `postgres_data:` — Declares the named volume. Docker manages its location on the host filesystem (`/var/lib/docker/volumes/travel-agency_postgres_data/_data`). This volume persists across container restarts, image updates, and `docker compose down`. It contains the entire PostgreSQL data directory — tables, indexes, WAL logs, configuration.

### Why not expose PostgreSQL port?

The database has no `ports` mapping — it's only accessible from the `app` container via Docker's internal bridge network. It is **not exposed to the internet**. This is a security best practice: even if someone knows the database password, they can't connect because port 5432 is not reachable from outside the Docker network.

### What happens during redeployment?

```
1. New Docker image is loaded (docker load)
2. docker compose up -d detects the app image changed
3. Only the app container is stopped and recreated with the new image
4. The postgres container is NOT touched — it keeps running
5. The postgres_data volume is NOT touched — all data is preserved
6. The app container starts, waits for postgres health check, connects
7. Spring Boot runs schema.sql (CREATE TABLE IF NOT EXISTS — idempotent)
8. Spring Boot runs data.sql (INSERT ON CONFLICT DO NOTHING — idempotent)
9. Application is ready to serve requests
```

Zero data loss. Downtime is ~15 seconds (time to stop old container + start new one + Spring Boot startup).

---

## File 3: `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

env:
  DOCKER_IMAGE: travel-agency

jobs:
  test:
    name: Build & Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run tests
        run: mvn verify -B

  deploy:
    name: Deploy to AWS
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master' && github.event_name == 'push'

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Generate SSL keystore
        run: |
          keytool -genkeypair -alias travelagency -keyalg RSA -keysize 2048 \
            -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
            -validity 365 -storepass ${{ secrets.SSL_KEY_STORE_PASSWORD }} \
            -dname "CN=travel-agency, OU=EPAM, O=EPAM, L=Kyiv, ST=Kyiv, C=UA" \
            -noprompt 2>/dev/null || true

      - name: Build jar
        run: mvn package -DskipTests -B

      - name: Build Docker image
        run: |
          docker build -t $DOCKER_IMAGE:latest .
          docker save $DOCKER_IMAGE:latest | gzip > image.tar.gz

      - name: Copy files to EC2
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          source: "image.tar.gz,docker-compose.prod.yml"
          target: "/home/${{ secrets.EC2_USERNAME }}/travel-agency"

      - name: Deploy on EC2
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd /home/${{ secrets.EC2_USERNAME }}/travel-agency
            docker load < image.tar.gz
            export DB_NAME=${{ secrets.DB_NAME }}
            export DB_USERNAME=${{ secrets.DB_USERNAME }}
            export DB_PASSWORD=${{ secrets.DB_PASSWORD }}
            export SSL_KEY_STORE_PASSWORD=${{ secrets.SSL_KEY_STORE_PASSWORD }}
            export JWT_SECRET=${{ secrets.JWT_SECRET }}
            export GOOGLE_CLIENT_ID=${{ secrets.GOOGLE_CLIENT_ID }}
            export GOOGLE_CLIENT_SECRET=${{ secrets.GOOGLE_CLIENT_SECRET }}
            export OAUTH2_REDIRECT_URI=${{ secrets.OAUTH2_REDIRECT_URI }}
            docker compose -f docker-compose.prod.yml up -d
            rm image.tar.gz
```

### Purpose

GitHub Actions workflow that runs tests on every push/PR and deploys to AWS EC2 on pushes to master. This is the entry point of the entire CI/CD pipeline — it connects code changes to production deployment.

### Section-by-section explanation

**Trigger (`on`)**

- `push: branches: [master]` — Runs the workflow when code is pushed to master (direct push or merged PR).
- `pull_request: branches: [master]` — Runs the workflow when a PR targeting master is opened or updated. Only the `test` job runs for PRs — the `deploy` job has an additional `if` condition that blocks it.

**Global environment (`env`)**

- `DOCKER_IMAGE: travel-agency` — Shared variable used across steps. The Docker image is tagged as `travel-agency:latest`. Defined once here to avoid repeating the name in multiple steps.

**Job 1: test**

- `runs-on: ubuntu-latest` — Runs on a GitHub-hosted Ubuntu runner. Free for public repos, 2000 minutes/month for private repos.
- **Step: Checkout code** — `actions/checkout@v4` clones the repository into the runner's workspace (`/home/runner/work/travel-agency/travel-agency`). Without this, the runner has an empty workspace.
- **Step: Set up JDK 17** — `actions/setup-java@v4` installs JDK 17 (Eclipse Temurin distribution, the open-source successor to AdoptOpenJDK). The `cache: 'maven'` option caches `~/.m2/repository` between workflow runs — subsequent builds skip downloading dependencies, saving ~30 seconds. The cache key is derived from `pom.xml` — if dependencies change, the cache is invalidated.
- **Step: Run tests** — `mvn verify -B` compiles the source code, runs all 92 tests (unit + controller + integration), and packages the application. The `verify` phase runs the full Maven lifecycle through the integration-test phase. `-B` enables batch mode (non-interactive, cleaner CI logs). If any test fails, Maven exits with a non-zero code, the step fails, and the entire job is marked as failed — blocking deployment.

**Job 2: deploy**

- `needs: test` — Dependency declaration: this job only runs after the `test` job succeeds. If tests fail, the deploy job is skipped entirely. This is the gate that prevents broken code from reaching production.
- `if: github.ref == 'refs/heads/master' && github.event_name == 'push'` — Double guard: only deploys on **direct pushes** to master, not on pull requests. When a PR is opened, only the test job runs — this prevents deploying unreviewed code. The `&&` ensures both conditions must be true.

**Deploy steps:**

1. **Checkout + Setup Java** — Same as the test job. Each GitHub Actions job runs on a **fresh runner** — there is no shared filesystem or state between jobs. Even though the test job already compiled the code, the deploy job starts from scratch.

2. **Generate SSL keystore** — `keytool` (included in every JDK installation) creates a self-signed PKCS12 certificate for HTTPS:
   - `-alias travelagency` — Name of the key entry in the keystore.
   - `-keyalg RSA -keysize 2048` — RSA algorithm with 2048-bit key (industry standard minimum).
   - `-storetype PKCS12` — Modern keystore format (replaces legacy JKS).
   - `-keystore src/main/resources/keystore.p12` — Placed in resources so it's included in the JAR when Maven packages it in the next step.
   - `-validity 365` — Certificate valid for 1 year.
   - `-storepass ${{ secrets.SSL_KEY_STORE_PASSWORD }}` — Keystore password from GitHub Secrets. GitHub automatically masks this value in logs (displayed as `***`).
   - `-dname "CN=travel-agency, OU=EPAM, O=EPAM, L=Kyiv, ST=Kyiv, C=UA"` — Certificate distinguished name (subject info).
   - `-noprompt 2>/dev/null || true` — Suppresses interactive prompts and prevents failure if a keystore already exists. The `|| true` ensures the step always succeeds (exit code 0).

3. **Build jar** — `mvn package -DskipTests -B` creates the Spring Boot fat JAR (~80 MB) containing: compiled classes, embedded Tomcat, all dependencies, static resources (Thymeleaf templates, CSS), configuration files, and the keystore.p12 generated in step 2. Tests are skipped (`-DskipTests`) because they already passed in the `test` job — running them again would waste ~40 seconds.

4. **Build Docker image** — Two commands:
   - `docker build -t $DOCKER_IMAGE:latest .` — Reads the Dockerfile, copies the JAR from `target/` into a slim JRE image, tags it as `travel-agency:latest`.
   - `docker save $DOCKER_IMAGE:latest | gzip > image.tar.gz` — Serializes the Docker image to a tar archive and compresses it with gzip (~100 MB). This is the transport format — we transfer the compressed image file to EC2 instead of using a Docker registry (Docker Hub, AWS ECR). Direct transfer is simpler for a single-server deployment.

5. **Copy files to EC2** — Uses `appleboy/scp-action@v0.1.7` (a community GitHub Action) to securely copy two files to the EC2 instance via SCP (SSH Copy Protocol):
   - `image.tar.gz` — The compressed Docker image (~100 MB).
   - `docker-compose.prod.yml` — The production orchestration file.
   - Files are placed in `/home/ec2-user/travel-agency/` on the EC2 instance.
   - Authentication uses the SSH private key stored in `secrets.EC2_SSH_KEY` — the same `.pem` file used for manual SSH access.

6. **Deploy on EC2** — Uses `appleboy/ssh-action@v1.0.3` to execute a shell script on the EC2 instance via SSH:
   - `docker load < image.tar.gz` — Imports the Docker image from the compressed archive into the EC2's local Docker daemon. After this, `docker images` shows `travel-agency:latest`.
   - `export DB_NAME=... DB_USERNAME=... etc.` — Sets environment variables from GitHub Secrets. These variables are consumed by `docker-compose.prod.yml`'s `${VARIABLE}` placeholders. They exist only for the duration of this SSH session.
   - `export OAUTH2_REDIRECT_URI=...` — Sets the Google OAuth2 callback URL to the production value (`https://52-28-160-199.nip.io/login/oauth2/code/google`). Without this, Spring Boot would use the default (`https://localhost:8443/...`) which doesn't match the Google Console configuration.
   - `docker compose -f docker-compose.prod.yml up -d` — Starts or updates the containers in detached mode (`-d`). Docker Compose compares the current state with the desired state: if the `app` image changed, it stops the old container and starts a new one. The `postgres` container is untouched (same image, same config). This is the actual deployment moment.
   - `rm image.tar.gz` — Cleans up the ~100 MB archive to save disk space. The EC2 instance (t3.micro) has only 8 GB of storage.

---

## File 4: `application-prod.yml`

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:travel_agency}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
            redirect-uri: ${OAUTH2_REDIRECT_URI:https://localhost:8443/login/oauth2/code/google}
```

### Purpose

Spring Boot production profile configuration. Activated when `SPRING_PROFILES_ACTIVE=prod` is set (by docker-compose.prod.yml). Overrides the base `application.yml` with PostgreSQL, production SQL data, and Google OAuth2 credentials.

### Section-by-section explanation

**Config import**

- `spring.config.import: optional:file:.env[.properties]` — Loads the `.env` file as a Spring properties source. The `optional:` prefix means the app doesn't fail if `.env` doesn't exist (it doesn't exist in Docker — we pass env vars directly). Used only for local development with prod profile.

**Datasource**

- `url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:travel_agency}` — JDBC connection URL with environment variable placeholders and defaults. Locally: `localhost:5433` (Docker Compose maps 5433→5432). On EC2: `postgres:5432` (Docker internal DNS, internal port). The `${VAR:default}` syntax provides fallback values for local development.
- `username` / `password` — From environment variables, no defaults (must be provided).
- `driver-class-name: org.postgresql.Driver` — Explicit PostgreSQL JDBC driver. Spring Boot can auto-detect this, but explicit is better for clarity.

**JPA**

- `ddl-auto: none` — Hibernate does NOT manage the schema. We use `schema.sql` for full control — it defines CHECK constraints, indexes, and complex constraints that Hibernate's auto-generation can't create.
- `dialect: org.hibernate.dialect.PostgreSQLDialect` — Tells Hibernate to generate PostgreSQL-specific SQL. Spring Boot can auto-detect this, but an explicit setting avoids ambiguity.
- `show-sql: false` — No SQL logging in production (performance + log cleanliness).

**SQL initialization**

- `mode: always` — Runs SQL scripts on every startup.
- `schema-locations: classpath:schema.sql` — Creates tables using `CREATE TABLE IF NOT EXISTS` — safe to run repeatedly (idempotent).
- `data-locations: classpath:data.sql` — Inserts sample data using `INSERT ... ON CONFLICT (id) DO NOTHING` (PostgreSQL upsert) — won't duplicate rows on restart.

**OAuth2**

- `client-id` / `client-secret` — Google OAuth2 credentials from environment variables. These are the credentials from the Google Cloud Console project.
- `scope: email, profile` — Requests user's email address and basic profile (name). Minimal permissions — no access to contacts, calendar, or other Google services.
- `redirect-uri: ${OAUTH2_REDIRECT_URI:https://localhost:8443/login/oauth2/code/google}` — The callback URL that Google redirects to after user authentication. **This is the key to making OAuth2 work in both local and production environments:**
    - **Locally**: Environment variable is not set → uses default `https://localhost:8443/login/oauth2/code/google` → matches Google Console's authorized redirect URI for local development.
    - **On EC2**: Environment variable is set to `https://52-28-160-199.nip.io/login/oauth2/code/google` (via GitHub Secret → docker-compose → Spring Boot) → matches Google Console's authorized redirect URI for production.
    - **Why nip.io?** Google OAuth2 requires redirect URIs with a domain name (not bare IP addresses). nip.io is a free wildcard DNS service: `52-28-160-199.nip.io` automatically resolves to `52.28.160.199`. No registration or configuration needed.
    - **Why is this override needed?** The app runs on port 8443 inside the Docker container, but is mapped to port 443 on the host. Without the explicit redirect URI, Spring Boot generates `https://52-28-160-199.nip.io:8443/login/oauth2/code/google` (using its internal port). Google rejects this because the authorized redirect URI doesn't include port 8443. The explicit URI uses the external port (443, omitted because it's the HTTPS default).

---

## Security: How Secrets Are Protected

| Layer | Protection |
|---|---|
| **GitHub Secrets** | Encrypted at rest, masked in logs (shown as `***`), only accessible by workflows |
| **SSH transfer** | SCP uses encrypted SSH channel — files encrypted in transit |
| **EC2 environment** | Credentials passed as environment variables — not written to disk files |
| **Docker networking** | PostgreSQL not exposed to internet — only accessible from app container |
| **HTTPS** | All browser traffic encrypted with TLS (self-signed certificate) |
| **OAuth2 redirect** | Explicit redirect URI prevents open redirect attacks |

---

## GitHub Secrets Reference

| Secret | Used by | Purpose |
|---|---|---|
| `EC2_HOST` | SCP + SSH actions | EC2 Elastic IP address (e.g., `52.28.160.199`) |
| `EC2_USERNAME` | SCP + SSH actions | SSH user (`ec2-user` for Amazon Linux) |
| `EC2_SSH_KEY` | SCP + SSH actions | Full contents of `.pem` private key file |
| `DB_NAME` | docker-compose → PostgreSQL | Database name (e.g., `travel_agency`) |
| `DB_USERNAME` | docker-compose → PostgreSQL + Spring Boot | Database user (e.g., `travel`) |
| `DB_PASSWORD` | docker-compose → PostgreSQL + Spring Boot | Database password |
| `SSL_KEY_STORE_PASSWORD` | keytool + Spring Boot | HTTPS keystore password |
| `JWT_SECRET` | Spring Boot | 64-char hex string for JWT token signing |
| `GOOGLE_CLIENT_ID` | Spring Boot OAuth2 | Google Cloud Console client ID |
| `GOOGLE_CLIENT_SECRET` | Spring Boot OAuth2 | Google Cloud Console client secret |
| `OAUTH2_REDIRECT_URI` | Spring Boot OAuth2 | Production OAuth2 callback URL (e.g., `https://52-28-160-199.nip.io/login/oauth2/code/google`) |

---

## Deployment Flow Summary

```
 1. Developer pushes code to master
 2. GitHub Actions: test job runs mvn verify (92 tests)
 3. Tests pass → deploy job starts
 4. GitHub Actions: generates self-signed SSL keystore (keytool)
 5. GitHub Actions: builds Spring Boot JAR with keystore (mvn package)
 6. GitHub Actions: builds Docker image with JAR (~200 MB)
 7. GitHub Actions: compresses image to tar.gz (~100 MB)
 8. GitHub Actions: SCPs image + compose file to EC2
 9. GitHub Actions: SSHs into EC2, loads Docker image
10. GitHub Actions: sets environment variables from secrets
11. GitHub Actions: runs docker compose up -d
12. EC2: Docker detects app image changed
13. EC2: Stops old app container
14. EC2: Starts new app container (postgres untouched)
15. EC2: App waits for postgres health check
16. EC2: Spring Boot starts, runs schema.sql + data.sql
17. EC2: Application ready at https://<Elastic-IP>
```

Total pipeline time: ~3-4 minutes (test: ~40s, build+deploy: ~3 min).
Downtime: ~15 seconds (stop old container + start new + Spring Boot startup).
Data loss: none (PostgreSQL volume persists across deployments).
