import { Tag } from '@carbon/react';
import { type FC } from 'react';

import './index.scss';

export type SubmissionStatusTagProps = {
  status: string;
};

const COMPLETED_STATUSES = new Set(['COM', 'Complete']);

type TagType =
  | 'red'
  | 'magenta'
  | 'purple'
  | 'blue'
  | 'cyan'
  | 'teal'
  | 'green'
  | 'gray'
  | 'cool-gray'
  | 'warm-gray'
  | 'high-contrast'
  | 'outline';

const STATUS_COLOUR_MAP: Record<string, TagType> = {
  // By code
  COM: 'green',
  INB: 'blue',
  LOB: 'cyan',
  REJ: 'red',
  // By description (returned by backend)
  Complete: 'green',
  Inbox: 'blue',
  Lobby: 'cyan',
  Rejected: 'red',
};

const SubmissionStatusTag: FC<SubmissionStatusTagProps> = ({ status }) => (
  <Tag
    type={STATUS_COLOUR_MAP[status] ?? 'gray'}
    size="sm"
    className={`submission-status-tag${COMPLETED_STATUSES.has(status) ? ' submission-status-tag--completed' : ''}`}
  >
    {status}
  </Tag>
);

export default SubmissionStatusTag;
