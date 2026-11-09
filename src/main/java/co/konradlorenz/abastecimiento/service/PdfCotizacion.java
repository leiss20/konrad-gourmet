package co.konradlorenz.abastecimiento.service;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class PdfCotizacion {

    public static byte[] generar(
            String numero,
            String proveedor,
            LocalDate plazo,
            List<Map<String, Object>> lineas) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document d = new Document();

            PdfWriter.getInstance(d, out);
            d.open();

            d.add(new Paragraph(
                    "Konrad Gourmet · Solicitud de cotización N.º " + numero,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)
            ));

            d.add(new Paragraph(
                    "Proveedor: " + proveedor
                    + "\nFecha máxima de respuesta: " + plazo
                    + "\nGenerado: " + LocalDateTime.now()
            ));

            PdfPTable t = new PdfPTable(3);
            t.setSpacingBefore(12);

            for (String h : new String[]{"Producto", "Cantidad", "Unidad"}) {
                t.addCell(h);
            }

            for (Map<String, Object> l : lineas) {
                t.addCell(String.valueOf(l.get("producto")));
                t.addCell(String.valueOf(l.get("cantidad")));
                t.addCell(String.valueOf(l.get("unidad")));
            }

            d.add(t);
            d.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }
}