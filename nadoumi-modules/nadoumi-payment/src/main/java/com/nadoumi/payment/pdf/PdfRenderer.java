package com.nadoumi.payment.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ColumnText;
import com.nadoumi.identity.service.mail.BrandProperties;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * The one place that knows how a Nadoumi document looks: A4 page, logo watermark under everything, the company
 * block and page numbers in the footer of every page. Receipt and application form only write their content.
 *
 * <p>Student text is drawn as plain strings (never parsed as HTML or CSS), so a hostile value cannot make the
 * renderer read a file or open a connection.</p>
 */
@Component
public class PdfRenderer {

    static final Color INK = new Color(0x0F172A);
    static final Color MUTED = new Color(0x64748B);
    static final Color RULE = new Color(0xE2E8F0);
    static final Color BRAND = new Color(0xF29200);
    static final Color PRICE = new Color(0xDC2626);
    static final Color BAND = new Color(0xF8FAFC);

    /** Watermark strength. Kept low so body text always stays well above AA contrast. */
    static final float WATERMARK_OPACITY = 0.06f;
    private static final float WATERMARK_WIDTH = mm(110);
    private static final float SIDE_MARGIN = mm(16);
    private static final float TOP_MARGIN = mm(16);
    private static final float BOTTOM_MARGIN = mm(30);
    private static final float FOOTER_RULE_Y = mm(24);
    private static final float SECTION_MIN_ROOM = mm(45);
    private static final int MAX_PAGES = 30;

    private final PdfAssets assets;
    private final BrandProperties brand;

    public PdfRenderer(PdfAssets assets, BrandProperties brand) {
        this.assets = assets;
        this.brand = brand;
    }

    /** Writes a document: {@code body} adds content to the {@link Page}. */
    public byte[] render(String title, String documentNumber, Consumer<Page> body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, SIDE_MARGIN, SIDE_MARGIN, TOP_MARGIN, BOTTOM_MARGIN);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new BrandedPages(documentNumber));
            doc.addTitle(PdfText.printable(title, assets::canDraw));
            doc.addSubject(PdfText.printable(documentNumber, assets::canDraw));
            doc.addAuthor("Nadoumi Education Consulting");
            doc.addCreator("Nadoumi");
            doc.open();
            body.accept(new Page(doc, writer));
            if (writer.getPageNumber() > MAX_PAGES) {
                throw new IllegalStateException("document exceeds " + MAX_PAGES + " pages");
            }
            doc.close();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("could not render the PDF", e);
        }
        return out.toByteArray();
    }

    static float mm(float millimetres) {
        return millimetres * 72f / 25.4f;
    }

    /** Content helpers over one open document. */
    public final class Page {

        private final Document doc;
        private final PdfWriter writer;

        private Page(Document doc, PdfWriter writer) {
            this.doc = doc;
            this.writer = writer;
        }

        /** Logo, title and the right-hand block of reference lines. */
        public void masthead(String title, List<String[]> references) {
            masthead(title, references, null);
        }

        /** As {@link #masthead(String, List)}, with the applicant's photo (JPEG bytes) in a framed box on the right. */
        public void masthead(String title, List<String[]> references, byte[] photoJpeg) {
            boolean withPhoto = photoJpeg != null;
            PdfPTable table = new PdfPTable(withPhoto ? new float[] {1.0f, 2.8f, 2.0f, 1.1f} : new float[] {1.1f, 2.6f, 2.3f});
            table.setWidthPercentage(100);
            Image logo = assets.logo();
            logo.scaleToFit(mm(22), mm(22));
            PdfPCell logoCell = cell(Rectangle.NO_BORDER, 0);
            logoCell.addElement(logo);
            table.addCell(logoCell);

            PdfPCell titleCell = cell(Rectangle.NO_BORDER, 0);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleCell.addElement(new Paragraph(text(title), assets.bold(20, INK)));
            titleCell.addElement(new Paragraph("Nadoumi Education Consulting", assets.regular(9, MUTED)));
            table.addCell(titleCell);

            PdfPCell refs = cell(Rectangle.NO_BORDER, 0);
            refs.setVerticalAlignment(Element.ALIGN_MIDDLE);
            for (String[] reference : references) {
                Paragraph line = new Paragraph();
                line.setAlignment(Element.ALIGN_RIGHT);
                line.add(new Phrase(text(reference[0]) + "  ", assets.regular(8, MUTED)));
                line.add(new Phrase(text(reference[1]), assets.bold(9, INK)));
                refs.addElement(line);
            }
            table.addCell(refs);
            if (withPhoto) {
                table.addCell(photoCell(photoJpeg));
            }
            add(table);
            add(rule(BRAND, 1.2f, mm(3), mm(5)));
        }

        private PdfPCell photoCell(byte[] jpeg) {
            PdfPCell cell = cell(Rectangle.BOX, 2);
            cell.setBorderColor(RULE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            try {
                Image photo = Image.getInstance(jpeg);
                photo.scaleToFit(mm(24), mm(31));
                cell.addElement(photo);
            }
            catch (IOException | DocumentException e) {
                cell.setBorder(Rectangle.NO_BORDER); // an unusable image is left out, never an error
            }
            return cell;
        }

        /** A numbered section heading. */
        public void section(int number, String title) {
            if (writer.getVerticalPosition(true) - doc.bottom() < SECTION_MIN_ROOM) {
                doc.newPage(); // never strand a heading at the foot of a page
            }
            PdfPTable heading = new PdfPTable(new float[] {0.35f, 9.65f});
            heading.setWidthPercentage(100);
            heading.setSpacingBefore(mm(5));
            heading.setSpacingAfter(mm(2));
            heading.setKeepTogether(true);
            PdfPCell badge = cell(Rectangle.NO_BORDER, 0);
            badge.setBackgroundColor(BRAND);
            badge.setHorizontalAlignment(Element.ALIGN_CENTER);
            badge.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badge.setPhrase(new Phrase(String.valueOf(number), assets.bold(10, Color.WHITE)));
            heading.addCell(badge);
            PdfPCell label = cell(Rectangle.BOTTOM, 4);
            label.setBorderColor(RULE);
            label.setPaddingLeft(mm(2));
            label.setPhrase(new Phrase(text(title), assets.bold(12, INK)));
            heading.addCell(label);
            add(heading);
        }

        /** A small heading inside a section. */
        public void subheading(String value) {
            Paragraph p = new Paragraph(text(value), assets.bold(9.5f, INK));
            p.setSpacingBefore(mm(2));
            p.setSpacingAfter(mm(1));
            add(p);
        }

        /** Label/value pairs in two columns. */
        public void pairs(List<String[]> pairs) {
            PdfPTable table = new PdfPTable(new float[] {1.5f, 3.5f, 1.5f, 3.5f});
            table.setWidthPercentage(100);
            for (String[] pair : pairs) {
                table.addCell(labelCell(pair[0]));
                table.addCell(valueCell(pair[1], false));
            }
            if (pairs.size() % 2 == 1) {
                table.addCell(cell(Rectangle.NO_BORDER, 0));
                table.addCell(cell(Rectangle.NO_BORDER, 0));
            }
            add(table);
        }

        /** A table with a repeated header row and solid white rows, so the watermark never shows through data. */
        public void table(List<String> headers, List<List<String>> rows, float[] widths) {
            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String header : headers) {
                PdfPCell h = cell(Rectangle.BOX, 4);
                h.setBorderColor(RULE);
                h.setBackgroundColor(BAND);
                h.setPhrase(new Phrase(text(header), assets.bold(8, MUTED)));
                table.addCell(h);
            }
            for (List<String> row : rows) {
                for (String value : row) {
                    PdfPCell c = cell(Rectangle.BOX, 4);
                    c.setBorderColor(RULE);
                    c.setBackgroundColor(Color.WHITE);
                    c.setPhrase(new Phrase(text(PdfText.orEmpty(value)), assets.regular(9, INK)));
                    table.addCell(c);
                }
            }
            add(table);
        }

        public void note(String value) {
            Paragraph p = new Paragraph(text(value), assets.regular(8.5f, MUTED));
            p.setSpacingBefore(mm(2));
            add(p);
        }

        public void paragraph(String value) {
            Paragraph p = new Paragraph(text(value), assets.regular(10, INK));
            p.setSpacingBefore(mm(1));
            p.setLeading(14);
            add(p);
        }

        /** A bordered panel with a small caption, a large value and an optional status stamp. */
        public void amountPanel(String caption, String amount, String stamp) {
            PdfPTable table = new PdfPTable(new float[] {3f, 1f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(mm(4));
            table.setKeepTogether(true);
            PdfPCell left = cell(Rectangle.BOX, 10);
            left.setBorderColor(RULE);
            left.setBackgroundColor(Color.WHITE);
            left.addElement(new Paragraph(text(caption), assets.regular(8.5f, MUTED)));
            left.addElement(new Paragraph(text(amount), assets.bold(26, PRICE)));
            table.addCell(left);
            PdfPCell right = cell(Rectangle.BOX, 10);
            right.setBorderColor(RULE);
            right.setBackgroundColor(Color.WHITE);
            right.setHorizontalAlignment(Element.ALIGN_CENTER);
            right.setVerticalAlignment(Element.ALIGN_MIDDLE);
            right.setPhrase(new Phrase(text(stamp), assets.bold(16, INK)));
            table.addCell(right);
            add(table);
        }

        /** A ruled box with a caption underneath for the signature, with the signature image and the date when given. */
        public void signatureLine(String caption, byte[] signaturePng, String date) {
            PdfPTable table = new PdfPTable(new float[] {1f, 1f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(mm(8));
            table.setKeepTogether(true);
            PdfPCell signature = cell(Rectangle.BOTTOM, 4);
            signature.setBorderColor(MUTED);
            signature.setFixedHeight(mm(20));
            signature.setVerticalAlignment(Element.ALIGN_BOTTOM);
            if (signaturePng != null) {
                try {
                    Image image = Image.getInstance(signaturePng);
                    image.scaleToFit(mm(60), mm(16));
                    signature.addElement(image);
                }
                catch (IOException | DocumentException e) {
                    // an unusable image leaves the box empty for a handwritten signature
                }
            }
            table.addCell(signature);
            PdfPCell day = cell(Rectangle.BOTTOM, 4);
            day.setBorderColor(MUTED);
            day.setFixedHeight(mm(20));
            day.setVerticalAlignment(Element.ALIGN_BOTTOM);
            day.setPhrase(new Phrase(date == null ? "" : text(date), assets.regular(10, INK)));
            table.addCell(day);
            PdfPCell captionLeft = cell(Rectangle.NO_BORDER, 3);
            captionLeft.setPhrase(new Phrase(text(caption), assets.regular(8.5f, MUTED)));
            table.addCell(captionLeft);
            PdfPCell captionRight = cell(Rectangle.NO_BORDER, 3);
            captionRight.setPhrase(new Phrase("Date", assets.regular(8.5f, MUTED)));
            table.addCell(captionRight);
            add(table);
        }

        private PdfPCell labelCell(String label) {
            PdfPCell c = cell(Rectangle.NO_BORDER, 3);
            c.setPhrase(new Phrase(text(label), assets.regular(8, MUTED)));
            return c;
        }

        private PdfPCell valueCell(String value, boolean strong) {
            PdfPCell c = cell(Rectangle.NO_BORDER, 3);
            c.setPhrase(new Phrase(text(PdfText.orEmpty(value)), strong ? assets.bold(10, INK) : assets.regular(10, INK)));
            return c;
        }

        private PdfPCell cell(int border, float padding) {
            PdfPCell c = new PdfPCell();
            c.setBorder(border);
            c.setPadding(padding);
            return c;
        }

        private PdfPTable rule(Color color, float weight, float before, float after) {
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            line.setSpacingBefore(before);
            line.setSpacingAfter(after);
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(color);
            c.setBorderWidth(weight);
            c.setFixedHeight(0.5f);
            line.addCell(c);
            return line;
        }

        private String text(String value) {
            return PdfText.printable(value, assets::canDraw);
        }

        private void add(Element element) {
            try {
                doc.add(element);
            }
            catch (DocumentException e) {
                throw new IllegalStateException("could not lay out the PDF", e);
            }
        }
    }

    /** Watermark under the content and the company footer on every page. */
    private final class BrandedPages extends PdfPageEventHelper {

        private final String documentNumber;
        private PdfTemplate totalPages;

        private BrandedPages(String documentNumber) {
            this.documentNumber = documentNumber;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(mm(12), mm(5));
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            watermark(writer, document);
            footer(writer, document);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPages.beginText();
            totalPages.setFontAndSize(assets.regular(8, MUTED).getBaseFont(), 8);
            totalPages.setColorFill(MUTED);
            totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPages.endText();
        }

        private void watermark(PdfWriter writer, Document document) {
            try {
                Image mark = assets.logo();
                mark.scaleToFit(WATERMARK_WIDTH, WATERMARK_WIDTH);
                PdfContentByte under = writer.getDirectContentUnder();
                under.saveState();
                PdfGState state = new PdfGState();
                state.setFillOpacity(WATERMARK_OPACITY);
                under.setGState(state);
                float x = (document.getPageSize().getWidth() - mark.getScaledWidth()) / 2;
                float y = (document.getPageSize().getHeight() - mark.getScaledHeight()) / 2;
                under.addImage(mark, mark.getScaledWidth(), 0, 0, mark.getScaledHeight(), x, y);
                under.restoreState();
            }
            catch (DocumentException e) {
                throw new IllegalStateException("could not draw the watermark", e);
            }
        }

        private void footer(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float left = document.left();
            float right = document.right();
            canvas.saveState();
            canvas.setColorStroke(RULE);
            canvas.setLineWidth(0.6f);
            canvas.moveTo(left, FOOTER_RULE_Y);
            canvas.lineTo(right, FOOTER_RULE_Y);
            canvas.stroke();
            canvas.restoreState();

            BrandProperties.Contact contact = brand.contact();
            String address = contact.officeEn() == null ? "" : contact.officeEn();
            String reach = String.join("  ·  ", java.util.stream.Stream.concat(
                    contact.emails().stream(), java.util.stream.Stream.concat(
                            contact.phones().stream(), java.util.stream.Stream.of(brand.baseUrl()))).toList());
            float centre = (left + right) / 2;
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(PdfText.printable(brand.wordmark() + "  ·  " + address, assets::canDraw),
                            assets.regular(7.5f, MUTED)), centre, FOOTER_RULE_Y - mm(4), 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(PdfText.printable(reach, assets::canDraw), assets.regular(7.5f, MUTED)),
                    centre, FOOTER_RULE_Y - mm(7.5f), 0);

            String label = documentNumber + "   Page " + writer.getPageNumber() + " / ";
            float width = assets.regular(8, MUTED).getBaseFont().getWidthPoint(label, 8);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(label, assets.regular(8, MUTED)), right - width - mm(4), FOOTER_RULE_Y - mm(11.5f), 0);
            canvas.addTemplate(totalPages, right - mm(4), FOOTER_RULE_Y - mm(11.5f) - 1);
        }
    }
}
