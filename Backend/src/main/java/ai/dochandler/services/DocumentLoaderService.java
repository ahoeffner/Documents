package ai.dochandler.services;

import java.net.URI;
import java.io.InputStream;
import java.net.URLConnection;
import java.io.ByteArrayInputStream;
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
        return(new ApacheTikaDocumentParser().parse(new ByteArrayInputStream(content)));
    }


    public URLDocumentResult loadURL(String url) throws Exception
    {
        URI uri = new URI(url);
        URLConnection conn = uri.toURL().openConnection();
        InputStream in = conn.getInputStream();
        byte[] content = in.readAllBytes();
        Document doc = new ApacheTikaDocumentParser().parse(new ByteArrayInputStream(content));
        return(new URLDocumentResult(content, doc));
    }
}
