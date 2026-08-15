package ca.bc.gov.nrs.csp.backend.service.reporting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubreportInjectorTest {

    @Nested
    @DisplayName("rewriteSubreportExpression()")
    class RewriteSubreportExpression {

        @Test
        void shouldRewriteExpressionAndDeclareParameter() {
            String jrxml = """
                    <jasperReport>
                      <parameter name="USER_ID" class="java.lang.String"/>
                      <detail>
                        <band height="10">
                          <element kind="subreport" x="0" y="0" width="555" height="10">
                            <expression><![CDATA["repo:r06_subreport1.jrxml"]]></expression>
                          </element>
                        </band>
                      </detail>
                    </jasperReport>
                    """;

            String result = SubreportInjector.rewriteSubreportExpression(jrxml, "r06_subreport1.jrxml", "SUBREPORT_R06_1");

            assertThat(result)
                    .contains("<expression><![CDATA[$P{SUBREPORT_R06_1}]]></expression>")
                    .doesNotContain("repo:r06_subreport1.jrxml")
                    .contains("<parameter name=\"SUBREPORT_R06_1\" class=\"net.sf.jasperreports.engine.JasperReport\"/>");
        }

        @Test
        void shouldDeclareParameterAfterLastExistingParameter() {
            String jrxml = """
                    <jasperReport>
                      <parameter name="USER_ID" class="java.lang.String"/>
                      <parameter name="INVOICE_FROM" class="java.lang.String"/>
                      <query language="plsql"><![CDATA[{call CSP_SP_RPT_06()}]]></query>
                      <element kind="subreport">
                        <expression><![CDATA["repo:r06_subreport1.jrxml"]]></expression>
                      </element>
                    </jasperReport>
                    """;

            String result = SubreportInjector.rewriteSubreportExpression(jrxml, "r06_subreport1.jrxml", "SUBREPORT_R06_1");

            int lastParamIdx = result.lastIndexOf("name=\"INVOICE_FROM\"");
            int newParamIdx = result.indexOf("name=\"SUBREPORT_R06_1\"");
            int queryIdx = result.indexOf("<query");
            assertThat(newParamIdx).isGreaterThan(lastParamIdx).isLessThan(queryIdx);
        }

        @Test
        void shouldRewriteAllMatchingOccurrences_whenSameSubreportInvokedMultipleTimes() {
            String jrxml = """
                    <jasperReport>
                      <parameter name="TYPE_CODE_MATURITY" class="java.lang.String"/>
                      <detail>
                        <band height="10">
                          <element kind="subreport">
                            <expression><![CDATA["repo:r11_subreport.jrxml"]]></expression>
                            <parameter name="TYPE_CODE_MATURITY"><expression><![CDATA["O"]]></expression></parameter>
                          </element>
                        </band>
                        <band height="10">
                          <element kind="subreport">
                            <expression><![CDATA["repo:r11_subreport.jrxml"]]></expression>
                            <parameter name="TYPE_CODE_MATURITY"><expression><![CDATA["S"]]></expression></parameter>
                          </element>
                        </band>
                      </detail>
                    </jasperReport>
                    """;

            String result = SubreportInjector.rewriteSubreportExpression(jrxml, "r11_subreport.jrxml", "SUBREPORT_R11_SUB");

            assertThat(result)
                    .doesNotContain("repo:r11_subreport.jrxml")
                    .containsOnlyOnce("<parameter name=\"SUBREPORT_R11_SUB\" class=\"net.sf.jasperreports.engine.JasperReport\"/>");
            int occurrences = result.split("\\$P\\{SUBREPORT_R11_SUB\\}", -1).length - 1;
            assertThat(occurrences).isEqualTo(2);
        }

        @Test
        void shouldNotDuplicateParameterDeclaration_whenAlreadyDeclared() {
            String jrxml = """
                    <jasperReport>
                      <parameter name="SUBREPORT_R06_1" class="net.sf.jasperreports.engine.JasperReport"/>
                      <element kind="subreport">
                        <expression><![CDATA["repo:r06_subreport1.jrxml"]]></expression>
                      </element>
                    </jasperReport>
                    """;

            String result = SubreportInjector.rewriteSubreportExpression(jrxml, "r06_subreport1.jrxml", "SUBREPORT_R06_1");

            assertThat(result).containsOnlyOnce("name=\"SUBREPORT_R06_1\"");
        }

        @Test
        void shouldThrow_whenNoMatchingSubreportFound() {
            String jrxml = """
                    <jasperReport>
                      <element kind="subreport">
                        <expression><![CDATA["repo:other.jrxml"]]></expression>
                      </element>
                    </jasperReport>
                    """;

            assertThatThrownBy(() -> SubreportInjector.rewriteSubreportExpression(jrxml, "missing.jrxml", "SUBREPORT_X"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing.jrxml");
        }

        @Test
        void shouldOnlyMatchElementsWithSubreportKind() {
            String jrxml = """
                    <jasperReport>
                      <element kind="staticText">
                        <expression><![CDATA["repo:r06_subreport1.jrxml"]]></expression>
                      </element>
                    </jasperReport>
                    """;

            assertThatThrownBy(() -> SubreportInjector.rewriteSubreportExpression(jrxml, "r06_subreport1.jrxml", "SUBREPORT_X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addPassThroughParameter()")
    class AddPassThroughParameter {

        @Test
        void shouldAddPassThroughMappingAndDeclareParameter() {
            String jrxml = """
                    <jasperReport>
                      <parameter name="SUBREPORT_R06_1" class="net.sf.jasperreports.engine.JasperReport"/>
                      <element kind="subreport">
                        <expression><![CDATA[$P{SUBREPORT_R06_1}]]></expression>
                        <parameter name="SUBREPORT_DIR"><expression><![CDATA[$P{SUBREPORT_DIR}]]></expression></parameter>
                      </element>
                    </jasperReport>
                    """;

            String result = SubreportInjector.addPassThroughParameter(jrxml, "SUBREPORT_R06_1", "SUBREPORT_R06_2");

            assertThat(result)
                    .contains("<parameter name=\"SUBREPORT_R06_2\"><expression><![CDATA[$P{SUBREPORT_R06_2}]]></expression></parameter>")
                    .contains("<parameter name=\"SUBREPORT_R06_2\" class=\"net.sf.jasperreports.engine.JasperReport\"/>");
        }

        @Test
        void shouldAddPassThroughToAllMatchingSubreportElements() {
            String jrxml = """
                    <jasperReport>
                      <parameter name="SUBREPORT_R11_SUB" class="net.sf.jasperreports.engine.JasperReport"/>
                      <detail>
                        <band height="10">
                          <element kind="subreport">
                            <expression><![CDATA[$P{SUBREPORT_R11_SUB}]]></expression>
                          </element>
                        </band>
                        <band height="10">
                          <element kind="subreport">
                            <expression><![CDATA[$P{SUBREPORT_R11_SUB}]]></expression>
                          </element>
                        </band>
                      </detail>
                    </jasperReport>
                    """;

            String result = SubreportInjector.addPassThroughParameter(jrxml, "SUBREPORT_R11_SUB", "SUBREPORT_R11_XTAB");

            int occurrences = result.split("name=\"SUBREPORT_R11_XTAB\"", -1).length - 1;
            // 2 subreport-element pass-through mappings + 1 top-level parameter declaration
            assertThat(occurrences).isEqualTo(3);
        }

        @Test
        void shouldThrow_whenNoMatchingHostParameterFound() {
            String jrxml = """
                    <jasperReport>
                      <element kind="subreport">
                        <expression><![CDATA[$P{SOME_OTHER_PARAM}]]></expression>
                      </element>
                    </jasperReport>
                    """;

            assertThatThrownBy(() -> SubreportInjector.addPassThroughParameter(jrxml, "SUBREPORT_R06_1", "SUBREPORT_R06_2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SUBREPORT_R06_1");
        }
    }
}
