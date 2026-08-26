/**
 * API smoke tests against a deployed CSP environment.
 *
 * Invoked by reusable-tests.yml after the deploy job completes:
 *   BASE_URL=https://<env-host> node src/main.js
 *
 * Zero dependencies (Node 24 global fetch). Exits non-zero on the first
 * failure so the CI job fails.
 */

const baseUrl = (process.env.BASE_URL || '').replace(/\/+$/, '');
if (!baseUrl) {
  console.error('BASE_URL environment variable is required');
  process.exit(1);
}

let failures = 0;

async function check(name, fn) {
  try {
    await fn();
    console.log(`✅ ${name}`);
  } catch (err) {
    failures++;
    console.error(`❌ ${name}: ${err.message}`);
  }
}

function expect(condition, message) {
  if (!condition) throw new Error(message);
}

await check('GET /api/health returns 200 with status UP', async () => {
  const res = await fetch(`${baseUrl}/api/health`);
  expect(res.status === 200, `expected 200, got ${res.status}`);
  const body = await res.json();
  expect(body.status === 'UP', `expected status UP, got ${JSON.stringify(body)}`);
});

await check('GET /api/v3/api-docs returns the OpenAPI document', async () => {
  const res = await fetch(`${baseUrl}/api/v3/api-docs`);
  expect(res.status === 200, `expected 200, got ${res.status}`);
  const body = await res.json();
  expect(typeof body.openapi === 'string', 'response is not an OpenAPI document');
});

await check('POST /api/R10 without a token is rejected with 401', async () => {
  const res = await fetch(`${baseUrl}/api/R10`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reportFormat: 'CSV', dateFrom: '20240101', dateTo: '20240131' }),
  });
  expect(res.status === 401, `expected 401, got ${res.status}`);
});

if (failures > 0) {
  console.error(`\n${failures} check(s) failed`);
  process.exit(1);
}
console.log('\nAll checks passed');
