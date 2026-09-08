# ProjectM

Build Minecraft plugins from visual blocks. Users assemble logic on a canvas, the backend
compiles it into a ready-to-drop JAR.

This repository is the backend: a Spring Boot multi-module reactor with an HTTP API and a
separate compile worker. The web client lives in its own repository.

---

## Architecture

Three modules under a parent POM:

| Module | Role |
|---|---|
| `projectm-common` | Message DTOs, `BuildStatus`, queue/exchange names — the contract both apps share |
| `projectm-api` | REST API: authentication, project CRUD, queueing builds, build status |
| `projectm-build-worker` | RabbitMQ consumer: compiles plugins, uploads artifacts, cleans up after deletions |

Storage is split by what each store is good at:

| Store | Owns |
|---|---|
| PostgreSQL | `users`, `verification_tokens`, `plugin_builds` — anything relational or transactional |
| MongoDB | Projects and their visual document (`projectData`), which is deeply nested and schemaless |
| Redis | `tokenVersion` cache, so JWT validation does not hit Postgres on every request |
| MinIO (S3) | Compiled JAR artifacts |
| RabbitMQ | Compile queue and cleanup queue, both with a dead-letter exchange |

### Compile flow

```
POST /compile
   └─ row in plugin_builds, status QUEUED
        └─ CompileTaskMessage published after the transaction commits
             └─ worker atomically claims the build (QUEUED → RUNNING)
                  └─ loads the project from Mongo, validates and compiles it
                       └─ uploads the JAR to MinIO
                            └─ status SUCCESS (or FAILED with the reason)
```

The API never compiles anything and the worker never serves HTTP. They share only the
message contract in `projectm-common` and the two databases.

---

## Tech stack

Spring Boot 4.1 · Java 21 · Spring Security 7 · Hibernate 7 · Jackson 3 · Liquibase ·
AWS SDK v2 · springdoc-openapi · Lombok.

Plugin compilation itself is delegated to `visual-core`, an external library that owns the
visual element model, its validation rules and the Java code generator.

---

## Prerequisites

- **JDK 21** — a JDK, not a JRE. The worker invokes the Java compiler at runtime, so
  `ToolProvider.getSystemJavaCompiler()` must return something.
- **Docker** with Compose, for the infrastructure.
- **A GitHub token with `read:packages`.** `visual-core` is published to GitHub Packages,
  which requires authentication **even though the package is public**. Without this the
  build cannot resolve its dependencies. This trips up every fresh clone.

Put the token in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_TOKEN_WITH_read:packages</password>
    </server>
  </servers>
</settings>
```

The `github` id must match the repository id in the root `pom.xml`.

---

## Getting started

**1. Start the infrastructure**

```bash
docker compose up -d
```

**2. Create the artifact bucket**

MinIO does not create buckets on its own, and the worker does not create one either. Without
this step a build compiles successfully and then fails on upload with `NoSuchBucketException`.

```bash
docker run --rm --network host --entrypoint sh minio/mc -c \
  "mc alias set pm http://localhost:9000 projectm projectm123 && \
   mc mb --ignore-existing pm/projectm-artifacts"
```

Or through the console at http://localhost:9001 (`projectm` / `projectm123`).

**3. Create a `.env` file in the repository root**

```properties
JWT_TOKEN_SECRET=<see below>
SPRING_PROFILES_ACTIVE=local
MAILTRAP_USERNAME=<your mailtrap inbox username>
MAILTRAP_PASSWORD=<your mailtrap inbox password>
```

The JWT secret is required and has no default — the application will not start without it.
It is Base64URL-decoded and used as an HMAC-SHA key, so it needs at least 32 decoded bytes:

```bash
openssl rand -base64 32 | tr '+/' '-_' | tr -d '='
```

Registration and password reset send real email through SMTP. The `local` profile points at
[Mailtrap](https://mailtrap.io), which captures messages instead of delivering them.

**4. Install the modules into the local repository**

```bash
./mvnw install -DskipTests
```

This has to happen before running anything: `projectm-api` and `projectm-build-worker` both
depend on `projectm-common`, and Maven resolves it from the local repository.

**5. Run the two applications**

```bash
./mvnw -pl projectm-api spring-boot:run
./mvnw -pl projectm-build-worker spring-boot:run
```

Note the absence of `-am`. Adding it makes Maven run the goal against `projectm-parent`,
which is a POM-packaged aggregator with no application class, and the run fails with
`NoClassDefFoundError: SpringApplication`. Running both from an IDE is usually easier.

The API listens on `:8080`. Interactive API docs are at
http://localhost:8080/swagger-ui.html.

---

## Configuration

Every setting has a working local default except the JWT secret. Override through
environment variables or `.env`.

| Variable | Default | Purpose |
|---|---|---|
| `JWT_TOKEN_SECRET` | *(none — required)* | HMAC key for signing tokens |
| `SPRING_PROFILES_ACTIVE` | `local` | `local` uses Mailtrap, `prod` uses Resend |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `localhost:5432` / `postgres` / `1` | PostgreSQL |
| `MONGODB_URI` | `mongodb://projectm:projectm@localhost:27017/projectm?authSource=admin` | MongoDB |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `localhost` / `5672` / `projectm` / `projectm` | RabbitMQ |
| `S3_ENDPOINT` / `S3_BUCKET` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` | `http://localhost:9000` / `projectm-artifacts` / `projectm` / `projectm123` | Artifact storage (worker) |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed browser origins |
| `MAILTRAP_USERNAME` / `MAILTRAP_PASSWORD` | *(none)* | SMTP credentials on the `local` profile |
| `RESEND_API_KEY` | *(none)* | SMTP credentials on the `prod` profile |

The credentials in `docker-compose.yaml` are local development defaults and are meant to be
overridden everywhere else.

### Local infrastructure

| Service | Port | Credentials |
|---|---|---|
| PostgreSQL | 5432 | `postgres` / `1`, database `postgres` |
| MongoDB | 27017 | `projectm` / `projectm` |
| Redis | 6379 | — |
| RabbitMQ | 5672, UI on 15672 | `projectm` / `projectm` |
| MinIO | 9000, console on 9001 | `projectm` / `projectm123` |

The relational schema is owned by Liquibase (`projectm-api/src/main/resources/db/changelog`)
and Hibernate runs with `ddl-auto: validate`, so the schema is never modified by the ORM.

---

## API

All routes are under `/api/v1`. Everything except the auth endpoints requires a
`Authorization: Bearer <token>` header. Every response — success or failure — is wrapped in
`{ "message": "...", "data": ... }`.

**Authentication**

```
POST   /auth/register              POST /auth/login
GET    /auth/verify?token=         POST /auth/resend-verification
POST   /auth/forgot-password       POST /auth/reset-password
POST   /auth/logout-all
```

**Projects**

```
GET    /projects                   list (paged; returns id and name only)
POST   /projects                   create
GET    /projects/{id}              full project including its visual document
PATCH  /projects/{id}/metadata     rename
PATCH  /projects/{id}/data         apply a diff to the visual document
PUT    /projects/{id}/data         replace the visual document
DELETE /projects/{id}
```

**Builds**

```
GET    /projects/{id}/builds                    paged build history
POST   /projects/{id}/compile                   queue a compile
GET    /projects/{id}/builds/{buildId}          one build
GET    /projects/{id}/builds/{buildId}/artifact the artifact's storage key
```

---

## Design notes

The parts that are less obvious from reading the code top to bottom.

**Publishing happens after the transaction commits.** The compile message is sent from a
`TransactionSynchronization` callback rather than inside the service method. Publishing
inline can put a message on the queue for a row that then rolls back, and the worker picks
up a build that does not exist. This is not at-least-once delivery — a crash between commit
and publish loses the message — but the failure mode it leaves behind ("row with no
message") is detectable and repairable, whereas "message with no row" is not. Full
guarantees would need an outbox.

**Builds are claimed atomically.** The worker moves a build `QUEUED → RUNNING` with a
conditional bulk update and checks the affected row count, so a redelivered message cannot
start a second compile. Because a bulk JPQL update bypasses the auditing listener, the
modification timestamp is set explicitly in the query — otherwise stale builds would be
invisible to the reaper.

**One active build per project, enforced twice.** There is a check in application code, for
a comprehensible error message, and a partial unique index over `QUEUED`/`RUNNING` rows,
because check-then-act is not atomic and two concurrent requests would otherwise both pass.

**Failures are classified before they are handled.** Validation and malformed-source errors
are the user's problem: the build is marked `FAILED` and the message is acknowledged, since
retrying cannot help. Infrastructure errors release the claim back to `QUEUED` and rethrow,
so the retry and dead-letter machinery still works. Without that release, a retry would find
the build already `RUNNING`, quietly return, and acknowledge a message that never did
anything.

**Stuck builds are reaped on a schedule.** A build can be left `QUEUED` after its message
has exhausted its retries and gone to the dead-letter queue. Since the partial unique index
counts `QUEUED` rows, such a row would block that project forever, so a scheduled job fails
rows that have sat too long.

**Sessions are revoked through a version counter.** Each user row carries a `tokenVersion`
embedded in every issued token and cached in Redis. Logging out everywhere, and resetting a
password, increment it — invalidating every outstanding token without any server-side
session store. The increment is computed by the database and the cache is invalidated rather
than overwritten, since overwriting would require a read-back and reintroduce the race.

---

## Known limitations

Honest state of things rather than a roadmap.

- **The artifact endpoint returns a storage key, not the file.** There is no presigned-URL
  endpoint and the API does not stream bytes, so a browser cannot download a JAR without
  reaching MinIO directly.
- **Newly created projects hold a placeholder document** that the compiler rejects. A client
  has to write a valid empty plugin before the project can build.
- **Access tokens only.** Tokens are long-lived and there is no refresh endpoint or
  per-device session record, so the only revocation available is all-or-nothing.
- **Login distinguishes an unverified account from a wrong password** — 403 versus 401 —
  which lets an anonymous caller check whether an address is registered.
- **Keep worker concurrency at one.** The compiler caches runtime jars lazily and its
  thread-safety has not been established.
- **Mongo indexes are created from annotations at startup**, unlike the Postgres schema,
  which Liquibase owns. They have no migration history, a changed index definition would
  fail on boot against the existing index, and the two modules that map the `projects`
  collection do not declare the same set. Moving them to an explicit migration step, and
  checking rather than creating them at startup, is pending.
- **Test coverage is thin**, and the existing tests lean on mocks in exactly the places
  where the interesting bugs live: schema constraints, transaction boundaries, and broker
  retry behaviour.
- **Builds are not byte-reproducible.** The Spigot API dependency is a snapshot.

---

## Repository layout

```
projectm-common/         shared DTOs, BuildStatus, queue names
projectm-api/            REST API
  src/main/resources/db/changelog/   Liquibase migrations
projectm-build-worker/   compile worker
  libs/                  runtime jar bundled into compiled plugins
docker-compose.yaml      local infrastructure
```
