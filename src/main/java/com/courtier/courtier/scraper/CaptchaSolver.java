package com.courtier.courtier.scraper;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class CaptchaSolver {

    private final Tesseract tesseract;

    public CaptchaSolver() {
        tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
        tesseract.setLanguage("eng");
        tesseract.setVariable("tessedit_char_whitelist",
                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        tesseract.setPageSegMode(7);
        tesseract.setOcrEngineMode(1);
    }

    public String solve(InputStream captchaImageStream) throws IOException, TesseractException {
        BufferedImage original = ImageIO.read(captchaImageStream);
        BufferedImage processed = preprocess(original);
        String result = tesseract.doOCR(processed);
        return result.replaceAll("\\s+", "").trim();
    }

    private BufferedImage preprocess(BufferedImage image) {
        // scale up — tesseract works better on larger images
        int scale = 3;
        BufferedImage scaled = new BufferedImage(
                image.getWidth() * scale,
                image.getHeight() * scale,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        g.dispose();

        // convert to grayscale + threshold
        BufferedImage result = new BufferedImage(
                scaled.getWidth(), scaled.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();

        // binarize — pixels below threshold become black (text), above become white
        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                int pixel = result.getRGB(x, y) & 0xFF;
                result.setRGB(x, y, pixel < 128 ? 0x000000 : 0xFFFFFF);
            }
        }
        return result;
    }
}