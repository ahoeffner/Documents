package ai.dochandler.services;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


@Service
public class OCRService
{
    @Value("${app.tesseract.language}")
    private String language;


    public String scan(byte[] content) throws Exception
    {
        Path input = Files.createTempFile("ocr-", ".img");
        try
        {
            Files.write(input, content);
            ProcessBuilder pb = new ProcessBuilder("tesseract", input.toString(), "stdout", "-l", language);
            Process proc = pb.start();
            String text = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = proc.waitFor();
            if (exit != 0) throw new Exception("tesseract failed with code " + exit);
            return(text);
        }
        finally
        {
            Files.deleteIfExists(input);
        }
    }
}
