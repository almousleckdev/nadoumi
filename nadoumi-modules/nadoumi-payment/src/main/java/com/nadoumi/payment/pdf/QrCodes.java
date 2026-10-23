package com.nadoumi.payment.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.Map;

/** Encodes text as a QR module matrix; the PDF draws it as crisp vector squares. */
final class QrCodes {

    private static final int QUIET_ZONE_MODULES = 0; // the page margin around the code is the quiet zone

    private QrCodes() {
    }

    static BitMatrix matrix(String text) {
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, QUIET_ZONE_MODULES,
                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        }
        catch (WriterException e) {
            throw new IllegalStateException("could not encode the verification code", e);
        }
    }
}
