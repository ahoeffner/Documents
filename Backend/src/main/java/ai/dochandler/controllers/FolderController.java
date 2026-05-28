package ai.dochandler.controllers;

import java.util.Map;
import java.util.List;
import ai.dochandler.entities.Folder;
import ai.dochandler.entities.Document;
import ai.dochandler.services.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ai.dochandler.repository.DocumentRepository;


@RestController
@RequestMapping("/api/folders")
public class FolderController
{
    private final FolderService service;
    private final DocumentRepository documentRepo;


    public FolderController(FolderService service, DocumentRepository documentRepo)
    {
        this.service = service;
        this.documentRepo = documentRepo;
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> list()
    {
        List<Folder> folders = service.list();
        return(ResponseEntity.ok(Map.of("success", true, "folders", folders)));
    }


    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body)
    {
        String name = (String) body.get("name");
        Long pid = body.get("pid") != null ? ((Number) body.get("pid")).longValue() : null;
        long id = service.create(name, pid);
        return(ResponseEntity.ok(Map.of("success", true, "id", id)));
    }


    @GetMapping("/{id}/documents")
    public ResponseEntity<Map<String, Object>> documents(@PathVariable long id)
    {
        if (!service.existsById(id)) return(ResponseEntity.notFound().build());
        List<Document> documents = documentRepo.findAll(id).stream()
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


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable long id)
    {
        boolean success = service.deleteById(id);
        return(ResponseEntity.ok(Map.of("success", success)));
    }
}
