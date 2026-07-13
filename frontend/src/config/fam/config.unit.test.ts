import { afterEach, describe, expect, it, vi } from 'vitest';

type AmplifyConfig = NonNullable<typeof globalThis.amplifyConfig>;

const baseConfig: AmplifyConfig = {
  appEnv: 'test',
  idpName: 'TEST-IDIR',
  region: 'ca-central-1',
  userPoolId: 'pool',
  userPoolClientId: 'client',
  cognitoDomain: 'auth.example.com',
  redirectSignIn: 'https://example.com/',
  redirectSignOut: 'https://example.com/logout',
};

/** Re-imports the module so its module-level reads re-run with current globals. */
async function importFamConfig() {
  vi.resetModules();
  const mod = await import('@/config/fam/config');
  return mod.famConfig;
}

describe('famConfig', () => {
  afterEach(() => {
    globalThis.amplifyConfig = undefined;
  });

  it('reads clientId and environment from globalThis.amplifyConfig', async () => {
    globalThis.amplifyConfig = { ...baseConfig, famClientId: 'fam-client-123', appEnv: 'prod' };

    const famConfig = await importFamConfig();
    expect(famConfig.clientId).toBe('fam-client-123');
    expect(famConfig.environment).toBe('prod');
  });

  it('defaults clientId to empty string when famClientId is missing', async () => {
    globalThis.amplifyConfig = { ...baseConfig };

    const famConfig = await importFamConfig();
    expect(famConfig.clientId).toBe('');
    expect(famConfig.environment).toBe('test');
  });

  it('falls back to empty clientId and "dev" environment without amplifyConfig', async () => {
    globalThis.amplifyConfig = undefined;

    const famConfig = await importFamConfig();
    expect(famConfig.clientId).toBe('');
    expect(famConfig.environment).toBe('dev');
  });
});
