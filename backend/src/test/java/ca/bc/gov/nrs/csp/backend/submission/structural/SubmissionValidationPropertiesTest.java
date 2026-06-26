package ca.bc.gov.nrs.csp.backend.submission.structural;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the parameterization holder directly — its CSP defaults plus the
 * Lombok {@code @Data} accessors/value semantics (setters, equals, hashCode,
 * toString) that the pipeline tests don't touch (they rely on defaults only).
 */
class SubmissionValidationPropertiesTest {

  @Test
  void defaults_are_csp_specific() {
    SubmissionValidationProperties p = new SubmissionValidationProperties();

    assertThat(p.getSchemaClasspathRoot()).isEqualTo("schemas/csp/");
    assertThat(p.getEntrySchema()).isEqualTo("schemas/csp/mof-csp.xsd");
    assertThat(p.getJaxbContextPath()).isEqualTo("ca.bc.gov.nrs.csp.backend.submission.generated");
    assertThat(p.getEnvelopeNamespace()).isEqualTo("http://www.for.gov.bc.ca/schema/esf");
    assertThat(p.getEnvelopeRoot()).isEqualTo("ESFSubmission");
    assertThat(p.getEnvelopeContentElement()).isEqualTo("submissionContent");
    assertThat(p.getBodyNamespace()).isEqualTo("http://www.for.gov.bc.ca/schema/csp");
    assertThat(p.getBodyRoot()).isEqualTo("CSPSubmission");
  }

  @Test
  void setters_round_trip_through_getters() {
    SubmissionValidationProperties p = new SubmissionValidationProperties();
    p.setSchemaClasspathRoot("schemas/x/");
    p.setEntrySchema("schemas/x/entry.xsd");
    p.setJaxbContextPath("com.example.generated");
    p.setEnvelopeNamespace("urn:env");
    p.setEnvelopeRoot("EnvRoot");
    p.setEnvelopeContentElement("content");
    p.setBodyNamespace("urn:body");
    p.setBodyRoot("BodyRoot");

    assertThat(p.getSchemaClasspathRoot()).isEqualTo("schemas/x/");
    assertThat(p.getEntrySchema()).isEqualTo("schemas/x/entry.xsd");
    assertThat(p.getJaxbContextPath()).isEqualTo("com.example.generated");
    assertThat(p.getEnvelopeNamespace()).isEqualTo("urn:env");
    assertThat(p.getEnvelopeRoot()).isEqualTo("EnvRoot");
    assertThat(p.getEnvelopeContentElement()).isEqualTo("content");
    assertThat(p.getBodyNamespace()).isEqualTo("urn:body");
    assertThat(p.getBodyRoot()).isEqualTo("BodyRoot");
  }

  @Test
  void value_semantics_equals_hashCode_toString() {
    SubmissionValidationProperties a = configured();
    SubmissionValidationProperties b = configured();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a.toString()).contains("BodyRoot");
    assertThat(a).isNotEqualTo(new SubmissionValidationProperties());
  }

  private static SubmissionValidationProperties configured() {
    SubmissionValidationProperties p = new SubmissionValidationProperties();
    p.setSchemaClasspathRoot("schemas/x/");
    p.setEntrySchema("schemas/x/entry.xsd");
    p.setJaxbContextPath("com.example.generated");
    p.setEnvelopeNamespace("urn:env");
    p.setEnvelopeRoot("EnvRoot");
    p.setEnvelopeContentElement("content");
    p.setBodyNamespace("urn:body");
    p.setBodyRoot("BodyRoot");
    return p;
  }
}
