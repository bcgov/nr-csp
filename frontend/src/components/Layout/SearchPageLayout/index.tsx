import { Search } from '@carbon/icons-react';
import { Button } from '@carbon/react';

import PageTitle from '@/components/core/PageTitle';

import type { ReactNode } from 'react';

import './index.scss';

interface SearchPageLayoutProps {
  title: string;
  subtitle: string;
  filters: ReactNode;
  onSearch: () => void;
  onClear: () => void;
  searchDisabled: boolean;
  error: string | null;
  children: ReactNode;
}

export default function SearchPageLayout({
  title,
  subtitle,
  filters,
  onSearch,
  onClear,
  searchDisabled,
  error,
  children,
}: SearchPageLayoutProps) {
  return (
    <div className="search-page">
      <PageTitle title={title} subtitle={subtitle} />
      <form
        role="search"
        aria-label={title}
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          // CSP date fields validate ISO dates themselves. Carbon's native pattern expects MM/DD/YYYY.
          const invalidField = event.currentTarget.querySelector<HTMLInputElement>('input[aria-invalid="true"]');
          if (invalidField) invalidField.focus();
          else if (!searchDisabled) onSearch();
        }}
      >
        <div className="search-page__filters">{filters}</div>
        <div className="search-page__actions">
          <Button kind="tertiary" size="md" type="button" onClick={onClear}>
            Clear filters
          </Button>
          <Button kind="primary" size="md" type="submit" renderIcon={Search} disabled={searchDisabled}>
            Search
          </Button>
        </div>
      </form>
      {error && (
        <p role="alert" className="search-page__error">
          {error}
        </p>
      )}
      <section className="search-page__results" aria-label={`${title} results`}>
        {children}
      </section>
    </div>
  );
}
