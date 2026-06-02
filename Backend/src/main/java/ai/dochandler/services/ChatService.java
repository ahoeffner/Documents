package ai.dochandler.services;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import ai.dochandler.model.Folder;
import ai.dochandler.entities.Document;
import ai.dochandler.model.DocumentRecord;
import ai.dochandler.entities.ChatRequest;
import ai.dochandler.entities.ChatResponse;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import ai.dochandler.repository.FolderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.dochandler.repository.DocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;


@Service
public class ChatService
{
    @Value("${app.vector-search.boundary[0]}")
    private double boundaryLow;

    @Value("${app.vector-search.boundary[1]}")
    private double boundaryHigh;

    private final GeminiService geminiService;
    private final FolderRepository folderRepo;
    private final DocumentRepository documentRepo;
    private final ObjectMapper mapper;


    public ChatService(GeminiService geminiService, FolderRepository folderRepo, DocumentRepository documentRepo, ObjectMapper mapper)
    {
        this.geminiService = geminiService;
        this.folderRepo = folderRepo;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
    }


    public ChatResponse chat(ChatRequest req) throws Exception
    {
        JsonNode preprocessed = geminiService.preprocess(req.id(), req.query());

        if (preprocessed.has("function"))
        {
            String func = preprocessed.get("function").asText();
            Map<String, Object> args = mapper.convertValue(
                preprocessed.get("args"),
                new TypeReference<>() {}
            );
            return(dispatchFunction(req.id(), func, args));
        }

        String semantic = preprocessed.get("semantic").asText();
        String[] lexical = mapper.convertValue(preprocessed.get("lexical"), String[].class);

        double match = req.match() / 100.0;
        double threshold = boundaryLow + (1.0 - match) * (boundaryHigh - boundaryLow);

        List<DocumentRecord> docs = documentRepo.hybridSearch(semantic, lexical, req.folder(), threshold, geminiService);

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


    private ChatResponse dispatchFunction(String id, String func, Map<String, Object> args) throws Exception
    {
        return(switch (func)
        {
            case "createFolder" -> createFolderAction(id, args);
            case "deleteFolder" -> deleteFolderAction(id, args);
            case "listFiles"    -> listFilesAction(id, args);
            case "deleteFile"   -> deleteFileAction(id, args);
            default -> throw new IllegalArgumentException("Unknown agentic function: " + func);
        });
    }


    private ChatResponse createFolderAction(String id, Map<String, Object> args) throws Exception
    {
        String name = (String) args.get("name");
        folderRepo.create(name, null);
        JsonNode rag = geminiService.ragQuery(id, "What was the outcome of createFolder()", "Folder was created successfully");
        ChatResponse r = new ChatResponse();
        r.setSuccess(true);
        r.setResponse(rag.get("response").asText());
        r.setRefresh(true);
        return(r);
    }


    private ChatResponse deleteFolderAction(String id, Map<String, Object> args) throws Exception
    {
        String name = (String) args.get("name");
        boolean success = folderRepo.deleteByName(name);
        String outcome = success ? "Folder was deleted successfully" : "Deletion of folder failed";
        JsonNode rag = geminiService.ragQuery(id, "What was the outcome of deleteFolder()", outcome);
        ChatResponse r = new ChatResponse();
        r.setSuccess(true);
        r.setResponse(rag.get("response").asText());
        r.setRefresh(true);
        return(r);
    }


    private ChatResponse listFilesAction(String id, Map<String, Object> args) throws Exception
    {
        String folderName = (String) args.get("folder");
        Folder folder = folderRepo.findByName(folderName);
        Long fldid = folder != null ? folder.getId() : null;
        List<DocumentRecord> docs = documentRepo.findAll(fldid);
        List<Document> documents = docs.stream().map(this::toDocument).toList();
        JsonNode rag = geminiService.ragQuery(id, "What was the outcome of listFiles()", "List files returned success");
        ChatResponse r = new ChatResponse();
        r.setSuccess(true);
        r.setResponse(rag.get("response").asText());
        r.setDocuments(documents);
        return(r);
    }


    private ChatResponse deleteFileAction(String id, Map<String, Object> args) throws Exception
    {
        String title = (String) args.get("name");
        boolean success = documentRepo.deleteByTitle(title);
        String outcome = success ? "File was deleted successfully" : "Delete file failed";
        JsonNode rag = geminiService.ragQuery(id, "What was the outcome of deleteFile()", outcome);
        ChatResponse r = new ChatResponse();
        r.setSuccess(true);
        r.setResponse(rag.get("response").asText());
        return(r);
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
            d.getFldid()
        ));
    }
}
