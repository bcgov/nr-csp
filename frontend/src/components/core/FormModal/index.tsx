import { Button, ComposedModal, Loading, ModalBody, ModalFooter, ModalHeader } from '@carbon/react';
import type { CarbonIconType } from '@carbon/icons-react';
import type { FC, ReactNode } from 'react';

import './index.scss';

interface FormModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: () => void;
  title?: string;
  submitLabel?: string;
  cancelLabel?: string;
  submitIcon?: CarbonIconType;
  submitDisabled?: boolean;
  /** When true the submit button shows an inline spinner and both buttons are disabled. */
  submitLoading?: boolean;
  danger?: boolean;
  size?: 'xs' | 'sm' | 'md' | 'lg';
  children: ReactNode;
}

const FormModal: FC<FormModalProps> = ({
  open,
  onClose,
  onSubmit,
  title,
  submitLabel = 'Save',
  cancelLabel = 'Cancel',
  submitIcon,
  submitDisabled,
  submitLoading,
  danger,
  size,
  children,
}: FormModalProps): React.ReactElement => {
  return (
    <ComposedModal open={open} onClose={onClose} className="form-modal" size={size}>
      <ModalHeader title={title} />
      <ModalBody>{children}</ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose} disabled={submitLoading}>
          {cancelLabel}
        </Button>
        <Button
          kind={danger ? 'danger' : 'primary'}
          renderIcon={submitLoading ? undefined : submitIcon}
          iconDescription={submitLabel}
          disabled={submitDisabled || submitLoading}
          onClick={onSubmit}
        >
          {submitLabel}
          {submitLoading ? (
            <Loading small withOverlay={false} description={submitLabel} className="form-modal__submit-spinner" />
          ) : null}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default FormModal;
