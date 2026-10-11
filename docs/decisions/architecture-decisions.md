# CodeSentinel AI — Architecture Decision Records

## 1. Overview

This document records the major architectural decisions made during the development of CodeSentinel AI.

Each decision captures the context, the selected approach, its rationale, and the trade-offs involved. These records help maintain consistency as the project evolves.

## 2. Decision Status

The following statuses are used:

- **Accepted:** The architectural approach has been selected.
- **In progress:** Implementation or integration is still underway.
- **Proposed:** The approach is being considered but has not been finalized.

---

## ADR-001: Separate the Node.js API and Java Review Service

**Status:** Accepted

### Context

CodeSentinel AI needs to handle GitHub webhooks, retrieve Pull Request information, coordinate review processing, and perform AI-powered code analysis.

These responsibilities can be separated into application components with clear boundaries.

### Decision

Use Node.js with TypeScript and Fastify for the API and GitHub integration. Use Java 21 and Spring Boot for review orchestration and persistence.

### Rationale

- Keep GitHub webhook handling and GitHub API operations in the Node.js layer.
- Keep review orchestration and persistence in the Java service.
- Establish clear interfaces between the API and review service.
- Allow the components to evolve independently.

### Trade-offs

- Introduces communication between two application runtimes.
- Requires separate configuration, testing, and operational management.
- Requires consistent request and response contracts between services.

---

## ADR-002: Use a GitHub App for Repository Integration

**Status:** Accepted

### Context

CodeSentinel AI needs to receive Pull Request events, retrieve changed files, and publish inline review comments.

### Decision

Use a GitHub App to integrate with GitHub repositories.

### Rationale

- Supports event-driven Pull Request processing.
- Provides installation-based authentication.
- Enables repository-scoped permissions.
- Allows review comments to be published through GitHub APIs.

### Security considerations

- Validate webhook signatures before processing events.
- Keep application credentials and webhook secrets outside source control.
- Request only the permissions required by the application.
- Handle authentication failures and API limits explicitly.

---

## ADR-003: Use an Analyzer Abstraction for AI Review

**Status:** Accepted

### Context

The Java review service needs to invoke AI-powered code analysis without tightly coupling review orchestration to a particular provider implementation.

### Decision

Use a `ReviewAnalyzer` abstraction with an `AIReviewAnalyzer` implementation and an `AIProvider` abstraction for provider integration.

The current provider implementation is `OpenAIProvider`.

### Rationale

- Separates review orchestration from AI-provider communication.
- Makes analyzer behavior easier to test in isolation.
- Creates a boundary for future provider or analyzer implementations.
- Centralizes AI integration and provider-specific error handling.

### Trade-offs

- Adds abstraction and interface maintenance.
- Requires the analyzer and provider contracts to evolve alongside review requirements.

---

## ADR-004: Use PostgreSQL for Review Persistence

**Status:** Accepted

### Context

CodeSentinel AI needs to persist review records and track processing states across requests.

### Decision

Use PostgreSQL as the relational database for the Java review service.

The review identity is based on:

`repository + pull_request_number + commit_sha`

### Rationale

- Provides durable relational persistence.
- Supports unique constraints for review identity.
- Allows review processing state to be queried.
- Helps avoid repeating completed AI reviews for the same commit.

### Trade-offs

- Requires database configuration, schema management, and backup planning.
- Concurrent requests require additional application-level handling beyond a unique constraint.
- Recovery procedures are needed for interrupted reviews.

---

## ADR-005: Track the Review Lifecycle Explicitly

**Status:** Accepted

### Context

Reviews may be running, complete successfully, or fail during analysis.

Repeated requests for the same Pull Request commit should not automatically trigger unnecessary AI analysis.

### Decision

Track review records using these states:

- `IN_PROGRESS`
- `COMPLETED`
- `FAILED`

Reuse completed review results for the same review identity. Permit failed reviews to be retried.

### Rationale

- Makes review outcomes explicit.
- Supports basic idempotency for repeated requests.
- Provides a foundation for retry and recovery behavior.
- Makes operational troubleshooting easier.

### Trade-offs

- Concurrent review requests require coordination.
- Interrupted processing can leave stale `IN_PROGRESS` records.
- Persistence idempotency does not guarantee exactly-once GitHub comment publication.

---

## ADR-006: Introduce BullMQ and Redis for Asynchronous Processing

**Status:** In progress

### Context

AI review processing can take longer than the time a webhook handler should ideally spend processing an incoming event.

A background queue can separate webhook acceptance from review execution.

### Decision

Introduce BullMQ backed by Redis for background review jobs.

The intended processing flow is:

1. The Node.js API validates the GitHub webhook.
2. The API prepares a review input.
3. The API enqueues a review job.
4. A background worker consumes the job.
5. The worker calls the Java review service.
6. The worker maps findings and publishes valid GitHub comments.

### Rationale

- Decouples webhook handling from potentially slow AI processing.
- Provides configurable retry behavior.
- Allows the worker to run independently of the API process.
- Creates a foundation for scaling review processing.

### Trade-offs

- Adds Redis and queue operations to the infrastructure.
- Requires worker lifecycle management and graceful shutdown.
- Jobs can be retried or processed more than once.
- Duplicate-job handling and safe GitHub comment publication need explicit design.

### Implementation note

The Redis infrastructure and BullMQ dependencies have been configured. The complete asynchronous processing flow must be verified before this decision can be considered fully implemented.

---

## ADR-007: Validate AI Findings Against the Pull Request Diff

**Status:** Accepted

### Context

AI-generated line references may not correspond to lines that can be commented on in a GitHub Pull Request diff.

### Decision

Validate findings against changed file patches before creating GitHub inline review comments.

Findings that cannot be mapped to valid changed lines are skipped.

### Rationale

- Reduces invalid inline comment submissions.
- Keeps comments connected to the actual code changes.
- Prevents publishing findings at unsupported line positions.

### Trade-offs

- Some otherwise useful findings may be skipped if they cannot be mapped.
- Diff parsing and line mapping require careful testing.

---

## ADR-008: Keep Local Development Infrastructure in Docker Compose

**Status:** Accepted

### Context

The project needs PostgreSQL and Redis during local development. Adminer is also used for browser-based database administration.

### Decision

Maintain reusable Docker Compose infrastructure separately from the application source code.

The local setup contains:

- PostgreSQL
- Redis
- Adminer

### Rationale

- Provides repeatable local service configuration.
- Allows infrastructure services to be started and stopped independently.
- Supports reuse across local development projects.
- Keeps database administration tools separate from application code.

### Trade-offs

- Requires Docker to be available locally.
- Persistent data requires careful handling and backup practices.
- Local configuration must not be reused blindly for production.

---

## ADR-009: Separate Architecture Documentation from Implementation Guides

**Status:** Accepted

### Context

The project needs documentation for both system-level design and implementation-specific behavior.

### Decision

Organize technical documentation into separate directories:

- `docs/architecture/`
- `docs/implementation/`
- `docs/decisions/`

### Rationale

- Architecture documents explain components and interactions.
- Implementation guides explain how individual components work.
- Decision records capture why architectural choices were made.
- The README provides the entry point and links to detailed documentation.

### Trade-offs

- Documentation can become inconsistent if updates are not made alongside code changes.
- Cross-links and implementation-status notes need periodic review.

---

## 3. Decision Review Process

Review these decisions when a major architectural change occurs.

Examples include:

- Replacing or adding an AI provider.
- Changing the Node.js-to-Java service boundary.
- Introducing a new persistence technology.
- Changing queue or retry semantics.
- Changing GitHub integration or authentication.
- Moving from local infrastructure to production-managed services.

When a decision changes, update the relevant ADR rather than silently changing the documented architecture.

## 4. Related Documentation

### Architecture

- [System Architecture](../architecture/system-architecture.md)
- [GitHub Integration](../architecture/github-integration.md)
- [Review Processing](../architecture/review-processing.md)
- [Asynchronous Review Queue](../architecture/async-review-queue.md)
- [Review Lifecycle](../architecture/review-lifecycle.md)
- [Persistence Lifecycle](../architecture/persistence-lifecycle.md)
- [Inline Review Comments](../architecture/inline-review-comments.md)
- [Infrastructure](../architecture/infrastructure.md)
- [Deployment Architecture](../architecture/deployment-architecture.md)

### Implementation

- [Review Input](../implementation/review-input.md)
- [AI Analysis](../implementation/ai-analysis.md)
- [Redis and BullMQ](../implementation/redis-bullmq.md)
- [Testing](../implementation/testing.md)
