package rag.dochandler.services;

import java.net.URI;
import org.slf4j.Logger;
import java.time.Duration;
import org.slf4j.LoggerFactory;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;


@Service
public class GeminiService
{
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${app.ai-services}")
    private String aiServicesUrl;

    private final HttpClient client;
    private final ObjectMapper mapper;


    public GeminiService(HttpClient client, ObjectMapper mapper)
    {
        this.client = client;
        this.mapper = mapper;
    }


    public Float[] computeEmbedding(String text, boolean isQuery)
    {
        if (text == null) return(new Float[0]);
        String field = isQuery ? "query" : "text";
        ObjectNode req = mapper.createObjectNode();
        req.put(field, text);
        try
        {
            JsonNode response = invoke("/embed", req);
            JsonNode arr = response.get("embedding");
            if (arr == null) return(null);
            Float[] embedding = new Float[arr.size()];
            for (int i = 0; i < arr.size(); i++)
            {
                embedding[i] = (float) arr.get(i).asDouble();
            }
            return(embedding);
        }
        catch (Exception e)
        {
            log.error("Failed to compute embedding: {}", e.getMessage());
            return(null);
        }
    }


    public JsonNode preprocess(String id, String query) throws Exception
    {
        ObjectNode req = mapper.createObjectNode();
        req.put("id", id);
        req.put("query", query);
        return(invoke("/rag/preprocess", req));
    }


    public JsonNode ragQuery(String id, String query, String content) throws Exception
    {
        ObjectNode req = mapper.createObjectNode();
        req.put("id", id);
        req.put("query", query);
        req.put("content", content);
        return(invoke("/rag/query", req));
    }


    private JsonNode invoke(String path, ObjectNode body) throws Exception
    {
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(aiServicesUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200)
        {
            throw new Exception("AI service returned status " + response.statusCode() + ": " + response.body());
        }
        return(mapper.readTree(response.body()));
    }
}
