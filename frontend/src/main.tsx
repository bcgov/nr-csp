import React from 'react';
import ReactDOM from 'react-dom/client';

import { Amplify } from 'aws-amplify';

import { getAmplifyConfig } from './amplify-initializer';
import { env } from './env';
import App from './App';
import './styles/index.scss';

// Skip Amplify configuration in mock mode — window.amplifyConfig may not be set.
if (!env.mockUser) {
  Amplify.configure(getAmplifyConfig());
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
