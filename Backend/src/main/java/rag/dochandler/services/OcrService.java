package rag.dochandler.services;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;


@Service
public class OcrService
{
    public String scan(byte[] content) throws Exception
    {
        Tesseract scanner = new Tesseract();
        scanner.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        scanner.setLanguage("dan");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        return(scanner.doOCR(image));
    }
}
