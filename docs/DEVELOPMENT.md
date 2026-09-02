# Development

## Running `trail-plan.scala`

`trail-plan.scala` is executable and uses the user-installed `instant-scala`
runner. The runner caches its native image under `.scala-build/` and rebuilds
when the script content changes.

Canonical local run:

```bash
chmod +x ./trail-plan.scala && ./trail-plan.scala --input ./trails --output day --valhalla-url http://localhost:8002
```

When filtering output in a one-liner, always retain the complete run log.
Preserve the planner exit status, and print the full log if the run fails or
the filter matches nothing:

```bash
log=/tmp/trail-plan.$$.log; ./trail-plan.scala --input ./trails --output day --valhalla-url http://localhost:8002 2>&1 | tee "$log" | grep -E '^(FAIL|SELF-TESTS)'; rc=${PIPESTATUS[0]}; if [ "$rc" -ne 0 ] || ! grep -qE '^(FAIL|SELF-TESTS)' "$log"; then cat "$log"; fi; exit "$rc"
```

Do not infer planner behavior from filtered output when the underlying run
failed. Use the saved full log instead of rerunning solely to recover hidden
build/compiler output.
