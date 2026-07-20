package ca.bc.gov.nrs.csp.backend.invoice.submission.structural;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The handful of values that make the otherwise application-agnostic
 * structural submission-validation pipeline specific to CSP. These are the
 * parameters the shared-package design calls out as the only things a
 * consuming app must supply: the classpath location of its XSD bundle,
 * the JAXB context package generated from that bundle, and the
 * envelope/body element coordinates used to strip the ESF wrapper.
 *
 * <p>Defaults are wired for CSP so the pipeline works with zero config;
 * every value is overridable under {@code csp.submission.validation.*}
 * in application.yml. Keeping them here (rather than hardcoded in the
 * pipeline classes, as nr-fspts does) is what lets this code lift into
 * a reusable artifact later without edits.
 */
@Component
@ConfigurationProperties(prefix = "csp.submission.validation")
@Data
public class SubmissionValidationProperties {

  /**
   * Classpath directory that holds the XSD bundle. Used by the schema
   * resolver to locate {@code xs:import}ed siblings. Must end with '/'.
   */
  private String schemaClasspathRoot = "schemas/csp/";

  /** Classpath path to the entry-point XSD compiled into the schema. */
  private String entrySchema = "schemas/csp/mof-csp.xsd";

  /** Package of the xjc-generated JAXB types ({@code JAXBContext.newInstance}). */
  private String jaxbContextPath = "ca.bc.gov.nrs.csp.backend.invoice.submission.generated";

  /** Namespace of the ESF envelope root element. */
  private String envelopeNamespace = "http://www.for.gov.bc.ca/schema/esf";

  /** Local name of the ESF envelope root element. */
  private String envelopeRoot = "ESFSubmission";

  /** Local name of the ESF element wrapping the application body. */
  private String envelopeContentElement = "submissionContent";

  /** Local name of the ESF element carrying the submitter email address. */
  private String envelopeEmailElement = "emailAddress";

  /** Local name of the ESF element carrying the submitter telephone number. */
  private String envelopeTelephoneElement = "telephoneNumber";

  /** Namespace of the bare application submission body. */
  private String bodyNamespace = "http://www.for.gov.bc.ca/schema/csp";

  /** Local name of the bare application submission body root element. */
  private String bodyRoot = "CSPSubmission";
}
