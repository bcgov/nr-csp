#!/usr/bin/env bash
# ============================================================================
# Reset the local seeded DB back to the published snapshot.
#
# WHAT IT DOES: destroys the DB container and recreates it from the image, then
# waits until Oracle genuinely accepts a connection, then re-applies the seed
# patches (if any). The result is the snapshot state, exactly as published.
#
# WHY RECREATE RATHER THAN ROLL BACK: the container has NO volumes, so all of
# Oracle's datafiles live in its writable layer — the image IS the snapshot and
# a fresh container is bit-for-bit that state. An in-place Oracle FLASHBACK
# DATABASE would be faster, but the published image runs NOARCHIVELOG with
# flashback off and no recovery area, so it is not available without
# reconfiguring and republishing the image.
#
# WHEN YOU NEED IT: write scenarios clean up after themselves via the cleanup
# registry, so routine runs should not drift. Use this when a cleanup failed and
# left residue, when a test wrote something you cannot identify, or simply to
# prove a green run was not depending on leftovers from an earlier one.
#
# COST: roughly 1-3 minutes (container teardown dominates; Oracle then needs
# ~40s to open). Not something to run between tests — it is the escape hatch.
#
# SETUP: nothing to configure. Config comes from .env / the environment:
#     DB_CONTAINER  (default real-data-seeded-csp-db)
#     DB_IMAGE      (default ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:latest)
#     DB_PORT       (default 1525 — the host port Oracle's 1521 maps to)
#
#   Usage:  ./scripts/reset-db.sh
# ============================================================================
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# PRECEDENCE: an explicitly-exported variable must beat .env, so
# `DB_CONTAINER=scratch ./scripts/reset-db.sh` targets a throwaway container instead of your real
# one. Sourcing .env with `set -a` OVERWRITES what the caller passed, which for a script whose
# first act is `docker rm -f` would mean destroying the wrong container. So capture the caller's
# values first and restore them afterwards.
_ENV_DB_CONTAINER="${DB_CONTAINER:-}"
_ENV_DB_IMAGE="${DB_IMAGE:-}"
_ENV_DB_PORT="${DB_PORT:-}"

ENV_FILE="$HERE/../.env"
# shellcheck disable=SC1090
[ -f "$ENV_FILE" ] && { set -a; . "$ENV_FILE"; set +a; }

DB_CONTAINER="${_ENV_DB_CONTAINER:-${DB_CONTAINER:-real-data-seeded-csp-db}}"
DB_IMAGE="${_ENV_DB_IMAGE:-${DB_IMAGE:-ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:latest}}"
DB_PORT="${_ENV_DB_PORT:-${DB_PORT:-1525}}"

command -v docker >/dev/null 2>&1 || { echo "ERROR: docker not found on PATH." >&2; exit 1; }

if ! docker image inspect "$DB_IMAGE" >/dev/null 2>&1; then
  echo "ERROR: image '$DB_IMAGE' is not present locally." >&2
  echo "       docker login ghcr.io -u <your-github-username>   # PAT with read:packages" >&2
  echo "       docker pull $DB_IMAGE" >&2
  exit 1
fi

echo "Resetting '$DB_CONTAINER' to the snapshot in $DB_IMAGE"
echo "  (destroys the container; all data written since it was created is lost)"

# Teardown can be slow: the writable layer holds the whole Oracle datafile set.
echo "==> removing the existing container (this can take a minute)"
docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true

echo "==> starting a fresh container on port $DB_PORT"
docker run -d --name "$DB_CONTAINER" -p "${DB_PORT}:1521" "$DB_IMAGE" >/dev/null

# `docker ps` reports Up well before Oracle can serve, and the listener transiently
# returns ORA-12514 during startup — so poll for a REAL connection, and require a
# few consecutive successes so a flicker mid-startup is not mistaken for ready.
# 12 min, not 5: verified that a second concurrent Oracle instance on a 16GB machine pushes startup
# past 5 minutes (alone this image opens in ~40s). Progress is printed so a slow start is visibly
# progressing rather than looking hung.
echo "==> waiting for Oracle to accept connections (usually ~40s; longer if another Oracle is running)"
ok=0
for i in $(seq 1 144); do
  if docker exec "$DB_CONTAINER" bash -c \
       "echo 'select 1 from dual;' | sqlplus -S -L THE/default@localhost:1521/DBDOCK_01" 2>/dev/null \
       | grep -qE '^ *1$'; then
    ok=$((ok + 1))
    if [ "$ok" -ge 3 ]; then echo "    ready after ~$((i * 5))s"; break; fi
  else
    ok=0
  fi
  [ $((i % 12)) -eq 0 ] && echo "    still starting... ($((i * 5))s)"
  sleep 5
done
if [ "$ok" -lt 3 ]; then
  echo "ERROR: Oracle did not accept connections within ~12 minutes." >&2
  echo "       It may still be starting - check progress with:" >&2
  echo "         docker logs --tail 20 $DB_CONTAINER      # look for 'ALTER DATABASE OPEN'" >&2
  echo "       Startup is much slower when another Oracle container is running; 'docker ps' will" >&2
  echo "       show if one is. The container was NOT removed, so nothing is lost - re-run this" >&2
  echo "       script, or just wait and retry your tests." >&2
  exit 1
fi

# Seed patches are NOT baked into the image, so a fresh container needs them again.
# apply-patches.sh is a no-op when there are none (CSP currently has none).
if [ -x "$HERE/apply-patches.sh" ]; then
  echo "==> re-applying seed patches"
  DB_CONTAINER="$DB_CONTAINER" ORACLE_DSN="THE/default@localhost:${DB_PORT}/DBDOCK_01" \
    "$HERE/apply-patches.sh"
fi

cat <<DONE

Reset complete — the DB matches the published snapshot.

NEXT: restart the backend so it does not serve a reference-data cache warmed
against the old container, then re-run the suite:
    docker restart csp-backend-e2e     # or however you run the backend
    npm test
DONE
