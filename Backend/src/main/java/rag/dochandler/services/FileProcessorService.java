package rag.dochandler.services;

import rag.dochandler.model.DocumentRecord;
import dev.langchain4j.data.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class FileProcessorService
{
    private final ChunkerService chunker;
    private final OcrService ocr;
    private final DocumentLoaderService loader;


    public FileProcessorService(ChunkerService chunker, OcrService ocr, DocumentLoaderService loader)
    {
        this.chunker = chunker;
        this.ocr = ocr;
        this.loader = loader;
    }


    public DocumentRecord process(String date, String catidStr, String title, String text,
                                  String language, MultipartFile file, String url) throws Exception
    {
        DocumentRecord doc = new DocumentRecord();
        doc.setDate(date);
        doc.setCatid(catidStr);
        doc.setLang(language);

        if (title != null && !title.isBlank())
        {
            doc.setTitle(title);
            doc.addTextChunk("Title: " + title);
        }

        if (text != null && !text.isBlank())
        {
            doc.addTextChunks(chunker.split(text));
            doc.setText(text);
        }

        if (file != null && !file.isEmpty())
        {
            byte[] bytes = file.getBytes();
            doc.addTextChunk("Filename: " + file.getOriginalFilename());

            if (isImage(bytes))
            {
                if (doc.getText() == null)
                {
                    String ocrText = ocr.scan(bytes);
                    doc.addTextChunks(chunker.split(ocrText));
                    doc.setText(ocrText);
                }
            }
            else
            {
                Document parsed = loader.load(bytes);
                doc.addTextChunks(chunker.split(parsed));
            }
            doc.setFile(file.getOriginalFilename());
            doc.setContent(bytes);
        }

        if (url != null && !url.isBlank())
        {
            DocumentLoaderService.URLDocumentResult result = loader.loadURL(url);
            doc.addTextChunks(chunker.split(result.document));
            doc.setFile(url);
            doc.setContent(result.content);
        }

        return(doc);
    }


    private boolean isImage(byte[] b)
    {
        if (b == null || b.length < 8) return(false);
        if (b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF) return(true);
        if (b[0] == (byte) 0x89 && b[1] == (byte) 0x50 && b[2] == (byte) 0x4E && b[3] == (byte) 0x47 &&
            b[4] == (byte) 0x0D && b[5] == (byte) 0x0A && b[6] == (byte) 0x1A && b[7] == (byte) 0x0A) return(true);
        if (b[0] == (byte) 0x47 && b[1] == (byte) 0x49 && b[2] == (byte) 0x46 && b[3] == (byte) 0x38 &&
            (b[4] == (byte) 0x37 || b[4] == (byte) 0x39) && b[5] == (byte) 0x61) return(true);
        if ((b[0] == (byte) 0x49 && b[1] == (byte) 0x49) || (b[0] == (byte) 0x4D && b[1] == (byte) 0x4D)) return(true);
        return(false);
    }
}
