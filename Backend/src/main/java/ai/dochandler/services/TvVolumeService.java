package ai.dochandler.services;

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;


/**
 * Volume for TVs that only react to key presses (volume_up/down) and never report their real volume (Samsung).
 * Callers only set a target; a background worker presses one step at a time towards it, so nothing blocks,
 * a new target simply redirects or stops the presses, and the volume never goes above app.homeassistant.max-volume.
 */
@Service
public class TvVolumeService
{
    private static final Logger log = LoggerFactory.getLogger(TvVolumeService.class);
    private static final int VOLUME_STEP = 1024;
    private static final long PRESS_PAUSE_MS = 150;

    @Value("${app.homeassistant.max-volume:50}")
    private int maxVolume;

    private final ObjectMapper mapper;
    private final HomeAssistantService homeAssistant;

    private final Map<String, Integer> estimates = new ConcurrentHashMap<>();
    private final Map<String, Integer> targets = new ConcurrentHashMap<>();
    private final Set<String> running = ConcurrentHashMap.newKeySet();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r ->
    {
        Thread t = new Thread(r, "tv-volume");
        t.setDaemon(true);
        return(t);
    });


    public TvVolumeService(HomeAssistantService homeAssistant, ObjectMapper mapper)
    {
        this.homeAssistant = homeAssistant;
        this.mapper = mapper;
    }


    /**
     * True for TVs that can only change volume by key presses (they report VOLUME_SET but ignore it).
     */
    public boolean stepVolumeOnly(JsonNode state)
    {
        int features = state.path("attributes").path("supported_features").asInt(0);
        return((features & VOLUME_STEP) != 0 && state.path("attributes").path("device_class").asText("").equals("tv"));
    }


    public int maxVolume()
    {
        return(maxVolume);
    }


    /**
     * Volume to show: the target while pressing, otherwise our own count (started from what Home Assistant reports).
     */
    public int volume(JsonNode state)
    {
        String id = state.path("entity_id").asText();
        int reported = (int) Math.round(state.path("attributes").path("volume_level").asDouble(0) * 100);
        int estimate = estimates.computeIfAbsent(id, k -> reported);
        return(targets.getOrDefault(id, estimate));
    }


    /**
     * Sets the target volume (capped at max-volume) and returns at once; the presses happen in the background.
     */
    public int setVolume(JsonNode state, int target)
    {
        String id = state.path("entity_id").asText();
        volume(state);

        int t = Math.max(0, Math.min(maxVolume, target));
        targets.put(id, t);
        if (running.add(id)) worker.submit(() -> press(id));
        return(t);
    }


    public int changeVolume(JsonNode state, int delta)
    {
        return(setVolume(state, volume(state) + delta));
    }


    private void press(String id)
    {
        ObjectNode data = mapper.createObjectNode();
        data.put("entity_id", id);

        try
        {
            while (true)
            {
                int current = estimates.get(id);
                int target = targets.getOrDefault(id, current);
                if (current == target) break;

                boolean up = target > current;
                homeAssistant.callService("media_player", up ? "volume_up" : "volume_down", data);
                estimates.put(id, current + (up ? 1 : -1));
                Thread.sleep(PRESS_PAUSE_MS);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (Exception e)
        {
            log.warn("TV volume press failed for {}: {}", id, e.getMessage());
            targets.put(id, estimates.get(id));
        }
        finally
        {
            running.remove(id);
        }

        // a new target may have arrived just as the loop ended
        if (!targets.getOrDefault(id, estimates.get(id)).equals(estimates.get(id)) && running.add(id)) worker.submit(() -> press(id));
    }
}
