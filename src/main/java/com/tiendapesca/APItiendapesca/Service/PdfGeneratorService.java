package com.tiendapesca.APItiendapesca.Service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.tiendapesca.APItiendapesca.Dtos.InvoicePdfDTO;
import com.tiendapesca.APItiendapesca.Dtos.ProductItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Service
public class PdfGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGeneratorService.class);
    private static final String INVOICE_DIRECTORY = "invoices";

    // Datos de la Empresa
    private static final String COMPANY_NAME = "Kraken Lures";
    private static final String COMPANY_ADDRESS = "Limón, Costa Rica";
    private static final String COMPANY_PHONE = "+506 2222-5555";
    private static final String COMPANY_EMAIL = "info@krakenlures.com";
    private static final String COMPANY_WEBSITE = "www.krakenlures.com";

    private static final BaseColor PRIMARY_COLOR = new BaseColor(0, 51, 102);

    private BaseFont robotoBaseFont = null;
    private BaseFont robotoBoldBaseFont = null;
    private Font normalRobotoFont;
    private Font boldRobotoFont;
    private Font titleRobotoFont;
    private Font smallRobotoFont;

    public PdfGeneratorService() {
        initializeFonts();
    }

    private void initializeFonts() {
        try {
            // Rutas ajustadas segun tu estructura de carpetas (image_f13107.png)
            String regularPath = "fonts/ttf/DejaVuLGCSans-Bold.ttf";
            String boldPath = "fonts/ttf/DejaVuLGCSans-Bold.ttf";

            byte[] regularBytes = new ClassPathResource(regularPath).getInputStream().readAllBytes();
            byte[] boldBytes = new ClassPathResource(boldPath).getInputStream().readAllBytes();

            // IDENTITY_H permite que el simbolo ₡ se muestre correctamente
            robotoBaseFont = BaseFont.createFont("Roboto-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, regularBytes, null);
            robotoBoldBaseFont = BaseFont.createFont("Roboto-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, boldBytes, null);

            normalRobotoFont = new Font(robotoBaseFont, 10);
            boldRobotoFont = new Font(robotoBoldBaseFont, 10, Font.BOLD);
            titleRobotoFont = new Font(robotoBoldBaseFont, 24, Font.BOLD, PRIMARY_COLOR);
            smallRobotoFont = new Font(robotoBaseFont, 8);

        } catch (Exception e) {
            logger.error("Error cargando fuentes: {}", e.getMessage());
            // Fallback
            normalRobotoFont = new Font(Font.FontFamily.HELVETICA, 10);
            boldRobotoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            titleRobotoFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
        }
    }

    public byte[] generateInvoicePdf(InvoicePdfDTO invoiceDto) {
        if (robotoBaseFont == null) initializeFonts();
        Document document = new Document(PageSize.A4, 40, 40, 80, 40);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new InvoicePageEventHandler());
            document.open();

            addInvoiceHeader(document);
            addInvoiceInfo(document, invoiceDto);
            addCustomerInfo(document, invoiceDto);
            addProductsTable(document, invoiceDto);
            addTotalsSection(document, invoiceDto);
            addTermsAndConditions(document);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public String savePdfToStorage(byte[] pdfBytes, String invoiceNumber) throws IOException {
        Path directory = Paths.get(INVOICE_DIRECTORY);
        if (!Files.exists(directory)) Files.createDirectories(directory);
        String fileName = "factura_" + invoiceNumber.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
        Path filePath = directory.resolve(fileName);
        Files.write(filePath, pdfBytes);
        return INVOICE_DIRECTORY + "/" + fileName;
    }

    // ========== METODOS DE DIBUJO ==========

    private void addInvoiceHeader(Document document) throws DocumentException {
        Paragraph title = new Paragraph("FACTURA", titleRobotoFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);
    }

    private void addInvoiceInfo(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addInfoRow(table, "Número de Factura:", invoiceDto.getInvoiceNumber());
        addInfoRow(table, "Fecha de Emisión:", invoiceDto.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        addInfoRow(table, "Términos de Pago:", invoiceDto.getPaymentMethod() + " - Contado");
        document.add(table);
    }

    private void addCustomerInfo(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        document.add(new Paragraph("\nINFORMACIÓN DEL CLIENTE", new Font(robotoBoldBaseFont, 12, Font.BOLD, PRIMARY_COLOR)));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addInfoRow(table, "Nombre:", invoiceDto.getCustomerName());
        addInfoRow(table, "Email:", invoiceDto.getCustomerEmail());
        addInfoRow(table, "Dirección:", invoiceDto.getShippingAddress());
        document.add(table);
    }

    private void addProductsTable(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15f);
        table.setWidths(new float[]{3f, 1.5f, 1f, 1.5f, 1.5f});

        String[] headers = {"Producto", "Precio Unitario", "Cantidad", "Subtotal", "Impuesto"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(robotoBoldBaseFont, 10, Font.BOLD, BaseColor.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (ProductItemDTO p : invoiceDto.getProducts()) {
            table.addCell(new PdfPCell(new Phrase(p.getName(), normalRobotoFont)));
            table.addCell(createCurrencyCell(p.getUnitPrice()));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getQuantity()), normalRobotoFont)));
            table.addCell(createCurrencyCell(p.getSubtotal()));
            table.addCell(createCurrencyCell(p.getTax()));
        }
        document.add(table);
    }

    private void addTotalsSection(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addTotalRow(table, "Subtotal:", invoiceDto.getSubtotal(), boldRobotoFont);
        addTotalRow(table, "Impuesto (13%):", invoiceDto.getTax(), boldRobotoFont);
        addTotalRow(table, "TOTAL:", invoiceDto.getTotal(), new Font(robotoBoldBaseFont, 12, Font.BOLD, PRIMARY_COLOR));
        document.add(table);
    }

    private void addTermsAndConditions(Document document) throws DocumentException {
        Paragraph terms = new Paragraph("\nTérminos y Condiciones:\n1. Precios en colones (₡).\n2. Pago inmediato.", smallRobotoFont);
        document.add(terms);
    }

    // ========== AUXILIARES ==========

    private PdfPCell createCurrencyCell(BigDecimal amount) {
        String formatted = String.format("%,.2f", amount);
        PdfPCell cell = new PdfPCell(new Phrase("₡ " + formatted, normalRobotoFont));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, boldRobotoFont));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(value, normalRobotoFont));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal val, Font f) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, f));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        PdfPCell c2 = createCurrencyCell(val);
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    // ========== TU CLASE DE EVENTOS INTEGRADA ==========
    private class InvoicePageEventHandler extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            try {
                // Logo (Usando ClassPathResource para mayor compatibilidad)
                Image logo = Image.getInstance(new ClassPathResource("static/img/logoKraken.png").getURL());
                logo.scaleToFit(70, 70);
                logo.setAbsolutePosition(document.left(), document.top() + 15);
                cb.addImage(logo);

                // Datos de la Empresa (Lado derecho)
                Font companyFont = new Font(robotoBoldBaseFont, 12, Font.BOLD, PRIMARY_COLOR);
                Font infoFont = new Font(robotoBaseFont, 8);

                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(COMPANY_NAME, companyFont), document.right(), document.top() + 70, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(COMPANY_ADDRESS, infoFont), document.right(), document.top() + 58, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Tel: " + COMPANY_PHONE, infoFont), document.right(), document.top() + 48, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(COMPANY_WEBSITE, infoFont), document.right(), document.top() + 38, 0);

                // Línea azul
                cb.setColorStroke(PRIMARY_COLOR);
                cb.setLineWidth(1.2f);
                cb.moveTo(document.left(), document.top() + 10);
                cb.lineTo(document.right(), document.top() + 10);
                cb.stroke();

            } catch (Exception e) {
                logger.warn("No se pudo dibujar el encabezado: {}", e.getMessage());
            }
        }
    }
}