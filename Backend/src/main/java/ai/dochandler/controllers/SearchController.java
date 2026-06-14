package ai.dochandler.controllers;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;
import ai.dochandler.entities.Document;
import ai.dochandler.model.DocumentRecord;
import ai.dochandler.entities.SearchRequest;
import ai.dochandler.services.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.dochandler.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/api/search")
public class SearchController
{
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    @Value("${app.vector-search.boundary[0]}")
    private double boundaryLow;

    @Value("${app.vector-search.boundary[1]}")
    private double boundaryHigh;

    private final GeminiService geminiService;
    private final DocumentRepository documentRepo;
    private final ObjectMapper mapper;


    public SearchController(GeminiService geminiService, DocumentRepository documentRepo, ObjectMapper mapper)
    {
        this.geminiService = geminiService;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
    }


    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extract(@RequestBody Map<String, Object> body)
    {
        String query = (String) body.getOrDefault("query", "");
        boolean languageIndependent = !Boolean.FALSE.equals(body.get("languageIndependent"));
        try
        {
            JsonNode preprocessed = geminiService.preprocessQuery(query, languageIndependent);
            String[] lexical = mapper.convertValue(preprocessed.path("lexical"), String[].class);
            if (lexical == null) lexical = new String[0];
            String terms;
            if (lexical.length == 0) terms = query;
            else if (languageIndependent) terms = Arrays.stream(lexical).map(t -> "(" + t + ")").collect(Collectors.joining(" "));
            else terms = String.join(" ", lexical);
            return(ResponseEntity.ok(Map.of("success", true, "terms", terms)));
        }
        catch (Exception e)
        {
            log.warn("Search term extraction failed, returning original query: {}", e.getMessage());
            return(ResponseEntity.ok(Map.of("success", true, "terms", query)));
        }
    }


    @PostMapping
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequest req)
    {
        String query = req.query() != null ? req.query() : "";
        String[] words = query.toLowerCase().split("\\s+");

        List<DocumentRecord> docs;
        if (req.languageIndependent())
        {
            try
            {
                JsonNode preprocessed = geminiService.preprocessQuery(query, true);
                String semantic = preprocessed.path("semantic").asText();
                String[] lexical = mapper.convertValue(preprocessed.path("lexical"), String[].class);
                if (lexical == null) lexical = new String[0];

                double threshold = boundaryLow + 0.5 * (boundaryHigh - boundaryLow);
                docs = documentRepo.hybridSearch(semantic, lexical, req.folder(), threshold, geminiService);
            }
            catch (Exception e)
            {
                log.warn("Search preprocessing failed, falling back to lexical search: {}", e.getMessage());
                docs = documentRepo.lexicalSearch(words, req.folder());
            }
        }
        else
        {
            docs = documentRepo.lexicalSearch(words, req.folder());
        }

        List<Document> documents = docs.stream()
            .map(d -> new Document(
                String.valueOf(d.getId()),
                d.getDate() != null ? d.getDate().toString() : "",
                d.getTitle(),
                d.getFile(),
                d.getText(),
                d.getContent() != null,
                d.getFldid(),
                false,
                null
            ))
            .toList();

        return(ResponseEntity.ok(Map.of("success", true, "documents", documents)));
    }
}
