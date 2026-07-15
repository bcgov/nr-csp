package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.invoice.shared.rules.SourceDocuments;
import ca.bc.gov.nrs.csp.backend.invoice.submission.SubmissionValidationService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.IdentifierNormalizer;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterResolver;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.StructuralValidationService;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.LineItemRepository;
import ca.bc.gov.nrs.csp.backend.repository.LogSaleParticipantRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists an already-validated uploaded CSP submission: one {@code csp_submission}
 * row plus one {@code coastal_log_sale} (invoice) per parsed invoice, with its
 * line items, log sources and related-invoice links. Reuses the same repositories
 * as the manual invoice-entry path so the persisted shape is identical.
 *
 * <p>Callers must run business validation first and only invoke this on a fully
 * accepted submission (no rejected invoices). This service still re-parses the
 * bytes to obtain the JAXB tree and defends against a structurally invalid
 * document, but it does not re-run the business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CspSubmissionPersistenceService {

  private final SubmissionValidationService validationService;
  private final SubmitterResolver submitterResolver;
  private final IdentifierNormalizer identifierNormalizer;
  private final CspSubmissionRepository submissionRepo;
  private final InvoiceRepository invoiceRepo;
  private final LineItemRepository lineItemRepo;
  private final LogSaleParticipantRepository participantRepo;

  /**
   * Parses the submission and persists it in a single transaction.
   *
   * @return the new {@code csp_submission_id}.
   * @throws BadRequestException if the bytes are not structurally valid (a
   *     validated caller should never hit this).
   */
  @Transactional
  public Long persist(byte[] xml) {
    StructuralValidationService.ValidationOutcome outcome = validationService.parse(xml);
    if (!outcome.result().valid() || outcome.submission() == null) {
      throw new BadRequestException("Submission is not structurally valid and cannot be saved.");
    }

    CSPSubmissionType submission = (CSPSubmissionType) outcome.submission();
    CSPSubmitterType submitter = submission.getCSPSubmitter();
    String user = SecurityContextUtils.requireUsername();
    List<CSPInvoiceType> invoices = submission.getCSPInvoice();

    Long submissionId = submissionRepo.insertSubmission(
        submitter.getSubmissionClientNumber(),
        submitter.getSubmissionClientLocnCode(),
        ConstantsCode.SUBMSTATUS_INBOX,
        monthCompleteInd(submission.getMonthComplete()),
        invoices.size(),
        user);

    for (CSPInvoiceType invoice : invoices) {
      identifierNormalizer.normalizeInvoiceIdentifiers(invoice);
      SubmitterInfo party = submitterResolver.resolve(submission, invoice);
      persistInvoice(toDetails(invoice, party, user), toLineItems(invoice), submissionId, user);
    }

    log.info("Persisted uploaded submission id={} with {} invoice(s)", submissionId, invoices.size());
    return submissionId;
  }

  /** Inserts one invoice (+ line items, log sources, related invoices) under the submission. */
  private void persistInvoice(InvoiceDetails details, List<LineItem> lines, Long submissionId, String user) {
    Long buyerParticipantId = null;
    Long sellerParticipantId = null;
    // A manual (non-registered) other party needs a participant row; it lands in the
    // slot opposite the submitter (submitter=Seller → other party is the buyer).
    if (hasManualOtherParty(details)) {
      Long participantId = participantRepo.insert(
          details.otherClientName(), details.otherClientCity(), details.otherClientProvState(), user);
      if (ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(details.submittedBy())) {
        buyerParticipantId = participantId;
      } else {
        sellerParticipantId = participantId;
      }
    }

    Long invoiceId = invoiceRepo.insertInvoice(
        details, submissionId, ConstantsCode.INVENTRYSTATUS_DRAFT, buyerParticipantId, sellerParticipantId, user);

    for (LineItem line : lines) {
      lineItemRepo.insertLineItem(invoiceId, line, user);
    }

    invoiceRepo.replaceLogSources(invoiceId, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, details.boomNumbers(), user);
    invoiceRepo.replaceLogSources(invoiceId, ConstantsCode.LOGSOURCECODE_TIMERMARK, details.timberMarks(), user);
    invoiceRepo.replaceLogSources(invoiceId, ConstantsCode.LOGSOURCECODE_WEIGHSLIP, details.weightSlips(), user);
    invoiceRepo.replaceRelatedInvoices(invoiceId, ConstantsCode.INVRELATETYPE_REPLACE,
        details.replaceInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
    invoiceRepo.replaceRelatedInvoices(invoiceId, ConstantsCode.INVRELATETYPE_ADJUST,
        details.adjustInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
  }

  /** Maps a parsed invoice + resolved party view onto the persistence detail model. */
  private InvoiceDetails toDetails(CSPInvoiceType invoice, SubmitterInfo party, String user) {
    CSPInvoiceDetailsType d = invoice.getCSPInvoiceDetails();
    String submittedBy = party.submittedBy() == SubmitterInfo.SubmittedBy.SELLER
        ? ConstantsCode.INVOICE_SUBMITTEDBY_SELLER
        : ConstantsCode.INVOICE_SUBMITTEDBY_BUYER;

    return new InvoiceDetails(
        null,
        invoice.getInvoiceNumber(),
        toLocalDate(invoice.getInvoiceDate()),
        null,
        invoice.getInvoiceType(),
        d == null ? null : d.getMaturity(),
        d == null ? null : d.getLocationFOB(),
        d == null ? null : d.getPrimarySortCode(),
        d == null ? null : d.getTotalAmount(),
        d == null ? null : d.getTotalPieces(),
        d == null ? null : d.getTotalVolume(),
        party.submitterClientNumber(),
        party.submitterLocnCode(),
        submittedBy,
        invoice.getSellerClientNumber(),
        invoice.getSellerClientLocnCode(),
        party.otherClientNumber(),
        party.otherLocnCode(),
        party.otherPartyName(),
        party.otherPartyCity(),
        party.otherPartyProvState(),
        sourceDocs(d == null ? null : d.getBoomNumbers()),
        sourceDocs(d == null ? null : d.getTimberMarks()),
        sourceDocs(d == null ? null : d.getWeighSlipNumbers()),
        invoice.getReplacesInvoiceNumbers(),
        invoice.getAdjustsInvoiceNumbers(),
        null,
        d == null ? null : d.getSubmitterNotes(),
        user);
  }

  private List<LineItem> toLineItems(CSPInvoiceType invoice) {
    List<LineItem> lines = new ArrayList<>();
    for (CSPLineItemType li : invoice.getCSPLineItem()) {
      lines.add(new LineItem(
          null,
          null,
          li.getSecondarySortCode(),
          li.getClientSecondarySortCode(),
          li.getSpecies(),
          null,
          li.getGrade(),
          li.getNumberOfPieces(),
          li.getPrice(),
          li.getVolume(),
          null,
          null));
    }
    return lines;
  }

  private boolean hasManualOtherParty(InvoiceDetails details) {
    return isBlank(details.otherClientNum())
        && (!isBlank(details.otherClientName())
        || !isBlank(details.otherClientCity())
        || !isBlank(details.otherClientProvState()));
  }

  /** Splits an ESF CSV source-document string into a de-duplicated token list. */
  private static List<String> sourceDocs(String csv) {
    if (isBlank(csv)) return List.of();
    return SourceDocuments.dedup(new ArrayList<>(List.of(csv.split(","))));
  }

  private static String monthCompleteInd(String monthComplete) {
    return "Y".equalsIgnoreCase(monthComplete) ? "Y" : "N";
  }

  private static LocalDate toLocalDate(XMLGregorianCalendar cal) {
    return cal == null ? null : LocalDate.of(cal.getYear(), cal.getMonth(), cal.getDay());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
