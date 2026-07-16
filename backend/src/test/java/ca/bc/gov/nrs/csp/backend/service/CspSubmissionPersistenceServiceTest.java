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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

  @Test
  void buyerSubmission_withManualSeller_insertsSellerParticipantSourceDocsAndMonthN() throws Exception {
    // Buyer submission: submitter is the buyer (registered); the seller is the
    // "other party" and unregistered (manual), so it needs a participant row in
    // the seller slot. monthComplete is not "Y" → the indicator is "N".
    CSPSubmissionType submission = submission("N", false);
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV1");
    invoice.setInvoiceDate(date(2026, 6, 1));
    invoice.setInvoiceType("SAL");
    invoice.setBuyerClientNumber("00126920");
    invoice.setBuyerClientLocnCode("00");
    invoice.setOtherPartyName("Acme Logs");
    invoice.setOtherPartyCity("Nanaimo");
    invoice.setOtherPartyProvState("BC");
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalAmount(new BigDecimal("5.00"));
    details.setBoomNumbers("B1,B2,B1"); // duplicate is de-duped
    details.setTimberMarks("T1");
    details.setWeighSlipNumbers("W1,W2");
    invoice.setCSPInvoiceDetails(details);
    submission.getCSPInvoice().add(invoice);

    given(participantRepo.insert(any(), any(), any(), any())).willReturn(777L);
    given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(901L);

    service.persist(submission);

    verify(submissionRepo).insertSubmission(
        "00126920", "00", ConstantsCode.SUBMSTATUS_INBOX, "N", 1, USER);

    // Manual seller party lands in the seller slot; the buyer slot stays null.
    ArgumentCaptor<InvoiceDetails> detailsCaptor = ArgumentCaptor.forClass(InvoiceDetails.class);
    verify(invoiceRepo).insertInvoice(detailsCaptor.capture(), any(),
        eq(ConstantsCode.INVENTRYSTATUS_DRAFT), isNull(), eq(777L), eq(USER));
    assertThat(detailsCaptor.getValue().submittedBy())
        .isEqualTo(ConstantsCode.INVOICE_SUBMITTEDBY_BUYER);

    // Source-document CSV strings are split and de-duplicated per type.
    verify(invoiceRepo).replaceLogSources(any(), eq(ConstantsCode.LOGSOURCECODE_BOOMNUMBER),
        eq(List.of("B1", "B2")), eq(USER));
    verify(invoiceRepo).replaceLogSources(any(), eq(ConstantsCode.LOGSOURCECODE_WEIGHSLIP),
        eq(List.of("W1", "W2")), eq(USER));
  }

  @Test
  void sellerSubmission_withManualBuyer_nullDetailsAndDate_insertsBuyerParticipant() throws Exception {
    // Seller submission with a manual (unregistered) buyer and no invoice details
    // or date: the participant lands in the buyer slot and the mapped detail
    // fields sourced from CSPInvoiceDetails / the date are all null/empty.
    CSPSubmissionType submission = submission("Y", true);
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV2");
    invoice.setSellerClientNumber("00126920");
    invoice.setSellerClientLocnCode("00");
    invoice.setOtherPartyName("Manual Buyer");
    submission.getCSPInvoice().add(invoice);

    given(participantRepo.insert(any(), any(), any(), any())).willReturn(555L);
    given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(902L);

    service.persist(submission);

    ArgumentCaptor<InvoiceDetails> detailsCaptor = ArgumentCaptor.forClass(InvoiceDetails.class);
    verify(invoiceRepo).insertInvoice(detailsCaptor.capture(), any(),
        eq(ConstantsCode.INVENTRYSTATUS_DRAFT), eq(555L), isNull(), eq(USER));
    InvoiceDetails details = detailsCaptor.getValue();
    assertThat(details.submittedBy()).isEqualTo(ConstantsCode.INVOICE_SUBMITTEDBY_SELLER);
    assertThat(details.invoiceDate()).isNull();
    assertThat(details.maturity()).isNull();
    assertThat(details.totalAmt()).isNull();
    assertThat(details.boomNumbers()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource
  void hasManualOtherParty_evaluatesEachPartyField(
      String name, String city, String prov, boolean expectParticipant) throws Exception {
    // A manual other party is one with no client number but at least one of
    // name/city/provState — exercising each arm of the short-circuit condition.
    CSPSubmissionType submission = submission("Y", true);
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV3");
    invoice.setSellerClientNumber("00126920");
    invoice.setOtherPartyName(name);
    invoice.setOtherPartyCity(city);
    invoice.setOtherPartyProvState(prov);
    submission.getCSPInvoice().add(invoice);

    service.persist(submission);

    verify(participantRepo, expectParticipant ? times(1) : never())
        .insert(any(), any(), any(), any());
  }

  @Test
  void manualOtherParty_treatsBlankClientNumberAsUnregistered() throws Exception {
    // A present-but-blank other-party client number is treated as unregistered,
    // so a manual party (name present) still triggers a participant insert.
    CSPSubmissionType submission = submission("Y", true);
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV4");
    invoice.setSellerClientNumber("00126920");
    invoice.setBuyerClientNumber("   ");
    invoice.setOtherPartyName("Manual Buyer");
    submission.getCSPInvoice().add(invoice);

    service.persist(submission);

    verify(participantRepo).insert(any(), any(), any(), any());
  }

  static Stream<Arguments> hasManualOtherParty_evaluatesEachPartyField() {
    return Stream.of(
        Arguments.of("Acme", null, null, true),
        Arguments.of(null, "Victoria", null, true),
        Arguments.of(null, null, "BC", true),
        Arguments.of(null, null, null, false));
  }

  private static CSPSubmissionType submission(String monthComplete, boolean sellerSubmission) {
    CSPSubmissionType submission = new CSPSubmissionType();
    submission.setMonthComplete(monthComplete);
    CSPSubmitterType submitter = new CSPSubmitterType();
    submitter.setSellerSubmission(sellerSubmission ? SellerSubmissionType.Y : SellerSubmissionType.N);
    submitter.setSubmissionClientNumber("00126920");
    submitter.setSubmissionClientLocnCode("00");
    submission.setCSPSubmitter(submitter);
    return submission;
  }

  private static XMLGregorianCalendar date(int year, int month, int day) throws Exception {
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendarDate(year, month, day, DatatypeConstants.FIELD_UNDEFINED);
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
