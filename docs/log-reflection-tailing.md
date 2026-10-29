# Log reflection: observation and tailing strategy

This document records the current design deliberately. It is both an operating contract and a decision log: future
implementations may replace individual mechanisms, but must preserve or explicitly revise their continuity semantics.

## Two sources, two promises

Log reflection exposes two fundamentally different sources.

| Source | Purpose | Continuity promise |
| --- | --- | --- |
| Structured live log | Low-latency observation from now on | Complete only inside one active collector generation and its bounded ring buffer |
| Log file stream | Durable history, initial views, downloads and recovery | Bounded by the files and rotations still present on the node |

The structured live log is not a cheaper historical log store. Log files are not the preferred high-frequency live
channel. Keeping this distinction visible prevents a convenient UI from silently claiming completeness it cannot
provide.

## Structured live-log lifecycle

The collector is demand driven:

1. No appender is attached while no live query is active. No `LogRecord` objects are created for log reflection.
2. The first query on a node creates a collector generation, attaches the appender and starts an empty ring buffer.
3. Every subsequent query renews the node-local lease.
4. After the configured quiet period, the appender is detached and the buffer is released.
5. A later query creates a new generation. It cannot continue a cursor from the previous generation.

Leases are preferable to subscribe/unsubscribe reference counting. Browsers, network connections and pods may vanish
without sending a final request; lease expiry cleans them up automatically.

The relevant configuration properties are:

- `liveLogCapacity` (default `10000` records per node while active)
- `liveLogLeaseMillis` (default `30000` ms after the latest query)
- `tailPollIntervalMillis` (default `1000` ms in the web client)

## Cursors and continuity gaps

A local live cursor has the form `live-v1:<generation>:<sequence>`. The generation makes process restarts, lease expiry
and collector reactivation distinguishable from a normal continuation. The sequence identifies a record inside that
generation.

A response reports a continuity gap for a node when:

- its requested generation is no longer active;
- its cursor predates the oldest record retained by the ring buffer; or
- its cursor is otherwise outside the active generation.

Records returned after a gap are valid, but they are not a complete continuation. The UI keeps the warning attached to
the view until a deliberate reload/filter reset creates a fresh view. It must never silently hide the gap.

For multi-node queries the cluster cursor is a map of node identity to local cursor. Each pod owns its collector,
generation and retention truth. The cluster processor merges records and continuity gaps; it does not invent a global
cursor. A newly joined, restarted or temporarily unreachable node can therefore be identified independently.

Ordering across nodes is deterministic but only as temporally accurate as their clocks. This is a merged observation,
not a distributed total-order protocol.

## Polling behavior

The web client uses non-blocking requests (`waitMillis = 0`) and exactly one in-flight request per tailing tab:

- the next poll starts only after the previous response completed;
- slow responses coalesce/skip nominal intervals instead of accumulating requests;
- a bounded server backlog (`moreAvailable`) is drained immediately, still sequentially;
- failures back off;
- stale responses from an older filter or tail generation are ignored;
- hidden documents pause requests, while the server lease provides a short reconnection grace period.

The server-side long-poll parameter remains compatible for other clients, but the web application does not consume a
request thread while waiting for new records.

## Filtering and time ranges

Server-side filters define a view, not a transformation of the already visible browser buffer. Changing a filter starts
a fresh query and cursor so matching records that were absent from the old view can be found.

Time-range rules:

- a lower bound is compatible with tailing;
- no upper bound means tail indefinitely;
- a future upper bound means tail until that instant;
- an upper bound already reached makes the view static and disables tailing.

The structured live source can only satisfy a past lower bound as far back as its current generation and retention. Use
file streams when historical completeness matters.

## File-tail hardening

File cursors use byte offsets plus a small content anchor. Reads are bounded, consume only complete physical lines and
align bounded initial windows to a real record start. Cursor anchors detect copy-truncate and fast regrowth. When a file
rotates, continuation searches the known rotated segments, including compressed segments, before moving to the new
active file.

These mechanisms make file tailing recoverable, but more expensive and format-dependent than the structured live
channel. They exist primarily for history and continuity recovery.

## Frozen local views

A local snapshot is a frozen copy of the records represented by another record tab. It:

- have no server, cursor, lease or tailing relationship;
- states its source tab, capture time and record count;
- supports inexpensive local filtering;
- never implies that it contains records outside the captured client buffer.

The source membership is immutable; filter results are a derived projection. A descendant snapshot captures the complete
filtered projection of its parent, never a partial intermediate rendering. Snapshot creation is disabled while its
parent's local filter scan is incomplete. This is preferable to adding a second, ambiguous "local filter" mode to a
live tab: the observation boundary and the absence of any server relationship stay explicit.

Local filtering and the integration of larger server pages are continuations. Work is bounded by both a record batch
and a short main-thread time slice, then scheduled as a new task. Every slice publishes its partial result so the browser
can paint and accept input. A filter revision/generation cancels obsolete continuations. This improves time-to-first-row
and responsiveness; it does not turn the client buffer into durable history or remove the eventual need for viewport
virtualization if substantially larger buffers are introduced.

## Replacement criteria

The live mechanism may later be replaced by streaming transport, JSONL files or another structured log facility. A
replacement should be evaluated against these explicit properties:

- no permanent cost when nobody observes;
- bounded memory and request concurrency;
- per-node cursor identity;
- explicit detection of every known discontinuity;
- deterministic merge behavior;
- separation of ephemeral live observation from durable history;
- no self-exciting logging loop caused by observation requests.

