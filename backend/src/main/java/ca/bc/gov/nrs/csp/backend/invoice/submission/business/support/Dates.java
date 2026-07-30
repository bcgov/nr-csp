package ca.bc.gov.nrs.csp.backend.invoice.submission.business.support;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

/**
 * Small conversion helper so rules work with {@link LocalDate} instead of the
 * JAXB {@link XMLGregorianCalendar} on the generated types.
 */
public final class Dates {

  private Dates() {}

  /** Convert an {@code xs:date} XMLGregorianCalendar to LocalDate; null-safe. */
  public static LocalDate toLocalDate(XMLGregorianCalendar cal) {
    return cal == null ? null : LocalDate.of(cal.getYear(), cal.getMonth(), cal.getDay());
  }
}
