# The api package is a first-party seam, not a public contract

The upstream merge brought in the `api` package, and with it the add-on, although a public API had been a non-goal. We treat `api` as the seam for our own add-ons and promise no stability to third parties yet. That is why the add-on pins the exact base mod version.

The add-on now uses `api` alone: a Gradle check (`:automatisation:checkApiOnly`, part of `check` and so of `build`) fails if a file under `automatisation/src` imports any other `dev.bettercoppers` package. A missing hook gets added to `api`, which forwards to the base mod's internal code. `TravelWatch` lives in `api` for this reason.
