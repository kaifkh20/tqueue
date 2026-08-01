#!/usr/bin/env python3
"""
tqueue benchmark script.

Measures:
  1. Throughput: how long it takes N tasks to go from submitted -> completed,
     using your /metrics endpoint to detect drain.
  2. (Optional) Crash-recovery time: run with --watch-only while you manually
     kill a worker process, and it'll print how long tasks sat in PENDING
     again before being picked back up.

BEFORE RUNNING — edit the CONFIG section below to match your actual API:
  - SUBMIT_URL / SUBMIT_METHOD / TASK_PAYLOAD: match your real POST /tasks controller
  - METRICS_URL: match your real GET /metrics endpoint (one per worker instance
    is fine — they all read from the same Postgres queue table)

Usage:
    python3 tqueue_benchmark.py --tasks 1000
    python3 tqueue_benchmark.py --tasks 1000 --watch-only   # just watch metrics, don't submit
"""

import argparse
import json
import time
import sys
import urllib.request
import urllib.error

# ----------------- CONFIG: EDIT THESE TO MATCH YOUR APP -----------------
SUBMIT_URL = "http://localhost:8081/api/task/add"
SUBMIT_METHOD = "POST"
TASK_PAYLOAD = {"taskName": "demo", "taskDescription":"Task Demo"}  # match your actual DTO shape

# List every worker instance's metrics endpoint (or just one — they should
# all reflect the same shared queue state if backed by the same Postgres DB)
METRICS_URLS = [
    "http://localhost:8081/api/metrics",
    "http://localhost:8082/api/metrics",
    "http://localhost:8083/api/metrics",
]

POLL_INTERVAL_SECONDS = 1.0
MAX_WAIT_SECONDS = 600  # give up after 10 minutes
# --------------------------------------------------------------------------


def submit_task():
    data = json.dumps(TASK_PAYLOAD).encode("utf-8")
    req = urllib.request.Request(
        SUBMIT_URL, data=data, method=SUBMIT_METHOD,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=5) as resp:
        return resp.status


def get_metrics(url):
    try:
        with urllib.request.urlopen(url, timeout=5) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except (urllib.error.URLError, json.JSONDecodeError) as e:
        print(f"  [warn] could not reach {url}: {e}", file=sys.stderr)
        return None


def aggregate_metrics():
    """Pull metrics from every instance and sum the queue counters.
    Assumes each instance's /metrics reflects the shared DB state, so in
    practice you may only need ONE of them — adjust if your endpoint
    already returns a global view."""
    latest = None
    for url in METRICS_URLS:
        m = get_metrics(url)
        if m:
            latest = m  # last successful read wins; edit if you need summing
    return latest


def run_throughput_test(num_tasks):
    print(f"Submitting {num_tasks} tasks to {SUBMIT_URL} ...")
    submitted = 0
    t_submit_start = time.time()
    for i in range(num_tasks):
        try:
            submit_task()
            submitted += 1
        except Exception as e:
            print(f"  [error] task {i} failed to submit: {e}", file=sys.stderr)
        if (i + 1) % 100 == 0:
            print(f"  submitted {i + 1}/{num_tasks}")
    t_submit_end = time.time()
    print(f"Submitted {submitted}/{num_tasks} tasks in {t_submit_end - t_submit_start:.2f}s\n")

    print("Polling /metrics until queue drains (pending == 0 and processing == 0)...")
    t_wait_start = time.time()
    while True:
        m = aggregate_metrics()
        if m:
            q = m.get("queue", {})
            pending = q.get("pending", "?")
            processing = q.get("processing", "?")
            completed = q.get("completed", "?")
            failed = q.get("failed", "?")
            elapsed = time.time() - t_wait_start
            print(f"  [{elapsed:6.1f}s] pending={pending} processing={processing} "
                  f"completed={completed} failed={failed}")
            if pending == 0 and processing == 0:
                break
        if time.time() - t_wait_start > MAX_WAIT_SECONDS:
            print("Timed out waiting for drain.", file=sys.stderr)
            break
        time.sleep(POLL_INTERVAL_SECONDS)

    t_end = time.time()
    total_time = t_end - t_submit_start
    drain_time = t_end - t_wait_start
    print("\n----- RESULTS -----")
    print(f"Total wall time (submit + drain): {total_time:.2f}s")
    print(f"Drain-only time:                  {drain_time:.2f}s")
    if drain_time > 0:
        print(f"Approx throughput:                {submitted / drain_time:.1f} tasks/sec")
    print("\nUse these numbers for your resume, e.g.:")
    print(f'  "Processed {submitted} tasks across {len(METRICS_URLS)} concurrent workers '
          f'in {drain_time:.0f}s (~{submitted / max(drain_time,1):.0f} tasks/sec)."')


def run_watch_only():
    """Just print metrics every second. Use this while you manually
    kill -9 a worker process to measure crash-recovery time:
      1. Start this in one terminal.
      2. In another, submit a batch of tasks.
      3. Once tasks are 'processing', kill one worker's PID.
      4. Watch how long until 'pending' rises then drains again —
         that's your recovery time."""
    print("Watching metrics (Ctrl+C to stop). Kill a worker now to measure recovery time.")
    t0 = time.time()
    try:
        while True:
            m = aggregate_metrics()
            if m:
                q = m.get("queue", {})
                w = m.get("workers", {})
                print(f"[{time.time()-t0:6.1f}s] "
                      f"pending={q.get('pending')} processing={q.get('processing')} "
                      f"completed={q.get('completed')} failed={q.get('failed')} "
                      f"active_workers={w.get('active')}")
            time.sleep(POLL_INTERVAL_SECONDS)
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--tasks", type=int, default=1000, help="number of tasks to submit")
    parser.add_argument("--watch-only", action="store_true",
                         help="just watch metrics, don't submit tasks (for manual crash test)")
    args = parser.parse_args()

    if args.watch_only:
        run_watch_only()
    else:
        run_throughput_test(args.tasks)
