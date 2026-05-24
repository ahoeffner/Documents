package rag.dochandler.controllers;

import rag.dochandler.model.DocumentRecord;
import rag.dochandler.services.GeminiService;
import rag.dochandler.entities.CreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rag.dochandler.services.FileProcessorService;
import rag.dochandler.repository.DocumentRepository;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/store")
public class StoreController
{
    private final FileProcessorService processor;
    private final DocumentRepository documentRepo;
    private final GeminiService geminiService;


    public StoreController(FileProcessorService processor, DocumentRepository documentRepo,
                           GeminiService geminiService)
    {
        this.processor = processor;
        this.documentRepo = documentRepo;
        this.geminiService = geminiService;
    }


    @PostMapping
    public ResponseEntity<CreateResponse> store(
            @RequestParam String date,
            @RequestParam String catid,
            @RequestParam String title,
            @RequestParam(required = false) String text,
            @RequestParam String language,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String url)
    {
        try
        {
            DocumentRecord record = processor.process(date, catid, title, text, language, file, url);
            long id = documentRepo.create(record, geminiService);
            return(ResponseEntity.ok(new CreateResponse(true, id)));
        }
        catch (Exception e)
        {
            return(ResponseEntity.internalServerError().body(new CreateResponse(false, null)));
        }
    }
}
