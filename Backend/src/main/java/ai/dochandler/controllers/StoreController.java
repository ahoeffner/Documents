package ai.dochandler.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;
import ai.dochandler.model.DocumentRecord;
import ai.dochandler.services.GeminiService;
import ai.dochandler.entities.CreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ai.dochandler.services.FileProcessorService;
import ai.dochandler.repository.DocumentRepository;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/store")
public class StoreController
{
    private static final Logger log = LoggerFactory.getLogger(StoreController.class);

    private final FileProcessorService processor;
    private final DocumentRepository documentRepo;
    private final GeminiService geminiService;


    public StoreController(FileProcessorService processor, DocumentRepository documentRepo, GeminiService geminiService)
    {
        this.processor = processor;
        this.documentRepo = documentRepo;
        this.geminiService = geminiService;
    }


    @PostMapping
    public ResponseEntity<CreateResponse> store
    (
        @RequestParam String date,
        @RequestParam String fldid,
        @RequestParam String title,
        @RequestParam(required = false) String text,
        @RequestParam(required = false) MultipartFile file,
        @RequestParam(required = false) String url,
        @RequestParam(required = false, defaultValue = "false") boolean noExtract,
        HttpSession session
    )
    {
        if (!Boolean.TRUE.equals(session.getAttribute("admin")))
            return(ResponseEntity.status(403).body(new CreateResponse(false, null)));

        try
        {
            DocumentRecord record = processor.process(date, fldid, title, text, file, url, noExtract);
            long id = documentRepo.create(record, geminiService);
            return(ResponseEntity.ok(new CreateResponse(true, id)));
        }
        catch (IllegalStateException e)
        {
            return(ResponseEntity.badRequest().body(new CreateResponse(false, null, e.getMessage())));
        }
        catch (Exception e)
        {
            log.error("Failed to store document", e);
            return(ResponseEntity.internalServerError().body(new CreateResponse(false, null)));
        }
    }
}
