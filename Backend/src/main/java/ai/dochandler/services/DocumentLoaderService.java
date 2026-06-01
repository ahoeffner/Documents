package ai.dochandler.services;

import java.net.URI;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import dev.langchain4j.data.document.Document;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;


@Service
public class DocumentLoaderService
{
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
        return(new ApacheTikaDocumentParser().parse(new ByteArrayInputStream(content)));
    }


    public URLDocumentResult loadURL(String url) throws Exception
    {
        URI uri = new URI(url);
        URLConnection conn = uri.toURL().openConnection();
        InputStream in = conn.getInputStream();
        byte[] content = in.readAllBytes();
        Document doc = isPdf(content) ? parsePdf(content) : new ApacheTikaDocumentParser().parse(new ByteArrayInputStream(content));
        return(new URLDocumentResult(content, doc));
    }


    private static boolean isPdf(byte[] b)
    {
        return(b.length >= 4 && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46);
    }


    private static Document parsePdf(byte[] content) throws Exception
    {
        Process proc = new ProcessBuilder("pdftotext", "-", "-").start();

        try (OutputStream out = proc.getOutputStream())
        {
            out.write(content);
        }

        String text = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        proc.waitFor();
        return(Document.from(text));
    }
}
