import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';

import { queryClient } from '@/config/react-query/config';
import { AuthProvider } from '@/context/auth/AuthProvider';
import { NotificationProvider } from '@/context/notification/NotificationProvider';
import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';
import { ThemeProvider } from '@/context/theme/ThemeProvider';
import Layout from '@/components/Layout';
import { LoginPage } from '@/pages/Login';
import { FlatPriceConversionPage } from '@/pages/FlatPriceConversion';
import { InvoicePage } from '@/pages/Invoice';
import { SearchPage } from '@/pages/Search';
import { NotFoundPage } from '@/pages/NotFound';
import { SortCodePage } from '@/pages/SortCode';
import { R06InvoicePrintOutPage } from '@/pages/R06InvoicePrintOut';
import { R07ReconciliationPage } from '@/pages/R07Reconciliation';
import { R08InvoiceAuditPage } from '@/pages/R08InvoiceAudit';
import { R10LogSalesSpeciesPage } from '@/pages/R10LogSalesSpecies';
import { R11AverageMarketValuesPage } from '@/pages/R11AverageMarketValues';
import { R12CfpaExtractPage } from '@/pages/R12CfpaExtract';
import { R13AdHocReportingPage } from '@/pages/R13AdHocReporting';
import { ProtectedRoute } from '@/routes/ProtectedRoute';
import { ROUTES } from '@/routes/routePaths';

const ReactQueryDevtools = import.meta.env.DEV
  ? lazy(() =>
      import('@tanstack/react-query-devtools').then((module) => ({
        default: module.ReactQueryDevtools,
      })),
    )
  : () => null;

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <NotificationProvider>
          <AuthProvider>
            <PageTitleProvider>
              <BrowserRouter>
                <Routes>
                  {/* Login — accessible without authentication */}
                  <Route path={ROUTES.LOGIN} element={<LoginPage />} />

                  {/* All other routes share the shell layout and require authentication */}
                  <Route element={<Layout />}>
                    <Route path={ROUTES.LANDING} element={<Navigate to={ROUTES.SEARCH} replace />} />
                    <Route
                      path={ROUTES.SEARCH}
                      element={
                        <ProtectedRoute>
                          <SearchPage />
                        </ProtectedRoute>
                      }
                    />
                    {/* New invoice (no id) and existing invoice (/:id) both
                        render InvoicePage; it branches on the presence of the
                        route param. */}
                    <Route
                      path={ROUTES.INVOICE}
                      element={
                        <ProtectedRoute>
                          <InvoicePage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={`${ROUTES.INVOICE}/:id`}
                      element={
                        <ProtectedRoute>
                          <InvoicePage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.SORT_CODE}
                      element={
                        <ProtectedRoute>
                          <SortCodePage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.FLAT_PRICE_CONVERSION}
                      element={
                        <ProtectedRoute>
                          <FlatPriceConversionPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R06_INVOICE_PRINT_OUT}
                      element={
                        <ProtectedRoute>
                          <R06InvoicePrintOutPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R07_RECONCILIATION}
                      element={
                        <ProtectedRoute>
                          <R07ReconciliationPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R08_INVOICE_AUDIT}
                      element={
                        <ProtectedRoute>
                          <R08InvoiceAuditPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R10_LOG_SALES_SPECIES}
                      element={
                        <ProtectedRoute>
                          <R10LogSalesSpeciesPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R11_AMV}
                      element={
                        <ProtectedRoute>
                          <R11AverageMarketValuesPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R12_CFPA_EXTRACT}
                      element={
                        <ProtectedRoute>
                          <R12CfpaExtractPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path={ROUTES.R13_AD_HOC}
                      element={
                        <ProtectedRoute>
                          <R13AdHocReportingPage />
                        </ProtectedRoute>
                      }
                    />
                    <Route path={ROUTES.NOT_FOUND} element={<NotFoundPage />} />
                  </Route>
                </Routes>
              </BrowserRouter>
            </PageTitleProvider>
          </AuthProvider>
        </NotificationProvider>
      </ThemeProvider>
      {import.meta.env.DEV && (
        <Suspense fallback={null}>
          <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-left" />
        </Suspense>
      )}
    </QueryClientProvider>
  );
}
