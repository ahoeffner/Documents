package ai.dochandler.services;

import java.net.URI;
import org.slf4j.Logger;
import java.time.Duration;
import org.slf4j.LoggerFactory;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;


/**
 * Minimal MCP client (Streamable HTTP transport) for the Home Assistant MCP server.
 */
@Service
public class HomeAssistantService
{
    private static final Logger log = LoggerFactory.getLogger(HomeAssistantService.class);
    private static final String PROTOCOL_VERSION = "2025-03-26";

    @Value("${app.homeassistant.url:http://home:8123/api/mcp}")
    private String url;

    @Value("${app.homeassistant.token:}")
    private String token;

    @Value("${app.homeassistant.timeout-seconds:60}")
    private int timeoutSeconds;

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final AtomicLong ids = new AtomicLong();

    private String sessionId;
    private boolean initialized;


    public HomeAssistantService(HttpClient client, ObjectMapper mapper)
    {
        this.client = client;
        this.mapper = mapper;
    }


    public JsonNode listTools() throws Exception
    {
        return(rpc("tools/list", mapper.createObjectNode()).path("tools"));
    }


    /**
     * Concatenates the text of every prompt the server offers (Home Assistant: "Assist", with rules and the device list).
     */
    public String prompts() throws Exception
    {
        StringBuilder sb = new StringBuilder();

        for (JsonNode p : rpc("prompts/list", mapper.createObjectNode()).path("prompts"))
        {
            ObjectNode params = mapper.createObjectNode();
            params.put("name", p.path("name").asText());

            for (JsonNode m : rpc("prompts/get", params).path("messages"))
            {
                String text = m.path("content").path("text").asText("");
                if (!text.isBlank()) sb.append(text).append("\n\n");
            }
        }

        return(sb.toString().trim());
    }


    public JsonNode callTool(String name, JsonNode args) throws Exception
    {
        ObjectNode params = mapper.createObjectNode();
        params.put("name", name);
        params.set("arguments", args == null || !args.isObject() ? mapper.createObjectNode() : args);
        return(rpc("tools/call", params));
    }


    private synchronized JsonNode rpc(String method, ObjectNode params) throws Exception
    {
        if (!initialized) initialize();

        try
        {
            return(send(method, params, true));
        }
        catch (SessionExpiredException e)
        {
            log.info("Home Assistant MCP session expired, re-initializing");
            initialize();
            return(send(method, params, true));
        }
    }


    private void initialize() throws Exception
    {
        if (token == null || token.isBlank()) throw new Exception("app.homeassistant.token is not configured");

        sessionId = null;
        initialized = false;

        ObjectNode params = mapper.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.putObject("capabilities");
        params.putObject("clientInfo").put("name", "dochandler").put("version", "1.0.0");

        send("initialize", params, true);
        send("notifications/initialized", null, false);

        initialized = true;
    }


    private JsonNode send(String method, ObjectNode params, boolean expectReply) throws Exception
    {
        long id = ids.incrementAndGet();

        ObjectNode msg = mapper.createObjectNode();
        msg.put("jsonrpc", "2.0");
        if (expectReply) msg.put("id", id);
        msg.put("method", method);
        if (params != null) msg.set("params", params);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .version(HttpClient.Version.HTTP_1_1)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(msg), StandardCharsets.UTF_8));

        if (sessionId != null) builder.header("Mcp-Session-Id", sessionId);
        if (initialized) builder.header("MCP-Protocol-Version", PROTOCOL_VERSION);

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();

        if (status == 404 && sessionId != null) throw new SessionExpiredException();
        if (status == 401) throw new Exception("Home Assistant rejected the access token (401)");
        if (status >= 300) throw new Exception("Home Assistant MCP " + status + ": " + response.body());

        response.headers().firstValue("Mcp-Session-Id").ifPresent(s -> sessionId = s);

        if (!expectReply) return(null);

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        JsonNode reply = contentType.contains("text/event-stream") ? fromEventStream(response.body(), id) : mapper.readTree(response.body());

        if (reply == null) throw new Exception("Home Assistant MCP sent no reply to " + method);

        JsonNode error = reply.path("error");
        if (!error.isMissingNode()) throw new Exception("Home Assistant MCP error: " + error.path("message").asText(error.toString()));

        return(reply.path("result"));
    }


    private JsonNode fromEventStream(String body, long id) throws Exception
    {
        StringBuilder data = new StringBuilder();

        for (String line : (body + "\n\n").split("\r?\n"))
        {
            if (line.startsWith("data:"))
            {
                data.append(line.substring(5).trim());
                continue;
            }

            if (line.isBlank() && data.length() > 0)
            {
                JsonNode event = mapper.readTree(data.toString());
                data.setLength(0);
                if (event.path("id").asLong(-1) == id) return(event);
            }
        }

        return(null);
    }


    private static class SessionExpiredException extends Exception
    {
    }
}
