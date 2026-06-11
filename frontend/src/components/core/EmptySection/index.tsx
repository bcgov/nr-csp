interface Props {
  title?: string;
  description?: string;
}

export function EmptySection({ title = 'No data', description }: Props) {
  return (
    <div className="empty-section-container">
      <p>{title}</p>
      {description && <p>{description}</p>}
    </div>
  );
}
