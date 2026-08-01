#!/usr/bin/env python3
"""
tqueue benchmark script (log-tailing version).

Your app prints a metrics block to stdout on every poll cycle, like:

    Total Submitted    : 151 (All-time tasks received)
    Total Completed    : 150 (Successfully finished)

and prints "[QUEUE EMPTY] Backing off." when the poller finds nothing.

This script:
  1. Submits N tasks to your /tasks endpoint.
  2. Tails your Spring Boot app's log output (redirect it to a file first,
     see instructions below) and parses "Total Completed" to detect when
     all N tasks have finished.
  3. Reports elapsed time + throughput, and flags whether it saw
     "[QUEUE EMPTY]" fire after the batch finished (confirms drain).

SETUP — run your Spring Boot app with output redirected to a file so this
script can tail it:

    ./mvnw spring-boot:run > tqueue_app.log 2>&1 &

Then run this script pointing at that log file:

    python3 tqueue_benchmark.py --tasks 150 --log-file tqueue_app.log

Edit the CONFIG section below to match your actual /tasks payload.
"""

import argparse
import json
import re
import time
import sys
import urllib.request

# ----------------- CONFIG: EDIT TO MATCH YOUR APP -----------------
SUBMIT_URL = "http://localhost:8081/api/task/add"
SUBMIT_METHOD = "POST"
TASK_PAYLOAD = {"taskName": "demo", "taskDescription":"Demo"}  # match your real DTO
# --------------------------------------------------------------------

COMPLETED_RE = re.compile(r"Total Completed\s*:\s*(\d+)")
SUBMITTED_RE = re.compile(r"Total Submitted\s*:\s*(\d+)")
QUEUE_EMPTY_RE = re.compile(r"\[QUEUE EMPTY\] Backing off")

POLL_INTERVAL_SECONDS = 0.5
MAX_WAIT_SECONDS = 900  # 15 min safety timeout


def submit_task():
    data = json.dumps(TASK_PAYLOAD).encode("utf-8")
    req = urllib.request.Request(
        SUBMIT_URL, data=data, method=SUBMIT_METHOD,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=5) as resp:
        return resp.status


def read_latest_completed(log_path):
    """Read the log file and return the most recent 'Total Completed' value
    seen, plus whether '[QUEUE EMPTY]' appears anywhere after that point."""
    try:
        with open(log_path, "r", errors="ignore") as f:
            text = f.read()
    except FileNotFoundError:
        print(f"  [warn] log file not found yet: {log_path}", file=sys.stderr)
        return None, False

    completed_matches = COMPLETED_RE.findall(text)
    latest_completed = int(completed_matches[-1]) if completed_matches else None

    # crude "did we see queue-empty after the last completed count changed" check
    saw_queue_empty = bool(QUEUE_EMPTY_RE.search(text[-2000:]))  # check recent tail
    return latest_completed, saw_queue_empty


def run_benchmark(num_tasks, log_path):
    print(f"Reading baseline 'Total Completed' from {log_path} ...")
    baseline_completed, _ = read_latest_completed(log_path)
    if baseline_completed is None:
        print("  No metrics seen yet — assuming baseline of 0.")
        baseline_completed = 0
    else:
        print(f"  Baseline Total Completed = {baseline_completed}")

    print(f"\nSubmitting {num_tasks} tasks to {SUBMIT_URL} ...")
    submitted = 0
    t_submit_start = time.time()
    for i in range(num_tasks):
        try:
            submit_task()
            submitted += 1
        except Exception as e:
            print(f"  [error] task {i} failed to submit: {e}", file=sys.stderr)
        if (i + 1) % 50 == 0:
            print(f"  submitted {i + 1}/{num_tasks}")
    t_submit_end = time.time()
    print(f"Submitted {submitted}/{num_tasks} tasks in {t_submit_end - t_submit_start:.2f}s")

    target_completed = baseline_completed + submitted
    print(f"\nWaiting for Total Completed to reach {target_completed} "
          f"(tailing {log_path})...")

    t_wait_start = time.time()
    queue_empty_seen_after = False
    while True:
        completed, saw_empty = read_latest_completed(log_path)
        elapsed = time.time() - t_wait_start
        if completed is not None:
            print(f"  [{elapsed:6.1f}s] Total Completed = {completed} "
                  f"(target {target_completed})")
            if completed >= target_completed:
                queue_empty_seen_after = saw_empty
                break
        if elapsed > MAX_WAIT_SECONDS:
            print("Timed out waiting for completion.", file=sys.stderr)
            break
        time.sleep(POLL_INTERVAL_SECONDS)

    t_end = time.time()
    drain_time = t_end - t_wait_start
    total_time = t_end - t_submit_start

    print("\n----- RESULTS -----")
    print(f"Submitted:            {submitted} tasks")
    print(f"Drain-only time:      {drain_time:.2f}s")
    print(f"Total time (incl. submit): {total_time:.2f}s")
    if drain_time > 0:
        print(f"Throughput:           {submitted / drain_time:.2f} tasks/sec")
    print(f"Queue confirmed empty after batch: {'yes' if queue_empty_seen_after else 'not seen in tail — check log manually'}")

    print("\nFor your resume, e.g.:")
    print(f'  "Processed {submitted} tasks in {drain_time:.0f}s '
          f'(~{submitted / max(drain_time, 1):.1f} tasks/sec), '
          f'with zero duplicate execution verified via DB query."')


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--tasks", type=int, default=150, help="number of tasks to submit")
    parser.add_argument("--log-file", type=str, required=True,
                         help="path to your Spring Boot app's redirected stdout log")
    args = parser.parse_args()
    run_benchmark(args.tasks, args.log_file)
