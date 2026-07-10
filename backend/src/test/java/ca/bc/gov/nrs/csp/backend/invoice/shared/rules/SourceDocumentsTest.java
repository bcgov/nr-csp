package ca.bc.gov.nrs.csp.backend.invoice.shared.rules;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The authoritative matrix for the I31 list de-duplication (moved from
 * {@code InvoiceMapperTest} per refactor doc §12.4 — the mapper keeps a thin
 * call-through test). Semantics mirror the electronic path's
 * {@code InvoiceSourceDocumentRules#deduplicateCsv} and the legacy
 * {@code ignoreCSVForDuplicates}.
 */
class SourceDocumentsTest {

  @Test
  void dedups_keeping_first_seen_order() {
    assertThat(SourceDocuments.dedup(List.of("B1", "B2", "B1", "B3", "B2")))
        .containsExactly("B1", "B2", "B3");
  }

  @Test
  void trims_tokens_and_dedups_on_trimmed_value() {
    assertThat(SourceDocuments.dedup(List.of(" B1 ", "B1", "B2 ")))
        .containsExactly("B1", "B2");
  }

  @Test
  void drops_blank_and_null_tokens() {
    assertThat(SourceDocuments.dedup(Arrays.asList("B1", "  ", null, "B2")))
        .containsExactly("B1", "B2");
  }

  @Test
  void null_list_maps_to_empty_list() {
    assertThat(SourceDocuments.dedup(null)).isEmpty();
  }

  @Test
  void distinct_values_pass_through_unchanged() {
    assertThat(SourceDocuments.dedup(List.of("B1", "B2", "B3")))
        .containsExactly("B1", "B2", "B3");
  }
}
