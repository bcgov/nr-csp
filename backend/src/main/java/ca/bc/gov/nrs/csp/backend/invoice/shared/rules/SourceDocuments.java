package ca.bc.gov.nrs.csp.backend.invoice.shared.rules;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure source-document helpers shared by both channels (catalogue I31). The
 * electronic path de-duplicates its CSV strings in
 * {@code InvoiceSourceDocumentRules#deduplicateCsv}; the manual path
 * de-duplicates its lists here (called from {@code InvoiceMapper} at the
 * domain-model boundary, so the validator's count checks and the persisted
 * log sources both see the de-duplicated values). Mirrors the legacy
 * {@code ignoreCSVForDuplicates}.
 */
public final class SourceDocuments {

  private SourceDocuments() {}

  /**
   * De-duplicates a source-document list (Boom Numbers / Timber Marks / Weigh
   * Slips): tokens are trimmed, blanks dropped, and duplicates removed keeping
   * first-seen order. A null list maps to an empty list.
   */
  public static List<String> dedup(List<String> values) {
    if (values == null) {
      return List.of();
    }
    Set<String> unique = new LinkedHashSet<>();
    for (String value : values) {
      if (value == null) {
        continue;
      }
      String trimmed = value.trim();
      if (!trimmed.isEmpty()) {
        unique.add(trimmed);
      }
    }
    return List.copyOf(unique);
  }
}
