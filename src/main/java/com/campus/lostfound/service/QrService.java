package com.campus.lostfound.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Generates QR codes (ZXing) that encode the claim verification token.
 * At handover, the finder scans this QR - or types the code on the
 * Verify page - to confirm the item was returned to its verified owner.
 */
@Service
public class QrService {

    public byte[] qrPng(String content, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate QR code", e);
        }
    }
}
