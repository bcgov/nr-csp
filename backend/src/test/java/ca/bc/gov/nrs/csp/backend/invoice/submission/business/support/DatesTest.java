package ca.bc.gov.nrs.csp.backend.invoice.submission.business.support;

import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class DatesTest {

  @Test
  void toLocalDate_converts_an_xs_date() throws Exception {
    XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        2024, 6, 15, DatatypeConstants.FIELD_UNDEFINED);

    assertThat(Dates.toLocalDate(cal)).isEqualTo(LocalDate.of(2024, Month.JUNE, 15));
  }

  @Test
  void toLocalDate_returns_null_when_calendar_is_null() {
    assertThat(Dates.toLocalDate(null)).isNull();
  }
}
