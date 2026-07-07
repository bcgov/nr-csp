package ca.bc.gov.nrs.csp.backend.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRepositoryTest {

    // ---------------------------------------------------------------
    // toInvoiceNumberPattern
    // ---------------------------------------------------------------

    @Test
    void toInvoiceNumberPattern_plainTerm_becomesContainsMatch() {
        assertThat(SearchRepository.toInvoiceNumberPattern("500")).isEqualTo("%500%");
    }

    @Test
    void toInvoiceNumberPattern_starBecomesPercent() {
        assertThat(SearchRepository.toInvoiceNumberPattern("INV-2024-*")).isEqualTo("INV-2024-%");
    }

    @Test
    void toInvoiceNumberPattern_percentPassesThroughAsWildcard() {
        assertThat(SearchRepository.toInvoiceNumberPattern("INV-2024-%")).isEqualTo("INV-2024-%");
    }

    @Test
    void toInvoiceNumberPattern_questionMarkBecomesUnderscore() {
        assertThat(SearchRepository.toInvoiceNumberPattern("INV-?-23")).isEqualTo("INV-_-23");
    }

    @Test
    void toInvoiceNumberPattern_mixedWildcards() {
        assertThat(SearchRepository.toInvoiceNumberPattern("*-2024-?")).isEqualTo("%-2024-_");
    }

    @Test
    void toInvoiceNumberPattern_starAndPercentBothBecomeWildcards() {
        assertThat(SearchRepository.toInvoiceNumberPattern("50%*")).isEqualTo("50%%");
    }

    @Test
    void toInvoiceNumberPattern_escapesUnderscoreLiteralInPlainTerm() {
        // A literal '_' (not the '?' wildcard) must be escaped so it matches literally.
        assertThat(SearchRepository.toInvoiceNumberPattern("ab_cd")).isEqualTo("%ab\\_cd%");
    }

    @Test
    void toInvoiceNumberPattern_escapesUnderscoreEvenInPatternMode() {
        // '_' stays escaped even when the term is a pattern (a wildcard is present).
        assertThat(SearchRepository.toInvoiceNumberPattern("a_b*")).isEqualTo("a\\_b%");
    }

    @Test
    void toInvoiceNumberPattern_escapesBackslash() {
        assertThat(SearchRepository.toInvoiceNumberPattern("a\\b")).isEqualTo("%a\\\\b%");
    }
}
