package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class SortCodeExportService {

    private static final String[] HEADERS = {
        "Sort Code", "Description", "Effective Date", "Expiry Date", "Update Timestamp"
    };
    private static final float[] COLUMN_WIDTHS = {1f, 5f, 2f, 2f, 2f};
    private static final String FILENAME_BASE = "Sortcodes";

    private final SortCodeService sortCodeService;

    public SortCodeExportService(SortCodeService sortCodeService) {
        this.sortCodeService = sortCodeService;
    }

    public ReportResult exportPdf() {
        List<SortCode> sortCodes = sortCodeService.listAll();
        byte[] data = generatePdf(sortCodes);
        return new ReportResult(data, FILENAME_BASE + ".pdf");
    }

    public ReportResult exportCsv() {
        List<SortCode> sortCodes = sortCodeService.listAll();
        byte[] data = generateCsv(sortCodes);
        return new ReportResult(data, FILENAME_BASE + ".csv");
    }

    private byte[] generatePdf(List<SortCode> sortCodes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph title = new Paragraph("Sort Code", titleFont);
            title.setSpacingAfter(10);
            document.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            table.setWidths(COLUMN_WIDTHS);

            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(220, 220, 220));
                cell.setPadding(4);
                table.addCell(cell);
            }

            for (SortCode sc : sortCodes) {
                addCell(table, sc.sortCode(), cellFont);
                addCell(table, sc.description(), cellFont);
                addCell(table, dateStr(sc.effectiveDate()), cellFont);
                addCell(table, dateStr(sc.expiryDate()), cellFont);
                addCell(table, dateStr(sc.updateTimestamp()), cellFont);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ReportGenerationException("Failed to generate sort code PDF", e);
        }
    }

    private byte[] generateCsv(List<SortCode> sortCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sort Code,Description,Effective Date,Expiry Date,Update Timestamp\n");
        for (SortCode sc : sortCodes) {
            sb.append(csvField(sc.sortCode())).append(',')
              .append(csvField(sc.description())).append(',')
              .append(dateStr(sc.effectiveDate())).append(',')
              .append(dateStr(sc.expiryDate())).append(',')
              .append(dateStr(sc.updateTimestamp())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String dateStr(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private String csvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
