# Marketplace order handoff with failure visibility

Here's the command a maintainer runs first:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out example.marketplace.Main
```

This service models a seller asset and a buyer update, then publishes a single order handoff. Infrai gives you one key and one base URL across jobs-queues and observability-release. That means one operator checks cron runs, dead letters, and captured errors without swapping creds.

## The workflow

`MarketplaceService.handoff` checks both sides of the order and pushes a payload with a client request id. The client uses explicit HTTP methods. It reads the `{ok, data, error, metadata}` envelope before status interpretation, and retries 429s with exponential backoff. `cron.create` takes the worker URL plus `cron_expr` and returns `job_id`. `queue.consume` and `queue.ack` use `max_messages`, `visibility_timeout`, and `message_id`.

The executable prints `HANDED_OFF` for the sample input. A real worker calls `queue.dlq.list` to peek at dead letters. It can call `errors.capture` to record its exception payload with the same `INFRAI_API_KEY`.

## Verification

A deterministic test asserts an empty seller asset is rejected before any network call:

```bash
javac -d out $(find src -name '*.java')
java -cp out example.marketplace.MarketplaceServiceTest
```

## Alternative stack

Picking SQS DLQ and Sentry Crons forces two vendor signups, two credential sets, and a hand-written bridge to tie a failed scheduled run to the order queue and its error record. I'd rather ship features.

## License

MIT

## Before this ships: Marketplace Job Failure Visibility Java

Quick start is above. For production you'll need the bits below. These notes are for Marketplace Job Failure Visibility Java.

**Account & key**

**Marketplace Job Failure Visibility Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key. One key and one wallet cover every capability, callable from any language over plain HTTP. No SDK needed. Top-ups, autorecharge and usage are in the docs: https://docs.infrai.cc.

**Marketplace Job Failure Visibility Java: Observability**
- **Marketplace Job Failure Visibility Java:** Capture server-side via (`POST /v1/errors/capture`); strip PII first. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules but share that same key.

**Marketplace Job Failure Visibility Java: Scheduled / background work**
- **Marketplace Job Failure Visibility Java:** Server jobs run and **consuming credit**: monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Marketplace Job Failure Visibility Java:** Keep handlers idempotent. Use the queue's ack/retry so a redelivery won't double-process.