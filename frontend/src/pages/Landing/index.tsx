import { Button, Column, Grid, InlineLoading, InlineNotification, Tile } from '@carbon/react';
import { Application, ArrowRight, ChartBar, Security } from '@carbon/icons-react';

import { useAuth } from '@/context/auth/useAuth';
import { useHealthQuery } from '@/services/health.service';

import './index.scss';

export function LandingPage() {
  const { user } = useAuth();
  const { data, isError, isSuccess, isFetching, refetch } = useHealthQuery();

  return (
    <div className="landing-page">
      <Grid fullWidth className="landing-page__banner">
        <Column lg={10} md={6} sm={4}>
          <h1 className="landing-page__heading">Welcome{user?.displayName ? `, ${user.displayName}` : ''}</h1>
          <p className="landing-page__subheading">
            This is a starting point for NR application teams. Replace this content with your application's purpose, key
            actions, and relevant information.
          </p>
          <div className="landing-page__actions">
            <Button renderIcon={ArrowRight} iconDescription="Get started">
              Get started
            </Button>
            <Button kind="secondary">Learn more</Button>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="landing-page__section">
        <Column lg={16} md={8} sm={4}>
          <h2 className="landing-page__section-heading">Backend connectivity</h2>
        </Column>
        <Column lg={6} md={4} sm={4}>
          <Tile className="landing-page__tile">
            {isFetching ? (
              <InlineLoading description="Checking backend..." />
            ) : (
              <Button kind="tertiary" onClick={() => refetch()}>
                Check backend health
              </Button>
            )}
            {isSuccess && (
              <InlineNotification
                kind="success"
                title={`Status: ${data.status}`}
                subtitle={`Server time: ${new Date(data.timestamp).toLocaleString()}`}
                hideCloseButton
              />
            )}
            {isError && (
              <InlineNotification kind="error" title="Error" subtitle="Could not reach backend" hideCloseButton />
            )}
          </Tile>
        </Column>
      </Grid>

      <Grid fullWidth className="landing-page__section">
        <Column lg={16} md={8} sm={4}>
          <h2 className="landing-page__section-heading">What CSP includes</h2>
          <p className="landing-page__section-description">
            Replace these tiles with the key features or entry points of your application.
          </p>
        </Column>

        <Column lg={5} md={4} sm={4}>
          <Tile className="landing-page__tile">
            <Application size={32} className="landing-page__tile-icon" />
            <h3 className="landing-page__tile-heading">Authentication</h3>
            <p className="landing-page__tile-body">
              Cognito OAuth via AWS Amplify. In development, toggle <code>VITE_MOCK_USER=true</code> to bypass the login
              flow entirely.
            </p>
          </Tile>
        </Column>

        <Column lg={5} md={4} sm={4}>
          <Tile className="landing-page__tile">
            <Security size={32} className="landing-page__tile-icon" />
            <h3 className="landing-page__tile-heading">Carbon Design System</h3>
            <p className="landing-page__tile-body">
              Pre-configured with the BC Gov NR-Theme. Dark and light modes switch instantly via the toggle in the
              header.
            </p>
          </Tile>
        </Column>

        <Column lg={5} md={4} sm={4}>
          <Tile className="landing-page__tile">
            <ChartBar size={32} className="landing-page__tile-icon" />
            <h3 className="landing-page__tile-heading">Ready to extend</h3>
            <p className="landing-page__tile-body">
              Add routes, pages, and API calls on top of this foundation. The side nav updates from a single{' '}
              <code>navigation.ts</code> config.
            </p>
          </Tile>
        </Column>
      </Grid>
    </div>
  );
}
