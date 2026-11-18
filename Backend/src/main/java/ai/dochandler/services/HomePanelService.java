package ai.dochandler.services;

import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;


/**
 * Control panel in the Home tab: direct on/off, brightness and volume for a fixed list of entities,
 * via Home Assistant's REST API (no model involved). Only entities listed in app.homeassistant.panel can be controlled.
 */
@Service
public class HomePanelService
{
    @Value("${app.homeassistant.panel:}")
    private String panelEntities;

    private final TvVolumeService tv;
    private final ObjectMapper mapper;
    private final HomeAssistantService homeAssistant;


    public HomePanelService(HomeAssistantService homeAssistant, TvVolumeService tv, ObjectMapper mapper)
    {
        this.homeAssistant = homeAssistant;
        this.tv = tv;
        this.mapper = mapper;
    }


    public ArrayNode items() throws Exception
    {
        ArrayNode items = mapper.createArrayNode();
        for (String id : entityIds()) items.add(item(id));
        return(items);
    }


    /**
     * action: on, off, level (value 0-100: brightness or volume), mute, unmute
     */
    public ObjectNode control(String id, String action, Integer value) throws Exception
    {
        if (!entityIds().contains(id)) throw new IllegalArgumentException("Not a panel entity: " + id);

        String domain = id.substring(0, id.indexOf('.'));
        ObjectNode data = mapper.createObjectNode();
        data.put("entity_id", id);

        switch (action)
        {
            case "on" -> homeAssistant.callService(domain, "turn_on", data);
            case "off" -> homeAssistant.callService(domain, "turn_off", data);
            case "level" -> level(id, domain, data, value);
            case "mute", "unmute" ->
            {
                data.put("is_volume_muted", "mute".equals(action));
                homeAssistant.callService("media_player", "volume_mute", data);
            }
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }

        return(item(id));
    }


    private void level(String id, String domain, ObjectNode data, Integer value) throws Exception
    {
        if (value == null) throw new IllegalArgumentException("level needs a value");
        int v = Math.max(0, Math.min(100, value));

        if ("light".equals(domain))
        {
            if (v == 0)
            {
                homeAssistant.callService("light", "turn_off", data);
                return;
            }
            data.put("brightness_pct", v);
            homeAssistant.callService("light", "turn_on", data);
            return;
        }

        JsonNode state = homeAssistant.state(id);
        if (tv.stepVolumeOnly(state))
        {
            tv.setVolume(state, v);
            return;
        }

        data.put("volume_level", v / 100.0);
        homeAssistant.callService("media_player", "volume_set", data);
    }


    private ObjectNode item(String id) throws Exception
    {
        JsonNode s = homeAssistant.state(id);
        JsonNode a = s.path("attributes");
        String state = s.path("state").asText("unknown");
        String domain = id.substring(0, id.indexOf('.'));

        ObjectNode item = mapper.createObjectNode();
        item.put("id", id);
        item.put("name", a.path("friendly_name").asText(id));
        item.put("domain", domain);
        item.put("state", state);
        item.put("available", !"unavailable".equals(state) && !"unknown".equals(state));
        item.put("on", !"off".equals(state) && !"unavailable".equals(state) && !"standby".equals(state));

        if ("light".equals(domain))
        {
            int brightness = a.path("brightness").asInt(0);
            item.put("level", "on".equals(state) ? (int) Math.round(brightness * 100.0 / 255.0) : 0);
        }
        else if ("media_player".equals(domain))
        {
            boolean steps = tv.stepVolumeOnly(s);
            item.put("level", steps ? tv.volume(s) : (int) Math.round(a.path("volume_level").asDouble(0) * 100));
            item.put("muted", a.path("is_volume_muted").asBoolean(false));
            item.put("stepVolume", steps);
            if (steps) item.put("max", tv.maxVolume());
        }

        return(item);
    }


    private List<String> entityIds()
    {
        List<String> ids = new ArrayList<>();
        if (panelEntities == null) return(ids);

        for (String id : panelEntities.split(","))
        {
            if (!id.isBlank()) ids.add(id.trim());
        }
        return(ids);
    }
}
