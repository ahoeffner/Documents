package ai.dochandler.controllers;

import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.dochandler.entities.Document;
import jakarta.servlet.http.HttpSession;
import ai.dochandler.model.DocumentRecord;
import ai.dochandler.services.GeminiService;
import ai.dochandler.entities.CreateResponse;
import ai.dochandler.entities.DocumentDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ai.dochandler.services.FileProcessorService;
import ai.dochandler.repository.DocumentRepository;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/documents")
public class DocumentController
{
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final FileProcessorService processor;
    private final DocumentRepository documentRepo;
    private final GeminiService geminiService;


    public DocumentController(FileProcessorService processor, DocumentRepository documentRepo, GeminiService geminiService)
    {
        this.processor = processor;
        this.documentRepo = documentRepo;
        this.geminiService = geminiService;
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long fldid, @RequestParam(required = false) String q)
    {
        List<Document> documents = documentRepo.findAll(fldid, q).stream()
            .map(this::toDocument)
            .toList();
        return(ResponseEntity.ok(Map.of("success", true, "documents", documents)));
    }


    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id)
    {
        DocumentRecord record = documentRepo.findById(id);
        if (record == null) return(ResponseEntity.notFound().build());
        return(ResponseEntity.ok(Map.of("success", true, "document", toDetail(record))));
    }


    @PutMapping("/{id}")
    public ResponseEntity<CreateResponse> update
    (
        @PathVariable long id,
        @RequestParam(required = false) String date,
        @RequestParam(required = false) String fldid,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String text,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) MultipartFile file,
        @RequestParam(required = false) String url,
        HttpSession session
    )
    {
        if (!Boolean.TRUE.equals(session.getAttribute("admin")))
            return(ResponseEntity.status(403).body(new CreateResponse(false, null)));

        try
        {
            DocumentRecord existing = documentRepo.findByIdWithLang(id);
            if (existing == null) return(ResponseEntity.notFound().build());

            String effectiveDate  = date     != null ? date     : (existing.getDate() != null ? existing.getDate().toString() : null);
            String effectiveFldid = fldid    != null ? fldid    : String.valueOf(existing.getFldid());
            String effectiveTitle = title    != null ? title    : existing.getTitle();
            String effectiveLang  = language != null ? language : existing.getLang();

            boolean reprocess = (text != null && !text.isBlank())
                || (file != null && !file.isEmpty())
                || (url  != null && !url.isBlank());

            DocumentRecord record;
            if (reprocess)
            {
                String effectiveText = text != null ? text : existing.getText();
                record = processor.process(effectiveDate, effectiveFldid, effectiveTitle, effectiveText, effectiveLang, file, url, false);
            }
            else
            {
                record = new DocumentRecord();
                record.setDate(effectiveDate);
                record.setFldid(effectiveFldid);
                record.setTitle(effectiveTitle);
                record.setText(existing.getText());
                record.setLang(effectiveLang);
            }

            boolean success = documentRepo.update(id, record, geminiService);
            if (!success) return(ResponseEntity.notFound().build());
            return(ResponseEntity.ok(new CreateResponse(true, id)));
        }
        catch (Exception e)
        {
            logger.error("Failed to update document {}", id, e);
            return(ResponseEntity.internalServerError().body(new CreateResponse(false, null)));
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable long id, HttpSession session)
    {
        if (!Boolean.TRUE.equals(session.getAttribute("admin")))
            return(ResponseEntity.status(403).body(Map.of("success", false, "message", "Admin required")));

        boolean success = documentRepo.deleteById(id);
        if (!success) return(ResponseEntity.notFound().build());
        return(ResponseEntity.ok(Map.of("success", true)));
    }


    private Document toDocument(DocumentRecord d)
    {
        return(new Document(
            String.valueOf(d.getId()),
            d.getDate() != null ? d.getDate().toString() : "",
            d.getTitle(),
            d.getFile(),
            d.getText(),
            d.getContent() != null,
            d.getFldid(),
            false,
            null
        ));
    }


    private DocumentDetail toDetail(DocumentRecord d)
    {
        String extref = d.getFile();
        boolean isUrl = extref != null && (extref.startsWith("http://") || extref.startsWith("https://"));
        return(new DocumentDetail(
            String.valueOf(d.getId()),
            d.getDate() != null ? d.getDate().toString() : "",
            d.getTitle(),
            d.getText(),
            d.getFldid(),
            isUrl ? null : extref,
            d.getContent() != null,
            isUrl ? extref : null
        ));
    }
}
