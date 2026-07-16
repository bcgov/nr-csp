package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.IdentifierNormalizer;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterResolver;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.SellerSubmissionType;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.LineItemRepository;
import ca.bc.gov.nrs.csp.backend.repository.LogSaleParticipantRepository;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CspSubmissionPersistenceServiceTest {

  private static final String USER = "tester";

  @Mock CspSubmissionRepository submissionRepo;
  @Mock InvoiceRepository invoiceRepo;
  @Mock LineItemRepository lineItemRepo;
  @Mock LogSaleParticipantRepository participantRepo;

  // Real, dependency-free collaborators so party resolution / normalisation run for real.
  SubmitterResolver submitterResolver = new SubmitterResolver();
  IdentifierNormalizer identifierNormalizer = new IdentifierNormalizer();

  CspSubmissionPersistenceService service;

  @BeforeEach
  void setUp() {
    service = new CspSubmissionPersistenceService(
        submitterResolver, identifierNormalizer,
        submissionRepo, invoiceRepo, lineItemRepo, participantRepo);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(USER, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persist_insertsOneSubmissionAndMapsInvoiceAndLine() throws Exception {
    CSPSubmissionType submission = sampleSubmission();
    given(submissionRepo.insertSubmission(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
        .willReturn(555L);
    given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(900L);

    Long submissionId = service.persist(submission);

    assertThat(submissionId).isEqualTo(555L);

    // One submission, keyed on the submission-level submitter, month-complete + count from the XML.
    verify(submissionRepo).insertSubmission(
        "00126920", "00", ConstantsCode.SUBMSTATUS_INBOX, "Y", 1, USER);

    // One invoice, saved under that submission as DRAFT, with the parsed fields mapped through.
    ArgumentCaptor<InvoiceDetails> detailsCaptor = ArgumentCaptor.forClass(InvoiceDetails.class);
    verify(invoiceRepo).insertInvoice(
        detailsCaptor.capture(), eq(555L), eq(ConstantsCode.INVENTRYSTATUS_DRAFT), any(), any(), eq(USER));
    InvoiceDetails details = detailsCaptor.getValue();
    assertThat(details.invNumber()).isEqualTo("BAH101"); // normalised to upper-case
    assertThat(details.invoiceDate()).isEqualTo(LocalDate.of(2026, Month.MAY, 27));
    assertThat(details.invType()).isEqualTo("SAL");
    assertThat(details.maturity()).isEqualTo("O");
    assertThat(details.fobCode()).isEqualTo("TEST");
    assertThat(details.totalAmt()).isEqualByComparingTo("1.00");
    assertThat(details.totalPieces()).isEqualTo(1);
    // Seller submission → submitter is the seller, other party is the buyer.
    assertThat(details.submittedBy()).isEqualTo(ConstantsCode.INVOICE_SUBMITTEDBY_SELLER);
    assertThat(details.submitterClientNum()).isEqualTo("00126920");
    assertThat(details.otherClientNum()).isEqualTo("00123946");

    // One line item mapped through.
    ArgumentCaptor<LineItem> lineCaptor = ArgumentCaptor.forClass(LineItem.class);
    verify(lineItemRepo).insertLineItem(eq(900L), lineCaptor.capture(), eq(USER));
    LineItem line = lineCaptor.getValue();
    assertThat(line.species()).isEqualTo("FI");
    assertThat(line.grade()).isEqualTo("J");
    assertThat(line.numOfPieces()).isEqualTo(1);
    assertThat(line.price()).isEqualByComparingTo("1.00");
  }

  private static CSPSubmissionType sampleSubmission() throws Exception {
    CSPSubmissionType submission = new CSPSubmissionType();
    submission.setMonthComplete("Y");

    CSPSubmitterType submitter = new CSPSubmitterType();
    submitter.setSellerSubmission(SellerSubmissionType.Y);
    submitter.setSubmissionClientNumber("00126920");
    submitter.setSubmissionClientLocnCode("00");
    submission.setCSPSubmitter(submitter);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("bah101"); // lower-case on purpose; normaliser upper-cases it
    invoice.setInvoiceDate(DatatypeFactory.newInstance()
        .newXMLGregorianCalendarDate(2026, 5, 27, DatatypeConstants.FIELD_UNDEFINED));
    invoice.setInvoiceType("SAL");
    invoice.setSellerClientNumber("00126920");
    invoice.setSellerClientLocnCode("00");
    invoice.setBuyerClientNumber("00123946");
    invoice.setBuyerClientLocnCode("00");

    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setMaturity("O");
    details.setLocationFOB("TEST");
    details.setTotalAmount(new BigDecimal("1.00"));
    details.setTotalVolume(new BigDecimal("1.000"));
    details.setTotalPieces(1);
    invoice.setCSPInvoiceDetails(details);

    CSPLineItemType line = new CSPLineItemType();
    line.setSecondarySortCode("G");
    line.setClientSecondarySortCode("G");
    line.setNumberOfPieces(1);
    line.setSpecies("FI");
    line.setGrade("J");
    line.setVolume(new BigDecimal("1.000"));
    line.setPrice(new BigDecimal("1.00"));
    invoice.getCSPLineItem().add(line);

    submission.getCSPInvoice().add(invoice);
    return submission;
  }
}
