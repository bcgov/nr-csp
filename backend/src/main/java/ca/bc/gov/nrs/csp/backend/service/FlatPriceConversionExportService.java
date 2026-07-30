package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.service.model.FlatPriceConversion;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
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
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlatPriceConversionExportService {

    private static final String[] HEADERS = {
        "Maturity", "Species", "Sort Code", "Grade", "Flat Price Conversion", "Effective Date", "Expiry Date"
    };
    private static final float[] COLUMN_WIDTHS = {1f, 2f, 1f, 1f, 2f, 2f, 2f};
    private static final String FILENAME_BASE = "FlatPriceConversions";

    private final FlatPriceConversionRepository repository;
    private final LookupService lookupService;

    public FlatPriceConversionExportService(FlatPriceConversionRepository repository, LookupService lookupService) {
        this.repository = repository;
        this.lookupService = lookupService;
    }

    @Transactional(readOnly = true)
    public ReportResult exportPdf(String modellingCode, String maturity, String sortCode, String species, String grade) {
        List<FlatPriceConversion> records = repository.search(modellingCode, maturity, sortCode, species, grade);
        return new ReportResult(generatePdf(records, buildDescriptionMaps()), FILENAME_BASE + ".pdf");
    }

    @Transactional(readOnly = true)
    public ReportResult exportCsv(String modellingCode, String maturity, String sortCode, String species, String grade) {
        List<FlatPriceConversion> records = repository.search(modellingCode, maturity, sortCode, species, grade);
        return new ReportResult(generateCsv(records, buildDescriptionMaps()), FILENAME_BASE + ".csv");
    }

    private DescriptionMaps buildDescriptionMaps() {
        Map<String, String> maturity = toDescriptionMap(lookupService.getMaturityCodes());
        Map<String, String> species = toDescriptionMap(lookupService.getSpeciesCodes());
        Map<String, String> sortCodes = toDescriptionMap(lookupService.getSortCodes());
        return new DescriptionMaps(maturity, species, sortCodes);
    }

    private Map<String, String> toDescriptionMap(List<LookupItem> items) {
        return items.stream().collect(Collectors.toMap(LookupItem::code, LookupItem::description, (a, b) -> a));
    }

    private record DescriptionMaps(Map<String, String> maturity, Map<String, String> species, Map<String, String> sortCodes) {
        String maturityDesc(String code) { return code != null ? maturity.getOrDefault(code, code) : ""; }
        String speciesDesc(String code) { return code != null ? species.getOrDefault(code, code) : ""; }
        String sortCodeDesc(String code) { return code != null ? sortCodes.getOrDefault(code, code) : ""; }
    }

    private byte[] generatePdf(List<FlatPriceConversion> records, DescriptionMaps maps) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph title = new Paragraph("Flat Price Conversion", titleFont);
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

            for (FlatPriceConversion r : records) {
                addCell(table, maps.maturityDesc(r.maturity()), cellFont);
                addCell(table, maps.speciesDesc(r.species()), cellFont);
                addCell(table, maps.sortCodeDesc(r.sortCode()), cellFont);
                addCell(table, r.grade(), cellFont);
                addCell(table, r.flatPriceConversion() != null ? r.flatPriceConversion().toString() : "", cellFont);
                addCell(table, dateStr(r.effectiveDate()), cellFont);
                addCell(table, dateStr(r.expiryDate()), cellFont);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ReportGenerationException("Failed to generate flat price conversion PDF", e);
        }
    }

    private byte[] generateCsv(List<FlatPriceConversion> records, DescriptionMaps maps) {
        StringBuilder sb = new StringBuilder();
        sb.append("Maturity,Species,Sort Code,Grade,Flat Price Conversion,Effective Date,Expiry Date\n");
        for (FlatPriceConversion r : records) {
            sb.append(csvField(maps.maturityDesc(r.maturity()))).append(',')
              .append(csvField(maps.speciesDesc(r.species()))).append(',')
              .append(csvField(maps.sortCodeDesc(r.sortCode()))).append(',')
              .append(csvField(r.grade())).append(',')
              .append(r.flatPriceConversion() != null ? r.flatPriceConversion() : "").append(',')
              .append(dateStr(r.effectiveDate())).append(',')
              .append(dateStr(r.expiryDate())).append('\n');
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
