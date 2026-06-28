# TQUEUE - Async Task Engine

A production-inspired asynchronous task processing engine built with Spring Boot and PostgreSQL to explore how reliable background job processing systems work internally.

---

## Motivation

Modern backend applications frequently need to execute long-running or failure-prone work outside the request-response lifecycle.

Examples include:

* Sending emails
* Generating reports
* Processing images
* Video transcoding
* Background data synchronization

Rather than relying immediately on Kafka, RabbitMQ, or Redis-based job queues, this project focuses on understanding the underlying engineering challenges by implementing the core mechanisms from scratch.

---

## Architecture

```text

                 Client
                    │
                    ▼
          POST /tasks (REST API)
                    │
                    ▼
        PostgreSQL Task Queue
                    │
      Poll + Claim (SKIP LOCKED)
                    │
                    ▼
        ThreadPoolTaskExecutor
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
     Worker 1               Worker 2
        │                       │
        └───────────┬───────────┘
                    ▼
          Business Task Handler
                    │
          Mark COMPLETED / RETRY


```
---

## Features

* PostgreSQL-backed durable task storage
* Atomic task claiming using `FOR UPDATE SKIP LOCKED`
* ThreadPool-based asynchronous execution
* Multi-worker coordination
* Heartbeat-based worker liveness detection
* Dead-man switch recovery
* Retry mechanism with exponential backoff and jitter
* Executor saturation detection and recovery
* Runtime metrics endpoint
* Multi-instance worker support
* At-least-once execution semantics

---

## Task Lifecycle

```text
               PENDING
                  │
                  ▼
             CLAIMED
                  │
                  ▼
            PROCESSING
        ┌─────────┴─────────┐
        │                   │
        ▼                   ▼
   COMPLETED         Runtime Exception
                            │
                            ▼
                 Retry (Backoff + Jitter)
                            │
                            ▼
                         PENDING

          Worker Crash
               │
               ▼
      Heartbeat Timeout
               │
               ▼
      Dead-man Switch
               │
               ▼
            Reclaimed

```
## Worker crash recovery:

```text
Worker
 │
 │ Heartbeat (10s)
 ▼
Database
 │
 │ Last heartbeat updated
 ▼

Worker crashes
 │
 ▼
No heartbeat

Main Scheduler
 │
 │ Every 30s
 ▼

Heartbeat older than threshold?

      YES
       │
       ▼
Move task back to PENDING

       │
       ▼
Another worker claims task
```

---

## Multi Worker Co-ordination

```text
                  PostgreSQL
              Task(id=101,PENDING)
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
 Spring Boot A              Spring Boot B
        │                         │
 SELECT ... SKIP LOCKED   SELECT ... SKIP LOCKED
        │                         │
   Row Locked               Skips Locked Row
        │                         │
        ▼                         ▼
 Processes Task              Claims Next Task
```

## Failure Scenarios Covered

| Scenario                       | Solution                       |
| ------------------------------ | ------------------------------ |
| Duplicate task claiming        | `FOR UPDATE SKIP LOCKED`       |
| Worker crash                   | Heartbeat + Dead-man switch    |
| Runtime failures               | Retry with exponential backoff |
| Executor saturation            | Requeue with retry delay       |
| Multiple application instances | Atomic row locking             |
| Long-running tasks             | Heartbeat lease renewal        |

---

## Metrics

Example:

```json
{
  "queue": {
    "pending": 12,
    "processing": 3,
    "completed": 487,
    "failed": 2
  },
  "workers": {
    "active": 4,
    "poolSize": 5,
    "queueSize": 6
  }
}
```

---

## Key Learnings

This project helped me understand:

* Reliable background processing
* Worker coordination
* Failure recovery
* Retry strategies
* Heartbeat-based leases
* At-least-once execution
* Why business services own idempotency
* Trade-offs between durability and throughput

---

## Future Improvements

* Redis-backed queue implementation
* Transactional Outbox Pattern
* OpenTelemetry integration
* Prometheus metrics
* Dashboard for queue monitoring
* Pluggable task handlers
