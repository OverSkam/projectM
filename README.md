# ProjectM 
[![CI](https://github.com/OverSkam/projectM/actions/workflows/ci.yml/badge.svg)](https://github.com/OverSkam/projectM/actions/workflows/ci.yml)

Build Minecraft plugins from visual blocks. Users assemble logic on a canvas, the backend
compiles it into a ready-to-drop JAR.

This repository is the backend: a Spring Boot multi-module reactor with an HTTP API and a
separate compile worker. The web client lives in its own repository.

**Live at [www.mcdaedalus.com](https://www.mcdaedalus.com)**, API at `https://api.mcdaedalus.com`.
Registration sends a real verification email, so sign up with an address you can read; from an
empty account to a downloaded JAR takes about a minute.

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
| Object storage (S3 API) | Compiled JAR artifacts — MinIO in development, Cloudflare R2 in production |
| RabbitMQ | Compile queue and cleanup queue, both with a dead-letter exchange |

### Compile flow

```
POST /compile
   └─ row in plugin_builds, status QUEUED
        └─ CompileTaskMessage published after the transaction commits
             └─ worker atomically claims the build (QUEUED → RUNNING)
                  └─ loads the project from Mongo, validates and compiles it
                       └─ uploads the JAR to object storage
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
- **Docker** with Compose, for the infrastructure and the applications.
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

**1. Create a `.env` file in the repository root**

```properties
JWT_TOKEN_SECRET=<see below>
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

Both emails link to the **web client**, not to the API, because completing a password reset
requires a form. The client reads the `token` query parameter and calls the API itself:
`/verify` calls `GET /api/v1/auth/verify`, and `/reset-password` posts the token together
with the new password. Point `APP_VERIFICATION_URL` and `APP_PASSWORD_RESET_URL` at wherever
the client is served.

**2. Build the application jars**

```bash
./mvnw package -DskipTests
```

The images copy these jars in rather than building inside Docker, which keeps the GitHub
Packages token out of the image build.

**3. Start everything**

```bash
docker compose up --build
```

This starts PostgreSQL, MongoDB, Redis, RabbitMQ and MinIO, creates the artifact bucket, then
the API, and finally the worker once the API reports healthy — the API owns the Liquibase
migrations and the worker only validates the schema.

The API listens on `:8080`. Interactive API docs are at
http://localhost:8080/swagger-ui.html, and the health endpoint is at
http://localhost:8080/actuator/health.

Stop with `docker compose down`, or `docker compose down -v` to also wipe the data.

### Running from the IDE instead

Start only the infrastructure, then run the two applications yourself:

```bash
docker compose up -d postgres mongo redis rabbitmq minio createbuckets
./mvnw install -DskipTests
./mvnw -pl projectm-api spring-boot:run
./mvnw -pl projectm-build-worker spring-boot:run
```

`projectm-api` and `projectm-build-worker` both depend on `projectm-common`, which Maven
resolves from the local repository, so `install` has to happen first. Note the absence of
`-am`: adding it makes Maven run the goal against `projectm-parent`, a POM-packaged aggregator
with no application class, and the run fails with `NoClassDefFoundError: SpringApplication`.

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
| `S3_PUBLIC_ENDPOINT` / `S3_REGION` / `S3_LINK_TTL_MINUTES` | `http://localhost:9000` / `us-east-1` / `10` | Artifact storage (API) — the public endpoint must be the address the *browser* resolves, since it ends up inside the signed download link |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed browser origins |
| `APP_VERIFICATION_URL` | `http://localhost:5173/verify` | Frontend page the verification email links to |
| `APP_PASSWORD_RESET_URL` | `http://localhost:5173/reset-password` | Frontend page the password reset email links to |
| `APP_MAIL_FROM` | `no-reply@projectm.local` | Sender address on outgoing mail |
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

## Deployment

The live instance runs this same stack on one Ubuntu VM in Google Cloud, through
`docker-compose.prod.yaml`.

- **The jars are built on the machine** and the images only copy them in, which keeps the
  GitHub Packages token out of every image layer.
- **The production compose file differs in three ways**: no object-storage container, because
  artifacts go to Cloudflare R2; no published database ports; and the API bound to
  `127.0.0.1:8080` rather than to every interface.
- **No inbound port is open at all.** A Cloudflare Tunnel runs as a systemd service and dials
  outward, so `https://api.mcdaedalus.com` terminates TLS at Cloudflare and reaches the API over
  the loopback interface. The VM's public IP answers nothing.
- **R2 speaks the S3 API**, so the same presigner code serves download links in both
  environments — only the endpoint and the region differ.
- **Mail goes through Resend** on the `prod` profile, DKIM-signed for the domain.
- **The web client is a static build on Vercel**, which is why the API has to allow its origin
  through `APP_CORS_ALLOWED_ORIGINS`, and why the email links point at the client rather than at
  the API.

Swagger and the OpenAPI document are disabled in production, so the route list below is the
reference.

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
GET    /projects/{id}/builds/{buildId}/artifact temporary signed link to download the JAR
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

**The JAR is handed over as a signed link, not as bytes.** The artifact endpoint answers with
a presigned URL, valid for ten minutes, and the browser fetches the object straight from
storage. Streaming it through the API would occupy a request thread and heap for the length of
the download, and the signature keeps the bucket private without the API proxying traffic it
contributes nothing to. The link has to be opened by the browser itself — a client that sends
it through its own HTTP layer, `Authorization` header and all, gets rejected by the signature.

**Sessions are revoked through a version counter.** Each user row carries a `tokenVersion`
embedded in every issued token and cached in Redis. Logging out everywhere, and resetting a
password, increment it — invalidating every outstanding token without any server-side
session store. The increment is computed by the database and the cache is invalidated rather
than overwritten, since overwriting would require a read-back and reintroduce the race.

---

## Known limitations

Honest state of things rather than a roadmap.

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
- **Some paths are still untested.** Nothing runs the real `visual-core` compiler or uploads to
  MinIO, so a compile is covered only up to the moment compilation starts. Three of the
  listener's failure branches — malformed project data, pre-compile validation reports, and
  the compile-exception classification — need a valid plugin document to reach, and are not
  covered. Worker tests build their schema from the entities rather than from the Liquibase
  migrations; the API tests cover the migrations instead.
- **Builds are not byte-reproducible.** The Spigot API dependency is a snapshot.
- **The AWS SDK is pinned to 2.29.52.** From 2.30 the S3 client sends integrity headers that
  Cloudflare R2 rejects unless checksum calculation is configured down to `WHEN_REQUIRED`.

---

## Repository layout

```
projectm-common/         shared DTOs, BuildStatus, queue names
projectm-api/            REST API
  src/main/resources/db/changelog/   Liquibase migrations
projectm-build-worker/   compile worker
  libs/                  runtime jar bundled into compiled plugins
docker-compose.yaml      local infrastructure and both applications
docker-compose.prod.yaml production stack: no object-storage container, no exposed databases
```
