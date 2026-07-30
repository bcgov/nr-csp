import { Amplify } from 'aws-amplify';
import ReactDOM from 'react-dom/client';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getAmplifyConfig } from './amplify-initializer';

const envState = vi.hoisted(() => ({ mockUser: true }));
const renderSpy = vi.hoisted(() => vi.fn());

vi.mock('./env', () => ({ env: envState }));

vi.mock('./styles/index.scss', () => ({}));

vi.mock('./App', () => ({
  default: () => <div>app</div>,
}));

vi.mock('./amplify-initializer', () => ({
  getAmplifyConfig: vi.fn(() => ({ Auth: { Cognito: {} } })),
}));

vi.mock('aws-amplify', () => ({
  Amplify: { configure: vi.fn() },
}));

vi.mock('react-dom/client', () => ({
  default: {
    createRoot: vi.fn(() => ({ render: renderSpy, unmount: vi.fn() })),
  },
}));

const mockCreateRoot = vi.mocked(ReactDOM.createRoot);
const mockConfigure = vi.mocked(Amplify.configure);
const mockGetAmplifyConfig = vi.mocked(getAmplifyConfig);

async function importMain() {
  vi.resetModules();
  await import('./main');
}

describe('main bootstrap', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '<div id="root"></div>';
  });

  it('renders the app into the #root element', async () => {
    envState.mockUser = true;
    await importMain();

    expect(mockCreateRoot).toHaveBeenCalledTimes(1);
    expect(mockCreateRoot).toHaveBeenCalledWith(document.getElementById('root'));
    expect(renderSpy).toHaveBeenCalledTimes(1);
  });

  it('skips Amplify configuration in mock mode', async () => {
    envState.mockUser = true;
    await importMain();

    expect(mockGetAmplifyConfig).not.toHaveBeenCalled();
    expect(mockConfigure).not.toHaveBeenCalled();
  });

  it('configures Amplify with the runtime config when not in mock mode', async () => {
    envState.mockUser = false;
    const amplifyConfig = { Auth: { Cognito: {} } };
    mockGetAmplifyConfig.mockReturnValue(amplifyConfig as ReturnType<typeof getAmplifyConfig>);

    await importMain();

    expect(mockGetAmplifyConfig).toHaveBeenCalledTimes(1);
    expect(mockConfigure).toHaveBeenCalledWith(amplifyConfig);
    expect(renderSpy).toHaveBeenCalledTimes(1);
  });
});
