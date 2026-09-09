import { Close } from '@carbon/icons-react';
import { HeaderPanel, IconButton } from '@carbon/react';
import { type FC } from 'react';

import { HeaderPanelProfile } from '@/components/Layout/HeaderPanelProfile';
import { useLayout } from '@/context/layout/useLayout';

import './index.scss';

export const LayoutHeaderPanel: FC = () => {
  const { isHeaderPanelOpen, closeHeaderPanel } = useLayout();

  if (!isHeaderPanelOpen) return null;

  return (
    <>
      {/* Scrim that dims the app behind the panel; clicking it closes the panel, as in the design. */}
      <button
        type="button"
        aria-label="Close profile panel"
        className="profile-panel-overlay"
        onClick={closeHeaderPanel}
      />
      <HeaderPanel aria-label="User Profile" className="profile-panel">
        <div className="right-title-section">
          <h4>My profile</h4>
          <div className="right-title-buttons">
            <IconButton kind="ghost" label="Close" onClick={closeHeaderPanel} align="bottom">
              <Close size={24} />
            </IconButton>
          </div>
        </div>
        <HeaderPanelProfile />
      </HeaderPanel>
    </>
  );
};
