import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';

export interface LookupItemResponse {
  code: string;
  description: string;
}

const fetchLookup = (path: string): Promise<LookupItemResponse[]> =>
  apiClient.get<LookupItemResponse[]>(path).then(({ data }) => data);

export const useInvoiceStatusesQuery = () =>
  useQuery({
    queryKey: ['lookup', 'status'],
    queryFn: () => fetchLookup('/lookup/status'),
  });

export const useInvoiceTypesQuery = () =>
  useQuery({
    queryKey: ['lookup', 'type'],
    queryFn: () => fetchLookup('/lookup/type'),
  });

export const useMaturityCodesQuery = () =>
  useQuery({
    queryKey: ['lookup', 'maturity'],
    queryFn: () => fetchLookup('/lookup/maturity'),
  });

export const useMaturityCodesNoCantsQuery = () =>
  useQuery({
    queryKey: ['lookup', 'maturity'],
    queryFn: () => fetchLookup('/lookup/maturity'),
    select: (data) => data.filter((item) => item.description !== 'Cants / Export' && item.description !== 'Export'),
  });

export const useMaturityCodesWithCantsQuery = () =>
  useQuery({
    queryKey: ['lookup', 'maturity'],
    queryFn: () => fetchLookup('/lookup/maturity'),
    select: (data) =>
      data
        .filter((item) => item.description !== 'Export')
        .map((item) => (item.description === 'Cants / Export' ? { ...item, description: 'Cants' } : item)),
  });

export const useSortCodesLookupQuery = () =>
  useQuery({
    queryKey: ['lookup', 'sort-code'],
    queryFn: () => fetchLookup('/lookup/sort-code'),
  });

export const useSpeciesLookupQuery = () =>
  useQuery({
    queryKey: ['lookup', 'species'],
    queryFn: () => fetchLookup('/lookup/species'),
  });

export const useGradeLookupQuery = () =>
  useQuery({
    queryKey: ['lookup', 'grade'],
    queryFn: () => fetchLookup('/lookup/grade'),
  });

export const useGradesBySpeciesLookupQuery = (species: string | null) =>
  useQuery({
    queryKey: ['lookup', 'grade-by-species', species],
    queryFn: () => fetchLookup(`/lookup/grade-by-species/${species!}`),
    enabled: !!species,
  });

export const useSubmissionStatusesQuery = () =>
  useQuery({
    queryKey: ['lookup', 'submission-status'],
    queryFn: () => fetchLookup('/lookup/submission-status'),
  });

/** A single (species, grade) pair from THE.csp_species_grade_xref. */
export interface SpeciesGradeCombinationResponse {
  species: string;
  grade: string;
}

/**
 * Loads the full active (species, grade) combination list once per session
 * (Infinity staleTime — the underlying xref only changes via admin updates,
 * so we cache aggressively and rely on local filtering for the dropdowns).
 */
export const useSpeciesGradeCombosQuery = () =>
  useQuery({
    queryKey: ['lookup', 'species-grade-combinations'],
    queryFn: () =>
      apiClient.get<SpeciesGradeCombinationResponse[]>('/lookup/species-grade-combinations').then(({ data }) => data),
    staleTime: Infinity,
  });
