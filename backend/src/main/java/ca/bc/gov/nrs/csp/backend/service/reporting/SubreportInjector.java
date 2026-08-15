package ca.bc.gov.nrs.csp.backend.service.reporting;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Rewrites JasperReports Server-only {@code "repo:name.jrxml"} subreport expressions into
 * {@code $P{...}} parameter references so subreports can be filled locally without a repository
 * service. Used by reports that host subreports (R06, R11) — self-contained reports (R07, R08,
 * R10, R12, R13) never need this.
 *
 * <p>The caller compiles each subreport separately (deepest first) and supplies the compiled
 * {@link net.sf.jasperreports.engine.JasperReport} objects as parameters when filling the parent —
 * see {@code rewriteSubreportExpression}. When a subreport itself hosts a further nested
 * subreport, the grandchild's compiled-report parameter has to be threaded through the
 * intermediate subreport's own parameter-passing block so it reaches where it's actually
 * used — see {@code addPassThroughParameter}.</p>
 */
public final class SubreportInjector {

    private static final String JASPER_REPORT_CLASS = "net.sf.jasperreports.engine.JasperReport";
    private static final String ELEMENT_EXPRESSION = "expression";
    private static final String ELEMENT_PARAMETER = "parameter";

    private SubreportInjector() {}

    /**
     * Rewrites every {@code <element kind="subreport">} whose expression is the literal
     * {@code "repo:<repoFileName>"} to instead reference {@code $P{parameterName}}, and declares
     * {@code parameterName} as a top-level {@code JasperReport}-class parameter (if not already
     * declared). A single repo file may be referenced by more than one subreport element in the
     * same document (e.g. R11.jrxml invokes the same subreport four times with different literal
     * parameters) — all matching occurrences are rewritten to share the one compiled report.
     *
     * @throws IllegalArgumentException if no subreport element references {@code repoFileName}
     */
    public static String rewriteSubreportExpression(String jrxmlContent, String repoFileName, String parameterName) {
        Document doc = parse(jrxmlContent);
        List<Element> matches = findSubreportElementsByExpression(doc.getRootElement(), "\"repo:" + repoFileName + "\"");
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No subreport element found with expression repo:" + repoFileName);
        }
        for (Element subreportEl : matches) {
            Element exprEl = subreportEl.element(ELEMENT_EXPRESSION);
            exprEl.clearContent();
            exprEl.addCDATA("$P{" + parameterName + "}");
        }
        ensureJasperReportParameterDeclared(doc, parameterName);
        return doc.asXML();
    }

    /**
     * Adds a pass-through parameter mapping to every {@code <element kind="subreport">} whose
     * expression is {@code $P{hostParameterName}} (i.e. one already rewritten by
     * {@link #rewriteSubreportExpression}), forwarding {@code $P{passThroughParameterName}} down
     * into that subreport's own parameter map — and declares {@code passThroughParameterName} as
     * a top-level {@code JasperReport}-class parameter on the hosting document (if not already
     * declared), since it is now referenced there.
     *
     * @throws IllegalArgumentException if no subreport element references {@code hostParameterName}
     */
    public static String addPassThroughParameter(String jrxmlContent, String hostParameterName, String passThroughParameterName) {
        Document doc = parse(jrxmlContent);
        List<Element> matches = findSubreportElementsByExpression(doc.getRootElement(), "$P{" + hostParameterName + "}");
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No subreport element found referencing parameter " + hostParameterName);
        }
        for (Element subreportEl : matches) {
            Element paramEl = subreportEl.addElement(ELEMENT_PARAMETER);
            paramEl.addAttribute("name", passThroughParameterName);
            paramEl.addElement(ELEMENT_EXPRESSION).addCDATA("$P{" + passThroughParameterName + "}");
        }
        ensureJasperReportParameterDeclared(doc, passThroughParameterName);
        return doc.asXML();
    }

    private static List<Element> findSubreportElementsByExpression(Element parent, String expressionText) {
        List<Element> results = new ArrayList<>();
        collectSubreportElementsByExpression(parent, expressionText, results);
        return results;
    }

    private static void collectSubreportElementsByExpression(Element parent, String expressionText, List<Element> results) {
        for (Iterator<Element> it = parent.elementIterator(); it.hasNext(); ) {
            Element child = it.next();
            if ("element".equals(child.getName()) && "subreport".equals(child.attributeValue("kind"))) {
                Element expr = child.element(ELEMENT_EXPRESSION);
                if (expr != null && expressionText.equals(expr.getTextTrim())) {
                    results.add(child);
                }
            }
            collectSubreportElementsByExpression(child, expressionText, results);
        }
    }

    private static void ensureJasperReportParameterDeclared(Document doc, String name) {
        Element root = doc.getRootElement();
        for (Element existing : root.elements(ELEMENT_PARAMETER)) {
            if (name.equals(existing.attributeValue("name"))) return;
        }

        Element newParam = DocumentHelper.createElement(ELEMENT_PARAMETER);
        newParam.addAttribute("name", name);
        newParam.addAttribute("class", JASPER_REPORT_CLASS);

        List<Element> topParams = root.elements(ELEMENT_PARAMETER);
        List<?> content = root.content();
        int insertIndex;
        if (!topParams.isEmpty()) {
            insertIndex = content.indexOf(topParams.get(topParams.size() - 1)) + 1;
        } else {
            List<Element> topProps = root.elements("property");
            insertIndex = topProps.isEmpty() ? 0 : content.indexOf(topProps.get(topProps.size() - 1)) + 1;
        }
        root.content().add(insertIndex, newParam);
    }

    private static Document parse(String jrxmlContent) {
        try {
            return DocumentHelper.parseText(jrxmlContent);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to parse JRXML content", e);
        }
    }
}
