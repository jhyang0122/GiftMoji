package com.giftmoji.giftmoji.voucher;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Service
public class QrCodeService {

	private static final int DEFAULT_SIZE_PX = 320;

	public byte[] generatePng(String payload) {
		try {
			BitMatrix matrix = new QRCodeWriter().encode(
					payload, BarcodeFormat.QR_CODE, DEFAULT_SIZE_PX, DEFAULT_SIZE_PX);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(matrix, "PNG", out);
			return out.toByteArray();
		} catch (WriterException e) {
			throw new IllegalArgumentException("Could not encode QR payload", e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
