package com.tiendapesca.APItiendapesca.Service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.tiendapesca.APItiendapesca.Dtos.InvoicePdfDTO;
import com.tiendapesca.APItiendapesca.Dtos.ProductItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Service
public class PdfGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGeneratorService.class);

    private static final String INVOICE_DIRECTORY = "invoices";
    
    // RUTAS SIMPLIFICADAS - Eliminamos dependencia de recursos externos
    private static final String COMPANY_NAME = "Kraken Lures";
    private static final String COMPANY_ADDRESS = "Limón, Costa Rica";
    private static final String COMPANY_PHONE = "+506 2222-5555";
    private static final String COMPANY_EMAIL = "info@krakenlures.com";
    private static final String COMPANY_WEBSITE = "www.krakenlures.com";

    private static final BaseColor PRIMARY_COLOR = new BaseColor(0, 51, 102);
    private static final BaseColor SECONDARY_COLOR = new BaseColor(220, 220, 220);
    private static final BaseColor ACCENT_COLOR = new BaseColor(255, 153, 0);

    // 🔹 SIMPLIFICAMOS: Usamos fuentes estándar de iText
    private static Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
    private static Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

    public byte[] generateInvoicePdf(InvoicePdfDTO invoiceDto) {
        logger.info("🔄 Iniciando generación de PDF para factura: {}", invoiceDto.getInvoiceNumber());
        
        Document document = new Document(PageSize.A4, 40, 40, 60, 40);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new InvoicePageEventHandler());
            
            document.open();

            // Espacio inicial
            document.add(new Paragraph(" "));
            
            addInvoiceHeader(document);
            addInvoiceInfo(document, invoiceDto);
            addCustomerInfo(document, invoiceDto);
            addProductsTable(document, invoiceDto);
            addTotalsSection(document, invoiceDto);
            addTermsAndConditions(document);

            document.close();
            
            byte[] pdfBytes = outputStream.toByteArray();
            logger.info("✅ PDF generado exitosamente - Tamaño: {} bytes", pdfBytes.length);
            return pdfBytes;
            
        } catch (Exception e) {
            logger.error("❌ Error crítico al generar PDF: {}", e.getMessage(), e);
            // Cerrar document si está abierto
            if (document.isOpen()) {
                document.close();
            }
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    public String savePdfToStorage(byte[] pdfBytes, String invoiceNumber) throws IOException {
        logger.info("💾 Guardando PDF para factura: {}", invoiceNumber);
        
        try {
            Path directory = Paths.get(INVOICE_DIRECTORY);
            
            // Verificación y creación de directorio
            if (!Files.exists(directory)) {
                logger.info("📁 Creando directorio: {}", directory.toAbsolutePath());
                Files.createDirectories(directory);
            }

            // Nombre de archivo seguro
            String fileName = "factura_" + invoiceNumber.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
            Path filePath = directory.resolve(fileName);
            
            // Guardar archivo
            Files.write(filePath, pdfBytes);
            
            String absolutePath = filePath.toAbsolutePath().toString();
            logger.info("✅ PDF guardado en: {}", absolutePath);
            return absolutePath;
            
        } catch (Exception e) {
            logger.error("❌ Error al guardar PDF: {}", e.getMessage(), e);
            throw new IOException("No se pudo guardar el PDF: " + e.getMessage(), e);
        }
    }

    private void addInvoiceHeader(Document document) throws DocumentException {
        try {
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
            Paragraph title = new Paragraph("FACTURA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(25f);
            document.add(title);
        } catch (Exception e) {
            logger.warn("⚠️ Error en encabezado, continuando...");
            // Continuar sin encabezado
        }
    }

    private void addInvoiceInfo(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        try {
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            addInfoRow(infoTable, "Número de Factura:", 
                      invoiceDto.getInvoiceNumber() != null ? invoiceDto.getInvoiceNumber() : "N/A", 
                      labelFont);
            
            String fecha = invoiceDto.getDate() != null ? 
                          invoiceDto.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : 
                          "N/A";
            addInfoRow(infoTable, "Fecha de Emisión:", fecha, labelFont);
            
            String paymentMethod = invoiceDto.getPaymentMethod() != null ? 
                                  invoiceDto.getPaymentMethod() + " - Contado" : 
                                  "No especificado";
            addInfoRow(infoTable, "Términos de Pago:", paymentMethod, labelFont);

            document.add(infoTable);
        } catch (Exception e) {
            logger.warn("⚠️ Error en información de factura, continuando...");
        }
    }

    private void addCustomerInfo(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        try {
            PdfPTable customerTable = new PdfPTable(2);
            customerTable.setWidthPercentage(100);
            customerTable.setSpacingAfter(20f);

            PdfPCell sectionHeader = new PdfPCell(new Phrase("INFORMACIÓN DEL CLIENTE",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR)));
            sectionHeader.setColspan(2);
            sectionHeader.setBorder(Rectangle.NO_BORDER);
            sectionHeader.setPaddingBottom(8f);
            customerTable.addCell(sectionHeader);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            addInfoRow(customerTable, "Nombre:", 
                      invoiceDto.getCustomerName() != null ? invoiceDto.getCustomerName() : "No especificado", 
                      labelFont);
            addInfoRow(customerTable, "Email:", 
                      invoiceDto.getCustomerEmail() != null ? invoiceDto.getCustomerEmail() : "No especificado", 
                      labelFont);
            addInfoRow(customerTable, "Dirección:", 
                      invoiceDto.getShippingAddress() != null ? invoiceDto.getShippingAddress() : "No especificado", 
                      labelFont);
            addInfoRow(customerTable, "Teléfono:", 
                      invoiceDto.getPhone() != null ? invoiceDto.getPhone() : "No especificado", 
                      labelFont);

            document.add(customerTable);
        } catch (Exception e) {
            logger.warn("⚠️ Error en información del cliente, continuando...");
        }
    }

    private void addProductsTable(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        try {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15f);
            table.setSpacingAfter(25f);

            float[] columnWidths = {3f, 1.5f, 1f, 1.5f, 1.5f};
            table.setWidths(columnWidths);

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

            addTableHeaderCell(table, "Producto", headerFont);
            addTableHeaderCell(table, "Precio Unitario", headerFont);
            addTableHeaderCell(table, "Cantidad", headerFont);
            addTableHeaderCell(table, "Subtotal", headerFont);
            addTableHeaderCell(table, "Impuesto", headerFont);

            if (invoiceDto.getProducts() != null && !invoiceDto.getProducts().isEmpty()) {
                for (ProductItemDTO product : invoiceDto.getProducts()) {
                    addProductRow(table, product);
                }
            } else {
                // Fila vacía si no hay productos
                addEmptyProductRow(table);
            }

            document.add(table);
        } catch (Exception e) {
            logger.warn("⚠️ Error en tabla de productos, continuando...");
        }
    }

    private void addTotalsSection(Document document, InvoicePdfDTO invoiceDto) throws DocumentException {
        try {
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setSpacingAfter(15f);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            
            BigDecimal subtotal = invoiceDto.getSubtotal() != null ? invoiceDto.getSubtotal() : BigDecimal.ZERO;
            BigDecimal tax = invoiceDto.getTax() != null ? invoiceDto.getTax() : BigDecimal.ZERO;
            BigDecimal total = invoiceDto.getTotal() != null ? invoiceDto.getTotal() : BigDecimal.ZERO;
            
            addTotalRow(totalsTable, "Subtotal:", subtotal, labelFont);
            addTotalRow(totalsTable, "Impuesto (13%):", tax, labelFont);

            // Línea divisoria
            PdfPCell dividerCell = new PdfPCell(new Phrase(" "));
            dividerCell.setColspan(2);
            dividerCell.setBorder(PdfPCell.TOP);
            dividerCell.setBorderColor(BaseColor.LIGHT_GRAY);
            dividerCell.setPaddingTop(5f);
            dividerCell.setPaddingBottom(5f);
            totalsTable.addCell(dividerCell);

            Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
            addTotalRow(totalsTable, "TOTAL:", total, totalLabelFont);

            document.add(totalsTable);
        } catch (Exception e) {
            logger.warn("⚠️ Error en sección de totales, continuando...");
        }
    }

    private void addTermsAndConditions(Document document) throws DocumentException {
        try {
            Paragraph terms = new Paragraph();
            terms.setSpacingBefore(25f);

            terms.add(new Chunk("Términos y Condiciones:\n",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.DARK_GRAY)));

            terms.add(new Chunk("1. Todos los precios están en colones costarricenses.\n" +
                            "2. Pago debido inmediatamente al recibir la factura.\n" +
                            "3. Productos no retornables una vez abiertos.\n" +
                            "4. Garantía limitada a 30 días contra defectos de fabricación.\n",
                    new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY)));

            document.add(terms);
        } catch (Exception e) {
            logger.warn("⚠️ Error en términos y condiciones, continuando...");
        }
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont) {
        try {
            PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPadding(3f);
            table.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(value));
            valueCell.setBorder(Rectangle.NO_BORDER);
            valueCell.setPadding(3f);
            table.addCell(valueCell);
        } catch (Exception e) {
            logger.warn("⚠️ Error añadiendo fila de información: {}", label);
        }
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font) {
        try {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(6f);
            table.addCell(cell);
        } catch (Exception e) {
            logger.warn("⚠️ Error añadiendo celda de encabezado: {}", text);
        }
    }

    private void addProductRow(PdfPTable table, ProductItemDTO product) {
        try {
            String productName = product.getName() != null ? product.getName() : "Producto";
            BigDecimal unitPrice = product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO;
            Integer quantity = product.getQuantity() != null ? product.getQuantity() : 0;
            BigDecimal subtotal = product.getSubtotal() != null ? product.getSubtotal() : BigDecimal.ZERO;
            BigDecimal tax = product.getTax() != null ? product.getTax() : BigDecimal.ZERO;
            
            table.addCell(createTableCell(productName, Element.ALIGN_LEFT, normalFont));
            table.addCell(createTableCell(formatCurrency(unitPrice), Element.ALIGN_RIGHT, normalFont));
            table.addCell(createTableCell(String.valueOf(quantity), Element.ALIGN_CENTER, normalFont));
            table.addCell(createTableCell(formatCurrency(subtotal), Element.ALIGN_RIGHT, normalFont));
            table.addCell(createTableCell(formatCurrency(tax), Element.ALIGN_RIGHT, normalFont));
        } catch (Exception e) {
            logger.warn("⚠️ Error añadiendo fila de producto");
        }
    }

    private void addEmptyProductRow(PdfPTable table) {
        try {
            table.addCell(createTableCell("No hay productos", Element.ALIGN_CENTER, normalFont));
            table.addCell(createTableCell("", Element.ALIGN_CENTER, normalFont));
            table.addCell(createTableCell("", Element.ALIGN_CENTER, normalFont));
            table.addCell(createTableCell("", Element.ALIGN_CENTER, normalFont));
            table.addCell(createTableCell("", Element.ALIGN_CENTER, normalFont));
        } catch (Exception e) {
            logger.warn("⚠️ Error añadiendo fila vacía");
        }
    }

    private PdfPCell createTableCell(String text, int alignment, Font font) {
        try {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setHorizontalAlignment(alignment);
            cell.setPadding(5f);
            return cell;
        } catch (Exception e) {
            // Celda de respaldo
            return new PdfPCell(new Phrase(""));
        }
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal value, Font labelFont) {
        try {
            PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            labelCell.setPadding(3f);
            table.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(value)));
            valueCell.setBorder(Rectangle.NO_BORDER);
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valueCell.setPadding(3f);
            table.addCell(valueCell);
        } catch (Exception e) {
            logger.warn("⚠️ Error añadiendo fila de total: {}", label);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        try {
            return String.format("₡%,.2f", amount);
        } catch (Exception e) {
            return "₡0.00";
        }
    }

    /**
     * PageEventHandler simplificado - Sin dependencias de recursos externos
     */
    private class InvoicePageEventHandler extends PdfPageEventHelper {
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();

                // Encabezado de empresa (sin logo)
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(COMPANY_NAME,
                                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR)),
                        document.left(), document.top() - 20, 0);

                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(COMPANY_ADDRESS,
                                new Font(Font.FontFamily.HELVETICA, 8)),
                        document.left(), document.top() - 30, 0);

                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase("Tel: " + COMPANY_PHONE,
                                new Font(Font.FontFamily.HELVETICA, 8)),
                        document.left(), document.top() - 40, 0);

                // Línea separadora
                cb.setColorStroke(PRIMARY_COLOR);
                cb.setLineWidth(0.8f);
                cb.moveTo(document.left(), document.top() - 50);
                cb.lineTo(document.right(), document.top() - 50);
                cb.stroke();

                // Pie de página
                Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(COMPANY_NAME + " - " + COMPANY_WEBSITE, footerFont),
                        (document.right() - document.left()) / 2 + document.left(),
                        document.bottom() - 20, 0);

            } catch (Exception e) {
                logger.warn("⚠️ Error en encabezado/pie de página, continuando...");
            }
        }
    }
}