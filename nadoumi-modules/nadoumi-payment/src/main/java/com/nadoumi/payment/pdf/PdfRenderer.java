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
    private static final float LEADING_FACTOR = 1.4f;
    private static final float QR_SIZE_MM = 17;
    private static final int MAX_PAGES = 30;

    private final PdfAssets assets;
    private final BrandProperties brand;

    public PdfRenderer(PdfAssets assets, BrandProperties brand) {
        this.assets = assets;
        this.brand = brand;
    }

    /** Writes a document: {@code body} adds content to the {@link Page}. */
    public byte[] render(String title, String documentNumber, String verifyUrl, Consumer<Page> body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, SIDE_MARGIN, SIDE_MARGIN, TOP_MARGIN, BOTTOM_MARGIN);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new BrandedPages(documentNumber, verifyUrl));
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
            PdfPTable table = new PdfPTable(withPhoto ? new float[] {1.0f, 5.0f, 1.2f} : new float[] {1.1f, 5.9f});
            table.setWidthPercentage(100);
            Image logo = assets.logo();
            logo.scaleToFit(mm(22), mm(22));
            PdfPCell logoCell = cell(Rectangle.NO_BORDER, 0);
            logoCell.addElement(logo);
            table.addCell(logoCell);

            PdfPCell titleCell = cell(Rectangle.NO_BORDER, 0);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleCell.addElement(paragraph(title, 20, true, INK));
            String zhTitle = PdfLabels.zh(title);
            titleCell.addElement(paragraph((zhTitle == null ? "" : zhTitle + "  \u00b7  ") + "Nadoumi Education Consulting", 9, false, MUTED));
            table.addCell(titleCell);
            if (withPhoto) {
                table.addCell(photoCell(photoJpeg));
            }
            add(table);

            // the references sit in their own full-width grid under the masthead, so nothing can run under the photo:
            // short ones share a row, the last (the fee line, with its transaction number) gets a row of its own
            PdfPTable refs = new PdfPTable(references.size() > 1 ? references.size() - 1 : 1);
            refs.setWidthPercentage(100);
            refs.setSpacingBefore(mm(2));
            int last = references.size() - 1;
            for (int i = 0; i < references.size(); i++) {
                String[] reference = references.get(i);
                PdfPCell cell = cell(Rectangle.NO_BORDER, 1.5f);
                if (i == last && last > 0) {
                    cell.setColspan(refs.getNumberOfColumns());
                }
                Paragraph line = new Paragraph();
                line.add(phrase(reference[0], 8, false, MUTED));
                line.add(new com.lowagie.text.Chunk("   ", assets.regular(8, MUTED)));
                line.add(phrase(reference[1], 9, true, INK));
                cell.addElement(line);
                refs.addCell(cell);
            }
            add(refs);
            add(rule(BRAND, 1.2f, mm(3), mm(5)));
        }

        private PdfPCell photoCell(byte[] jpeg) {
            PdfPCell cell = cell(Rectangle.BOX, 2);
            cell.setBorderColor(RULE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            try {
                Image photo = Image.getInstance(jpeg);
                photo.scaleToFit(mm(22), mm(29));
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
            label.setPhrase(phrase(label(title), 12, true, INK));
            heading.addCell(label);
            add(heading);
        }

        /** English text, followed by its Chinese translation on the next line when there is one. */
        private String bilingualBlock(String english) {
            String zh = PdfLabels.zh(english);
            return zh == null ? english : english + "\n" + zh;
        }

        /** A small heading inside a section. */
        public void subheading(String value) {
            Paragraph p = paragraph(label(value), 9.5f, true, INK);
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
                h.setPhrase(phrase(label(header), 8, true, MUTED));
                table.addCell(h);
            }
            for (List<String> row : rows) {
                for (String value : row) {
                    PdfPCell c = cell(Rectangle.BOX, 4);
                    c.setBorderColor(RULE);
                    c.setBackgroundColor(Color.WHITE);
                    c.setPhrase(phrase(PdfText.orEmpty(value), 9, false, INK));
                    table.addCell(c);
                }
            }
            add(table);
        }

        public void note(String value) {
            Paragraph p = paragraph(bilingualBlock(value), 8.5f, false, MUTED);
            p.setSpacingBefore(mm(2));
            add(p);
        }

        public void paragraph(String value) {
            Paragraph p = paragraph(bilingualBlock(value), 10, false, INK);
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
            left.addElement(paragraph(label(caption), 8.5f, false, MUTED));
            left.addElement(paragraph(amount, 26, true, PRICE));
            table.addCell(left);
            PdfPCell right = cell(Rectangle.BOX, 10);
            right.setBorderColor(RULE);
            right.setBackgroundColor(Color.WHITE);
            right.setHorizontalAlignment(Element.ALIGN_CENTER);
            right.setVerticalAlignment(Element.ALIGN_MIDDLE);
            right.setPhrase(phrase(label(stamp), 16, true, INK));
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
            day.setPhrase(phrase(date == null ? "" : date, 10, false, INK));
            table.addCell(day);
            PdfPCell captionLeft = cell(Rectangle.NO_BORDER, 3);
            captionLeft.setPhrase(phrase(label(caption), 8.5f, false, MUTED));
            table.addCell(captionLeft);
            PdfPCell captionRight = cell(Rectangle.NO_BORDER, 3);
            captionRight.setPhrase(phrase(label("Date"), 8.5f, false, MUTED));
            table.addCell(captionRight);
            add(table);
        }

        private PdfPCell labelCell(String label) {
            PdfPCell c = cell(Rectangle.NO_BORDER, 3);
            String zh = PdfLabels.zh(label);
            c.setPhrase(phrase(zh == null ? label : label + "\n" + zh, 8, false, MUTED));
            return c;
        }

        private PdfPCell valueCell(String value, boolean strong) {
            PdfPCell c = cell(Rectangle.NO_BORDER, 3);
            c.setPhrase(phrase(PdfText.orEmpty(value), 10, strong, INK));
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

        /** A fixed word of the document, with its Chinese translation when there is one. */
        private String label(String english) {
            return PdfLabels.bilingual(english);
        }

        private Phrase phrase(String value, float size, boolean bold, Color color) {
            return assets.phrase(text(value), size, bold, color);
        }

        private Paragraph paragraph(String value, float size, boolean bold, Color color) {
            Paragraph p = new Paragraph(phrase(value, size, bold, color));
            p.setLeading(size * LEADING_FACTOR);
            return p;
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
        private final String verifyUrl;
        private PdfTemplate totalPages;

        private BrandedPages(String documentNumber, String verifyUrl) {
            this.documentNumber = documentNumber;
            this.verifyUrl = verifyUrl;
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

        /** A small QR code at the foot of the page that opens the public page confirming Nadoumi issued this document. */
        private void verificationCode(PdfContentByte canvas, float left) {
            com.google.zxing.common.BitMatrix matrix = QrCodes.matrix(verifyUrl);
            float side = mm(QR_SIZE_MM);
            float module = side / matrix.getWidth();
            float x0 = left;
            float y0 = FOOTER_RULE_Y - mm(2) - side;
            canvas.saveState();
            canvas.setColorFill(INK);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        canvas.rectangle(x0 + x * module, y0 + (matrix.getHeight() - 1 - y) * module, module + 0.05f, module + 0.05f);
                    }
                }
            }
            canvas.fill();
            canvas.restoreState();
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    assets.phrase(PdfLabels.bilingual("Verify this document"), 6.5f, false, MUTED),
                    x0 + side + mm(1.5f), y0 + side / 2, 0);
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

            if (verifyUrl != null) {
                verificationCode(canvas, left);
            }

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
