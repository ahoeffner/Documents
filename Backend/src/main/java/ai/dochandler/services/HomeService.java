package ai.dochandler.services;

import java.util.Map;
import java.util.Set;
import java.util.List;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;


/**
 * Chat that talks only to Home Assistant: Gemini decides which MCP tools to call, results are fed back until it answers.
 */
@Service
public class HomeService
{
    private static final Logger log = LoggerFactory.getLogger(HomeService.class);
    private static final DateTimeFormatter NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm, EEEE");
    private static final String ADJUST = "AdjustBrightness";
    private static final String STATUS = "LightStatus";
    private static final String DEVICES = "ListDevices";
    private static final String MEDIA = "MediaControl";
    private static final Pattern HELP = Pattern.compile("^\\s*(\\?+|help|hjælp|hjaelp|hilfe|hjelp|hvad kan du( gøre)?|hvad kan jeg( gøre| styre)?|what can you do|what can i (do|control))\\s*[?!.]*\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> TABLES = Set.of(STATUS, DEVICES);
    private static final Pattern DOMAIN = Pattern.compile("(?m)^\\s*domain:\\s*(\\S+)");

    private static final Map<String, String> DOMAIN_DA = new LinkedHashMap<>(Map.of(
        "light", "lys", "switch", "kontakt", "media_player", "medieafspiller", "vacuum", "støvsuger",
        "todo", "liste", "binary_sensor", "sensor", "sensor", "sensor", "climate", "klima", "cover", "gardin", "fan", "ventilator"));

    private static final Map<String, String> DOMAIN_EN = Map.of(
        "light", "light", "switch", "switch", "media_player", "media player", "vacuum", "vacuum",
        "todo", "to-do list", "binary_sensor", "sensor", "sensor", "sensor", "climate", "climate", "cover", "cover", "fan", "fan");

    private static final Map<String, String> STATE_DA = Map.of(
        "on", "tændt", "off", "slukket", "playing", "spiller", "paused", "pause", "idle", "inaktiv",
        "cleaning", "gør rent", "docked", "i dock", "returning", "på vej hjem", "standby", "standby", "error", "fejl");
    private static final String CONFIRM = "(Reply now with one short sentence confirming what was done, in the language of my request above.)";
    private static final String CONFIRM_DA = "(Svar nu med én kort sætning på dansk, der bekræfter hvad der blev gjort.)";
    private static final Pattern DANISH = Pattern.compile("[æøå]|\\b(lys|lyset|lysene|hvad|hvordan|hvilke|er|på|alle|stuen|gangen|sal|vis|tænd|sluk|skru|sæt|dæmp|lysere|mørkere|op|ned|hjem|rent|støvsug\\w*)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGLISH = Pattern.compile("\\b(what|how|which|is|are|the|lights?|show|all|of|turn|on|off|set|dim|help)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATE = Pattern.compile("state:\\s*'?([^'\\n]+)'?");
    private static final Pattern BRIGHTNESS = Pattern.compile("(brightness:\\s*)'?(\\d+)'?");
    private static final Pattern AREA_QUESTION = Pattern.compile("[^.\\n]*ask (the )?user to specify an area[^.\\n]*\\.\\s*");

    private static final String DEFAULT_PROMPT = """
        You are a smart home assistant connected to Home Assistant through tools.
        Use ONLY the provided tools to read the state of, or control, the home. Never invent states or devices.
        When asked about current states, call the live-context tool first if one is available.
        Answer in the language of the user. Keep answers short and natural, they may be read aloud.
        If a request cannot be done with the available tools, say so briefly.
        """;

    @Value("${app.homeassistant.max-steps:10}")
    private int maxSteps;

    @Value("${app.homeassistant.hidden:}")
    private String hiddenNames;

    @Value("${app.gemini.max-conversations:64}")
    private int maxConversations;

    private final ObjectMapper mapper;
    private final GeminiService gemini;
    private final TvVolumeService tv;
    private final HomeAssistantService homeAssistant;

    private Map<String, ArrayNode> conversations;


    public HomeService(GeminiService gemini, HomeAssistantService homeAssistant, TvVolumeService tv, ObjectMapper mapper)
    {
        this.gemini = gemini;
        this.homeAssistant = homeAssistant;
        this.tv = tv;
        this.mapper = mapper;
    }


    @PostConstruct
    private void init()
    {
        final int max = maxConversations;
        conversations = Collections.synchronizedMap(new LinkedHashMap<>(max, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ArrayNode> eldest)
            {
                return(size() > max);
            }
        });
    }


    public String chat(String id, String query) throws Exception
    {
        ArrayNode history = conversations.computeIfAbsent(id, k -> mapper.createArrayNode());
        ArrayNode functions = toFunctions(homeAssistant.listTools());
        String system = systemText();

        boolean da = DANISH.matcher(query).find();
        boolean en = !da && ENGLISH.matcher(query).find();

        // "?" / "help" / "hjælp": answer with the device list directly, no model round
        if (HELP.matcher(query).matches())
        {
            String answer = devicesTable(mapper.createObjectNode(), en);
            history.add(textTurn("user", query));
            history.add(textTurn("model", answer));
            return(answer);
        }

        ArrayNode contents = history.deepCopy();
        contents.add(textTurn("user", query));

        boolean nudged = false;

        for (int step = 0; step < maxSteps; step++)
        {
            JsonNode content = gemini.generateWithTools(contents, functions, system);
            List<JsonNode> calls = functionCalls(content);

            // Empty reply (often after the actions already ran): ask once for a confirmation, then fall back to a check mark
            if (calls.isEmpty() && text(content).isBlank())
            {
                if (nudged)
                {
                    history.add(textTurn("user", query));
                    history.add(textTurn("model", "✓"));
                    return("✓");
                }
                nudged = true;
                contents.add(textTurn("user", da ? CONFIRM_DA : CONFIRM));
                continue;
            }

            ObjectNode modelTurn = content.deepCopy();
            modelTurn.put("role", "model");
            contents.add(modelTurn);

            String answer = calls.isEmpty() ? text(content) : statusAnswer(calls, en);

            if (answer != null)
            {
                history.add(textTurn("user", query));
                history.add(textTurn("model", answer));
                return(answer);
            }

            ObjectNode toolTurn = contents.addObject();
            toolTurn.put("role", "user");
            ArrayNode parts = toolTurn.putArray("parts");
            for (JsonNode call : calls) parts.add(execute(call, en));
        }

        throw new Exception("Gave up after " + maxSteps + " tool calls");
    }


    private ObjectNode execute(JsonNode call, boolean en)
    {
        String name = call.path("name").asText();
        ObjectNode response = mapper.createObjectNode();

        try
        {
            JsonNode args = call.path("args");
            JsonNode result;

            if (ADJUST.equals(name)) result = adjustBrightness(args);
            else if (MEDIA.equals(name)) result = mediaControl(args);
            else if (TABLES.contains(name)) result = toolResult(table(call, en), false);
            else result = homeAssistant.callTool(name, args);

            String text = resultText(result);
            if ("GetLiveContext".equals(name)) text = brightnessToPercent(withoutHidden(text));

            if (result.path("isError").asBoolean(false)) response.put("error", text);
            else if (nothingAffected(text)) response.put("error", "No device was affected; the device does not support this action in its current state. Tell the user it did not work.");
            else response.put("result", text);

            log.info("Home tool {} {} -> {}", name, call.path("args"), text.length() > 200 ? text.substring(0, 200) + "…" : text);
        }
        catch (Exception e)
        {
            log.warn("Home tool {} failed: {}", name, e.getMessage());
            response.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        ObjectNode part = mapper.createObjectNode();
        ObjectNode fr = part.putObject("functionResponse");
        if (call.has("id")) fr.set("id", call.get("id"));
        fr.put("name", name);
        fr.set("response", response);
        return(part);
    }


    private ArrayNode toFunctions(JsonNode tools)
    {
        ArrayNode functions = mapper.createArrayNode();

        for (JsonNode tool : tools)
        {
            ObjectNode fn = functions.addObject();
            fn.put("name", tool.path("name").asText());
            fn.put("description", tool.path("description").asText(""));

            JsonNode schema = tool.path("inputSchema");
            if (schema.isObject())
            {
                ObjectNode copy = schema.deepCopy();
                copy.remove("$schema");
                fn.set("parametersJsonSchema", copy);
            }
        }

        functions.add(adjustDeclaration());
        functions.add(statusDeclaration());
        functions.add(mediaDeclaration());
        functions.add(devicesDeclaration());
        return(functions);
    }


    private List<JsonNode> functionCalls(JsonNode content)
    {
        List<JsonNode> calls = new ArrayList<>();
        for (JsonNode part : content.path("parts"))
        {
            if (part.has("functionCall")) calls.add(part.get("functionCall"));
        }
        return(calls);
    }


    private String text(JsonNode content)
    {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : content.path("parts"))
        {
            if (part.has("text") && !part.path("thought").asBoolean(false)) sb.append(part.get("text").asText());
        }
        return(sb.toString().trim());
    }


    private String resultText(JsonNode result)
    {
        StringBuilder sb = new StringBuilder();
        for (JsonNode c : result.path("content"))
        {
            if ("text".equals(c.path("type").asText())) sb.append(c.path("text").asText()).append("\n");
        }
        if (sb.length() == 0) return(result.toString());
        return(sb.toString().trim());
    }


    /**
     * Home Assistant answers "action_done" even when its intent matched no device (e.g. volume on a TV that is
     * on but not playing); without this the model would report success.
     */
    private boolean nothingAffected(String text)
    {
        try
        {
            JsonNode json = mapper.readTree(text);
            if (!"action_done".equals(json.path("response_type").asText())) return(false);
            JsonNode data = json.path("data");
            return(data.path("success").size() == 0 && data.path("failed").size() == 0);
        }
        catch (Exception e)
        {
            return(false);
        }
    }


    /**
     * Local tool: mute and volume via direct service calls, which (unlike the voice intents) also work on
     * media players that are on but not playing, such as the TV.
     */
    private ObjectNode mediaDeclaration()
    {
        ObjectNode fn = mapper.createObjectNode();
        fn.put("name", MEDIA);
        fn.put("description", "Mutes, unmutes or sets the volume of a media player such as the TV. Use this instead of HassMediaPlayerMute, HassMediaPlayerUnmute, HassSetVolume and HassSetVolumeRelative.");

        ObjectNode schema = fn.putObject("parametersJsonSchema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("name").put("type", "string").put("description", "The media player name, e.g. Stue tv");
        ObjectNode action = props.putObject("action");
        action.put("type", "string");
        action.putArray("enum").add("mute").add("unmute").add("volume").add("volume_up").add("volume_down");
        props.putObject("level").put("type", "integer").put("description", "For volume: the level 0-100. For volume_up/volume_down: how many steps (key presses on a TV), omit for the default.");
        schema.putArray("required").add("name").add("action");

        return(fn);
    }


    private JsonNode mediaControl(JsonNode args) throws Exception
    {
        String name = args.path("name").asText();
        String action = args.path("action").asText();

        JsonNode player = null;
        for (JsonNode s : homeAssistant.states())
        {
            String id = s.path("entity_id").asText();
            String friendly = s.path("attributes").path("friendly_name").asText("");
            if (id.startsWith("media_player.") && friendly.equalsIgnoreCase(name.trim()) && !isHidden(List.of(friendly))) player = s;
        }
        if (player == null) return(toolResult("No media player named '" + name + "'", true));

        String id = player.path("entity_id").asText();
        int current = (int) Math.round(player.path("attributes").path("volume_level").asDouble(0) * 100);

        ObjectNode data = mapper.createObjectNode();
        data.put("entity_id", id);

        switch (action)
        {
            case "mute", "unmute" ->
            {
                data.put("is_volume_muted", "mute".equals(action));
                homeAssistant.callService("media_player", "volume_mute", data);
                return(toolResult(name + ": " + ("mute".equals(action) ? "muted" : "unmuted"), false));
            }
            case "volume_up", "volume_down" ->
            {
                if (tv.stepVolumeOnly(player))
                {
                    int steps = args.has("level") ? Math.abs(args.path("level").asInt()) : 5;
                    int to = tv.changeVolume(player, "volume_up".equals(action) ? steps : -steps);
                    return(toolResult(name + ": turning the volume " + ("volume_up".equals(action) ? "up" : "down") + " to about " + to + " (maximum " + tv.maxVolume() + ")", false));
                }
                int step = args.has("level") ? Math.abs(args.path("level").asInt()) : 10;
                int target = Math.max(0, Math.min(100, current + ("volume_up".equals(action) ? step : -step)));
                data.put("volume_level", target / 100.0);
                homeAssistant.callService("media_player", "volume_set", data);
                return(toolResult(name + ": volume changed from " + current + "% to " + target + "%", false));
            }
            case "volume" ->
            {
                if (tv.stepVolumeOnly(player))
                {
                    int to = tv.setVolume(player, args.path("level").asInt(tv.volume(player)));
                    return(toolResult(name + ": setting the volume to about " + to + " (maximum " + tv.maxVolume() + ")", false));
                }
                int target = Math.max(0, Math.min(100, args.path("level").asInt(current)));
                data.put("volume_level", target / 100.0);
                homeAssistant.callService("media_player", "volume_set", data);
                return(toolResult(name + ": volume changed from " + current + "% to " + target + "%", false));
            }
            default ->
            {
                return(toolResult("Unknown action '" + action + "'", true));
            }
        }
    }


    /**
     * Local tool: the model only picks light and direction, the arithmetic (and clamping to 1-100) is done here.
     */
    private ObjectNode adjustDeclaration()
    {
        ObjectNode fn = mapper.createObjectNode();
        fn.put("name", ADJUST);
        fn.put("description", "Makes a light brighter or dimmer relative to its current brightness. Use for requests like 'skru op/ned', 'lysere', 'mørkere', 'brighter', 'dimmer'. One call per light.");

        ObjectNode schema = fn.putObject("parametersJsonSchema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("name").put("type", "string").put("description", "The light name, e.g. Sofa");
        props.putObject("step").put("type", "integer").put("description", "Percentage points to change: positive is brighter, negative is dimmer. Use 20 or -20 unless the user says an amount.");
        schema.putArray("required").add("name").add("step");

        return(fn);
    }


    private JsonNode adjustBrightness(JsonNode args) throws Exception
    {
        String light = args.path("name").asText();
        int step = args.path("step").asInt(20);

        Entity state = find(entities(live()).stream().filter(e -> "light".equals(e.domain())).toList(), light);
        if (state == null) return(toolResult("No light named '" + light + "'", true));

        int current = "on".equals(state.state()) ? (state.percent() != null ? state.percent() : 100) : 0;

        int target = Math.max(1, Math.min(100, current + step));

        ObjectNode set = mapper.createObjectNode();
        set.put("name", light);
        set.putArray("domain").add("light");
        set.put("brightness", target);

        JsonNode result = homeAssistant.callTool("HassLightSet", set);
        if (result.path("isError").asBoolean(false)) return(result);

        return(toolResult(light + ": brightness changed from " + current + "% to " + target + "%", false));
    }


    /**
     * When the model only asked for status tables, the backend's tables are the answer (no second model round).
     */
    private String statusAnswer(List<JsonNode> calls, boolean en)
    {
        if (!calls.stream().allMatch(c -> TABLES.contains(c.path("name").asText()))) return(null);

        try
        {
            StringBuilder sb = new StringBuilder();
            for (JsonNode call : calls) sb.append(table(call, en)).append("\n\n");
            return(sb.toString().trim());
        }
        catch (Exception e)
        {
            log.warn("Status table failed: {}", e.getMessage());
            return(null);
        }
    }


    private String table(JsonNode call, boolean en) throws Exception
    {
        if (DEVICES.equals(call.path("name").asText())) return(devicesTable(call.path("args"), en));
        return(statusTable(call.path("args"), en));
    }


    private ObjectNode statusDeclaration()
    {
        ObjectNode fn = mapper.createObjectNode();
        fn.put("name", STATUS);
        fn.put("description", "Shows the current status of lights and the TV as a table. Use for every question about the state of lights or the TV, and for 'status'.");

        ObjectNode schema = fn.putObject("parametersJsonSchema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode names = props.putObject("names");
        names.put("type", "array").put("description", "The device names to show, e.g. [\"1 sal\", \"Sofa\", \"Stue tv\"]. [\"*\"] (all lights) ONLY when the user explicitly says all/alle; 'the lights' is not all.");
        names.putObject("items").put("type", "string");
        schema.putArray("required").add("names");

        return(fn);
    }


    private ObjectNode devicesDeclaration()
    {
        ObjectNode fn = mapper.createObjectNode();
        fn.put("name", DEVICES);
        fn.put("description", "Lists the devices (entities) in the home with type and current status as a table. Use for 'list all devices', 'vis alle enheder', 'what can you control', 'hvilke medieafspillere har jeg', and for the status of non-light devices such as the vacuum or the TV.");

        ObjectNode schema = fn.putObject("parametersJsonSchema");
        schema.put("type", "object");
        ObjectNode domain = schema.putObject("properties").putObject("domain");
        domain.put("type", "string").put("description", "Only list this type. Omit to list everything.");
        ArrayNode values = domain.putArray("enum");
        for (String d : DOMAIN_DA.keySet()) values.add(d);

        return(fn);
    }


    private String statusTable(JsonNode args, boolean en) throws Exception
    {
        List<Entity> all = entities(live()).stream().filter(e -> "light".equals(e.domain()) || "media_player".equals(e.domain())).toList();

        List<String> names = new ArrayList<>();
        for (JsonNode n : args.path("names")) names.add(n.asText());

        StringBuilder sb = new StringBuilder(en ? "| Device | Status |\n|---|---|\n" : "| Enhed | Status |\n|---|---|\n");

        // "*" (all lights) lists the lights only
        if (names.isEmpty() || names.contains("*"))
        {
            for (Entity l : all)
            {
                if ("light".equals(l.domain())) sb.append("| ").append(l.name()).append(" | ").append(statusText(l, en)).append(" |\n");
            }
            return(sb.toString().trim());
        }

        Map<String, JsonNode> players = null;

        for (String name : names)
        {
            Entity e = find(all, name);
            String status = "NA";

            if (e != null && "media_player".equals(e.domain()))
            {
                if (players == null) players = mediaPlayers();
                status = mediaStatus(e, players.get(e.name().toLowerCase()), en);
            }
            else if (e != null)
            {
                status = statusText(e, en);
            }

            sb.append("| ").append(e != null ? e.name() : name).append(" | ").append(status).append(" |\n");
        }

        return(sb.toString().trim());
    }


    private String devicesTable(JsonNode args, boolean en) throws Exception
    {
        String domain = args.path("domain").asText("");

        List<Entity> all = entities(live()).stream()
            .filter(e -> domain.isBlank() || domain.equals(e.domain()))
            .sorted((a, b) -> a.domain().equals(b.domain()) ? a.name().compareToIgnoreCase(b.name()) : a.domain().compareTo(b.domain()))
            .toList();

        StringBuilder sb = new StringBuilder(en ? "| Device | Type | Status |\n|---|---|---|\n" : "| Enhed | Type | Status |\n|---|---|---|\n");
        for (Entity e : all)
        {
            String type = (en ? DOMAIN_EN : DOMAIN_DA).getOrDefault(e.domain(), e.domain());
            sb.append("| ").append(e.name()).append(" | ").append(type).append(" | ").append(statusText(e, en)).append(" |\n");
        }

        return(sb.toString().trim());
    }


    /**
     * Media players by lower-case friendly name, from the REST API (it has mute state and features, the live context not).
     */
    private Map<String, JsonNode> mediaPlayers() throws Exception
    {
        Map<String, JsonNode> players = new LinkedHashMap<>();
        for (JsonNode s : homeAssistant.states())
        {
            if (s.path("entity_id").asText().startsWith("media_player.")) players.put(s.path("attributes").path("friendly_name").asText("").toLowerCase(), s);
        }
        return(players);
    }


    /**
     * "tændt, 40%" / "tændt, lydløs" / "slukket". The volume is left out for TVs that never report it (key presses only).
     */
    private String mediaStatus(Entity e, JsonNode rest, boolean en)
    {
        if (rest == null) return(statusText(e, en));

        String state = rest.path("state").asText("unknown");
        if ("unavailable".equals(state) || "unknown".equals(state)) return("NA");
        if ("off".equals(state) || "standby".equals(state)) return(en ? "off" : "slukket");

        String text = en ? state : STATE_DA.getOrDefault(state, state);
        JsonNode a = rest.path("attributes");

        if (a.path("is_volume_muted").asBoolean(false)) return(text + (en ? ", muted" : ", lydløs"));
        if (!tv.stepVolumeOnly(rest) && a.has("volume_level")) return(text + ", " + Math.round(a.path("volume_level").asDouble() * 100) + "%");
        return(text);
    }


    private String statusText(Entity e, boolean en)
    {
        String state = e.state();
        if ("unavailable".equals(state) || "unknown".equals(state)) return("NA");
        if ("light".equals(e.domain()) && "on".equals(state) && e.percent() != null) return(e.percent() + "%");
        if ("todo".equals(e.domain())) return(state + (en ? " items" : " punkter"));
        if (en) return(state);
        return(STATE_DA.getOrDefault(state, state));
    }


    private String live() throws Exception
    {
        return(resultText(homeAssistant.callTool("GetLiveContext", mapper.createObjectNode())));
    }


    private record Entity(List<String> aliases, String domain, String state, Integer percent)
    {
        String name()
        {
            return(aliases.get(0));
        }
    }


    private Entity find(List<Entity> entities, String name)
    {
        for (Entity e : entities)
        {
            for (String alias : e.aliases())
            {
                if (alias.equalsIgnoreCase(name.trim())) return(e);
            }
        }
        return(null);
    }


    /**
     * Parses the entities out of the live context; light brightness is converted from 0-255 to percent.
     */
    private List<Entity> entities(String live)
    {
        try
        {
            JsonNode json = mapper.readTree(live);
            if (json.has("result")) live = json.get("result").asText();
        }
        catch (Exception ignored) {}

        List<Entity> entities = new ArrayList<>();

        for (String block : live.split("(?m)^- names: "))
        {
            String[] lines = block.split("\\n", 2);
            if (lines.length < 2) continue;

            Matcher dm = DOMAIN.matcher(lines[1]);
            if (!dm.find()) continue;

            List<String> aliases = new ArrayList<>();
            for (String alias : lines[0].split(",")) aliases.add(alias.replace("'", "").replace("\"", "").trim());

            Matcher st = STATE.matcher(lines[1]);
            String state = st.find() ? st.group(1).trim() : "unknown";

            Matcher br = BRIGHTNESS.matcher(lines[1]);
            Integer percent = br.find() ? (int) Math.round(Integer.parseInt(br.group(2)) * 100.0 / 255.0) : null;

            if (!isHidden(aliases)) entities.add(new Entity(aliases, dm.group(1).trim(), state, percent));
        }

        return(entities);
    }


    private ObjectNode toolResult(String text, boolean error)
    {
        ObjectNode result = mapper.createObjectNode();
        result.putArray("content").addObject().put("type", "text").put("text", text);
        result.put("isError", error);
        return(result);
    }


    /**
     * Live context reports brightness on Home Assistant's 0-255 scale, while HassLightSet takes a percentage.
     */
    private String brightnessToPercent(String text)
    {
        Matcher m = BRIGHTNESS.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find())
        {
            long percent = Math.round(Integer.parseInt(m.group(2)) * 100.0 / 255.0);
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + percent + "%"));
        }
        m.appendTail(sb);
        return(sb.toString());
    }


    private ObjectNode textTurn(String role, String text)
    {
        ObjectNode turn = mapper.createObjectNode();
        turn.put("role", role);
        turn.putArray("parts").addObject().put("text", text);
        return(turn);
    }


    /**
     * Entities listed in app.homeassistant.hidden (comma separated) are left out everywhere,
     * e.g. a duplicate TV entry that does not respond.
     */
    private boolean isHidden(List<String> aliases)
    {
        if (hiddenNames == null || hiddenNames.isBlank()) return(false);

        for (String hidden : hiddenNames.split(","))
        {
            for (String alias : aliases)
            {
                if (alias.equalsIgnoreCase(hidden.trim())) return(true);
            }
        }
        return(false);
    }


    /**
     * Removes hidden entities from Home Assistant's device lists ("- names: ..." blocks), in plain or JSON-wrapped text.
     */
    private String withoutHidden(String text)
    {
        if (hiddenNames == null || hiddenNames.isBlank()) return(text);

        String inner = text;
        ObjectNode wrapper = null;
        try
        {
            JsonNode json = mapper.readTree(text);
            if (json.isObject() && json.has("result"))
            {
                wrapper = (ObjectNode) json;
                inner = json.get("result").asText();
            }
        }
        catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        String[] blocks = inner.split("(?m)^(?=- names: )");
        for (String block : blocks)
        {
            if (block.startsWith("- names: "))
            {
                List<String> aliases = new ArrayList<>();
                for (String alias : block.substring(9).split("\\n", 2)[0].split(",")) aliases.add(alias.replace("'", "").replace("\"", "").trim());
                if (isHidden(aliases)) continue;
            }
            sb.append(block);
        }

        if (wrapper == null) return(sb.toString());
        wrapper.put("result", sb.toString());
        return(wrapper.toString());
    }


    private String systemText()
    {
        String prompt = gemini.getPromptText("HOME");
        if (prompt.isBlank()) prompt = DEFAULT_PROMPT.trim();

        String assist = "";
        try
        {
            // Home Assistant tells the model to ask for an area on "turn on the lights"; that conflicts with the house defaults
            assist = withoutHidden(AREA_QUESTION.matcher(homeAssistant.prompts()).replaceAll(""));
        }
        catch (Exception e)
        {
            log.warn("Cannot load Home Assistant prompts: {}", e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        if (!assist.isBlank()) sb.append(assist).append("\n\n");
        sb.append(prompt);
        sb.append("\n\nThe current date and time is ").append(LocalDateTime.now().format(NOW)).append(". Location: Copenhagen, Denmark.");
        return(sb.toString());
    }
}
