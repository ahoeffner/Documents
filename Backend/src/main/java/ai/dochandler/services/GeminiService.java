package ai.dochandler.services;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import java.util.HashMap;
import java.time.Duration;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.LoggerFactory;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;


@Service
public class GeminiService
{
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final String API_BASE      = "https://generativelanguage.googleapis.com/v1beta";
    private static final String API_BASE_EMBED = "https://generativelanguage.googleapis.com/v1";

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.llm-model:gemini-2.5-flash-lite}")
    private String llmModel;

    @Value("${app.gemini.embedding-model:models/text-embedding-004}")
    private String embeddingModel;

    @Value("${app.gemini.prompts-file:}")
    private String promptsFile;

    @Value("${app.gemini.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${app.gemini.embedding-retries:3}")
    private int embeddingRetries;

    @Value("${app.gemini.max-conversations:64}")
    private int maxConversations;

    private final HttpClient client;
    private final ObjectMapper mapper;

    private Map<String, String> prompts;
    private Map<String, List<Map<String, Object>>> conversations;


    public GeminiService(HttpClient client, ObjectMapper mapper)
    {
        this.client = client;
        this.mapper = mapper;
    }


    @PostConstruct
    private void init()
    {
        final int max = maxConversations;
        conversations = Collections.synchronizedMap(new LinkedHashMap<>(max, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<Map<String, Object>>> eldest)
            {
                return(size() > max);
            }
        });
    }


    // ── Embedding ─────────────────────────────────────────────────────────────

    public Float[] computeEmbedding(String text, boolean isQuery)
    {
        if (text == null || text.isBlank()) return(new Float[0]);

        String taskType = isQuery ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT";
        String url = API_BASE_EMBED + "/" + embeddingModel + ":embedContent?key=" + apiKey;

        ObjectNode body = mapper.createObjectNode();
        body.put("model", embeddingModel);
        body.putObject("content").putArray("parts").addObject().put("text", text);
        body.put("taskType", taskType);

        for (int i = 0; i < embeddingRetries; i++)
        {
            try
            {
                JsonNode response = post(url, body);
                JsonNode values = response.path("embedding").path("values");
                if (!values.isMissingNode())
                {
                    Float[] result = new Float[values.size()];
                    for (int j = 0; j < values.size(); j++)
                        result[j] = (float) values.get(j).asDouble();
                    return(result);
                }
            }
            catch (Exception e)
            {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean retryable = msg.contains("429") || msg.contains("500") || msg.contains("503");
                if (retryable && i < embeddingRetries - 1)
                {
                    long wait = (1L << i) * 1000;
                    log.warn("Embedding retry {} after {}ms: {}", i + 1, wait, msg);
                    try { Thread.sleep(wait); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                else
                {
                    log.error("Embedding failed after {} attempt(s): {}", i + 1, msg);
                    return(null);
                }
            }
        }
        return(null);
    }


    // ── Preprocess ────────────────────────────────────────────────────────────

    public JsonNode preprocess(String id, String query) throws Exception
    {
        List<Map<String, Object>> history = getOrCreate(id);

        String prompt = getPrompt("EXTRACT").replace("{question}", query);

        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "OBJECT");
        ObjectNode props = schema.putObject("properties");
        props.putObject("semantic").put("type", "STRING");
        ObjectNode lex = props.putObject("lexical");
        lex.put("type", "ARRAY");
        lex.putObject("items").put("type", "STRING");
        schema.putArray("required").add("semantic").add("lexical");

        ObjectNode genConfig = mapper.createObjectNode();
        genConfig.put("temperature", 0.01);
        genConfig.put("responseMimeType", "application/json");
        genConfig.set("responseSchema", schema);

        String text = callModel(history, prompt, genConfig);
        append(history, "user", prompt);
        append(history, "model", text);

        return(mapper.readTree(text));
    }


    // ── RAG query ─────────────────────────────────────────────────────────────

    public JsonNode ragQuery(String id, String query, String context) throws Exception
    {
        List<Map<String, Object>> history = getOrCreate(id);

        String prompt = getPrompt("INFERENCE")
            .replace("{question}", query)
            .replace("{context}", context);

        String text = callModel(history, prompt, null);
        append(history, "user", prompt);
        append(history, "model", text);

        return(mapper.createObjectNode().put("response", text));
    }


    public void closeConversation(String id)
    {
        conversations.remove(id);
    }


    // ── Private ───────────────────────────────────────────────────────────────

    private String callModel(List<Map<String, Object>> history, String userPrompt, ObjectNode genConfig) throws Exception
    {
        ObjectNode body = mapper.createObjectNode();

        body.putObject("system_instruction")
            .putArray("parts").addObject()
            .put("text", "The current date is " + LocalDate.now() + ". Location: Copenhagen, Denmark.");

        ArrayNode contents = body.putArray("contents");
        for (Map<String, Object> turn : history)
        {
            contents.addObject()
                .put("role", (String) turn.get("role"))
                .putArray("parts").addObject()
                .put("text", (String) turn.get("text"));
        }
        contents.addObject()
            .put("role", "user")
            .putArray("parts").addObject()
            .put("text", userPrompt);

        if (genConfig != null) body.set("generationConfig", genConfig);

        String url = API_BASE + "/models/" + llmModel + ":generateContent?key=" + apiKey;
        JsonNode response = post(url, body);

        return(response.path("candidates").path(0)
            .path("content").path("parts").path(0)
            .path("text").asText());
    }


    private List<Map<String, Object>> getOrCreate(String id)
    {
        return(conversations.computeIfAbsent(id, k -> new ArrayList<>()));
    }


    private void append(List<Map<String, Object>> history, String role, String text)
    {
        Map<String, Object> turn = new HashMap<>();
        turn.put("role", role);
        turn.put("text", text);
        history.add(turn);
    }


    private JsonNode post(String url, ObjectNode body) throws Exception
    {
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200)
            throw new Exception("Gemini API " + response.statusCode() + ": " + response.body());
        return(mapper.readTree(response.body()));
    }


    private String getPrompt(String key)
    {
        if (prompts == null) prompts = loadPrompts();
        String p = prompts.get(key);
        return(p != null ? p.trim() : "");
    }


    private Map<String, String> loadPrompts()
    {
        if (promptsFile != null && !promptsFile.isBlank())
        {
            File f = new File(promptsFile);
            if (f.exists())
            {
                try (InputStream is = new FileInputStream(f))
                {
                    log.info("Prompts loaded from {}", promptsFile);
                    return(new Yaml().load(is));
                }
                catch (Exception e)
                {
                    log.warn("Cannot load external prompts '{}': {}", promptsFile, e.getMessage());
                }
            }
            else
            {
                log.warn("Prompts file not found: {}", promptsFile);
            }
        }

        try (InputStream is = GeminiService.class.getResourceAsStream("/prompts.yaml"))
        {
            if (is != null)
            {
                log.info("Prompts loaded from classpath");
                return(new Yaml().load(is));
            }
        }
        catch (Exception e)
        {
            log.error("Cannot load classpath prompts: {}", e.getMessage());
        }

        log.warn("No prompts loaded");
        return(new HashMap<>());
    }
}
