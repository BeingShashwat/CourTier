package com.courtier.courtier.scraper;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Component
public class CaptchaSolver {
    private final Tesseract tesseract;

    public CaptchaSolver() {
        tesseract = new Tesseract();
        tesseract.setDatapath("usr/share/tesseract-ocr/5/tessdata");
        tesseract.setLanguage("eng");
        tesseract.setVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        tesseract.setPageSegMode(7);
        tesseract.setOcrEngineMode(1);
    }

    public String solve(InputStream captchaImageStream) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(captchaImageStream);
        String result = tesseract.doOCR(image);
        return result.replaceAll("\\s+", "").trim();
    }
}
