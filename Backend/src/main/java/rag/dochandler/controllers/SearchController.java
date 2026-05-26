package rag.dochandler.controllers;

import java.util.Map;
import java.util.List;
import rag.dochandler.entities.Document;
import rag.dochandler.model.DocumentRecord;
import rag.dochandler.entities.SearchRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rag.dochandler.repository.DocumentRepository;


@RestController
@RequestMapping("/api/search")
public class SearchController
{
    private final DocumentRepository documentRepo;


    public SearchController(DocumentRepository documentRepo)
    {
        this.documentRepo = documentRepo;
    }


    @PostMapping
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequest req)
    {
        String query = req.query() != null ? req.query() : "";
        String[] words = query.toLowerCase().split("\\s+");

        List<DocumentRecord> docs = documentRepo.lexicalSearch(words, req.folder());

        List<Document> documents = docs.stream()
            .map(d -> new Document(
                String.valueOf(d.getId()),
                d.getDate() != null ? d.getDate().toString() : "",
                d.getTitle(),
                d.getFile(),
                d.getText(),
                d.getContent() != null,
                d.getFldid()
            ))
            .toList();

        return(ResponseEntity.ok(Map.of("success", true, "documents", documents)));
    }
}
