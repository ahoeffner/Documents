package ai.dochandler.services;

import java.util.List;
import java.util.ArrayList;
import ai.dochandler.entities.Document;
import ai.dochandler.entities.ChatRequest;
import ai.dochandler.model.DocumentRecord;
import ai.dochandler.entities.ChatResponse;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.dochandler.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;


@Service
public class ChatService
{
    @Value("${app.vector-search.boundary[0]}")
    private double boundaryLow;

    @Value("${app.vector-search.boundary[1]}")
    private double boundaryHigh;

    private final GeminiService geminiService;
    private final DocumentRepository documentRepo;
    private final ObjectMapper mapper;


    public ChatService(GeminiService geminiService, DocumentRepository documentRepo, ObjectMapper mapper)
    {
        this.geminiService = geminiService;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
    }


    public ChatResponse chat(ChatRequest req) throws Exception
    {
        JsonNode preprocessed = geminiService.preprocess(req.id(), req.query());

        String semantic = preprocessed.path("semantic").asText();
        String[] lexical = mapper.convertValue(preprocessed.path("lexical"), String[].class);

        boolean hasTerms = !semantic.isBlank() || lexical.length > 0;

        List<DocumentRecord> docs = List.of();
        if (hasTerms)
        {
            double match = req.match() / 100.0;
            double threshold = boundaryLow + (1.0 - match) * (boundaryHigh - boundaryLow);
            docs = documentRepo.hybridSearch(semantic, lexical, req.folder(), threshold, geminiService);
        }

        StringBuilder context = new StringBuilder();
        List<Document> documents = new ArrayList<>();
        for (DocumentRecord d : docs)
        {
            if (d.getDate() != null)
                context.append("date: ").append(d.getDate().toString().replace('-', '/')).append("\n");
            context.append(documentRepo.getAllText(d.getId())).append("\n");
            documents.add(toDocument(d));
        }

        JsonNode ragResult = geminiService.ragQuery(req.id(), req.query(), context.toString());

        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setResponse(ragResult.get("response").asText());
        response.setDocuments(documents);
        response.setRefresh(false);
        return(response);
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
}
