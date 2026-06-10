package ai.dochandler.services;

import java.net.URI;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Files;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.stream.Stream;
import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParserDecorator;
import dev.langchain4j.data.document.Document;
import org.apache.tika.parser.image.ImageParser;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;


@Service
public class DocumentLoaderService
{
    /*
     * GraalVM native image has no AWT/libawt.so, so Tika's ImageParser
     * (used for embedded images) crashes with UnsatisfiedLinkError.
     * Strip image types from the parser so embedded images are skipped.
     */
    private static final Parser TIKA_PARSER = ParserDecorator.withoutTypes(new AutoDetectParser(), new ImageParser().getSupportedTypes(new ParseContext()));


    private final OCRService ocr;

    @Value("${app.tesseract.pdf-dpi}")
    private int pdfDpi;


    public DocumentLoaderService(OCRService ocr)
    {
        this.ocr = ocr;
    }


    private static ApacheTikaDocumentParser tikaParser()
    {
        return(new ApacheTikaDocumentParser(
            () -> TIKA_PARSER,
            ApacheTikaDocumentParser.DEFAULT_CONTENT_HANDLER_SUPPLIER,
            ApacheTikaDocumentParser.DEFAULT_METADATA_SUPPLIER,
            () ->
            {
                ParseContext context = new ParseContext();
                context.set(Parser.class, TIKA_PARSER);
                return(context);
            }));
    }



    public static class URLDocumentResult
    {
        public final byte[] content;
        public final Document document;


        public URLDocumentResult(byte[] content, Document document)
        {
            this.content = content;
            this.document = document;
        }
    }


    public Document load(byte[] content) throws Exception
    {
        if (isPdf(content)) return(parsePdf(content));
        return(tikaParser().parse(new ByteArrayInputStream(content)));
    }


    public URLDocumentResult loadURL(String url) throws Exception
    {
        URI uri = new URI(url);
        URLConnection conn = uri.toURL().openConnection();
        InputStream in = conn.getInputStream();
        byte[] content = in.readAllBytes();
        Document doc = isPdf(content) ? parsePdf(content) : tikaParser().parse(new ByteArrayInputStream(content));
        return(new URLDocumentResult(content, doc));
    }


    private static boolean isPdf(byte[] b)
    {
        return(b.length >= 4 && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46);
    }


    private Document parsePdf(byte[] content) throws Exception
    {
        try
        {
            Process proc = new ProcessBuilder("pdftotext", "-", "-").start();
            try (OutputStream out = proc.getOutputStream()) { out.write(content); }
            String text = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            proc.waitFor();
            if (text.isBlank()) return(Document.from(ocrPdf(content)));
            return(Document.from(text));
        }
        catch (IOException e)
        {
            return(tikaParser().parse(new ByteArrayInputStream(content)));
        }
    }


    private String ocrPdf(byte[] content) throws Exception
    {
        Path pdfFile = Files.createTempFile("ocr-", ".pdf");
        Path pageDir = Files.createTempDirectory("ocr-pages-");

        try
        {
            Files.write(pdfFile, content);

            Process proc = new ProcessBuilder("pdftoppm", "-png", "-r", String.valueOf(pdfDpi), pdfFile.toString(), pageDir.resolve("page").toString()).start();
            proc.waitFor();

            List<Path> pages;
            try (Stream<Path> files = Files.list(pageDir))
            {
                pages = files.sorted().collect(Collectors.toList());
            }

            StringBuilder text = new StringBuilder();
            for (Path page : pages)
                text.append(ocr.scan(Files.readAllBytes(page))).append('\n');

            if (text.toString().isBlank())
                throw new IllegalStateException("No text could be extracted from this PDF. Please provide a detailed description of the document.");

            return(text.toString());
        }
        finally
        {
            Files.deleteIfExists(pdfFile);
            try (Stream<Path> files = Files.list(pageDir))
            {
                for (Path page : files.collect(Collectors.toList())) Files.deleteIfExists(page);
            }
            Files.deleteIfExists(pageDir);
        }
    }
}
