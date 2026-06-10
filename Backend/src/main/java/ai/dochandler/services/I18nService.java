package ai.dochandler.services;

import java.io.File;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


@Service
public class I18nService
{
    private static final Logger log = LoggerFactory.getLogger(I18nService.class);

    private static final Map<String, String> LOCALE_NAMES = new LinkedHashMap<>();
    static
    {
        LOCALE_NAMES.put("en", "English");
        LOCALE_NAMES.put("da", "Dansk");
    }

    @Value("${app.i18n.path:}")
    private String i18nPath;

    private final Map<String, Map<String, Object>> cache = new LinkedHashMap<>();


    public List<Map<String, String>> list()
    {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> e : LOCALE_NAMES.entrySet())
            result.add(Map.of("id", e.getKey(), "name", e.getValue()));
        return(result);
    }


    public Map<String, Object> get(String locale)
    {
        String key = LOCALE_NAMES.containsKey(locale) ? locale : "en";
        return(cache.computeIfAbsent(key, this::load));
    }


    private Map<String, Object> load(String locale)
    {
        if (i18nPath != null && !i18nPath.isBlank())
        {
            File f = new File(i18nPath, locale + ".yaml");
            if (f.exists())
            {
                try (InputStream is = new FileInputStream(f))
                {
                    log.info("Locale '{}' loaded from {}", locale, f);
                    return(new Yaml().load(is));
                }
                catch (Exception e)
                {
                    log.warn("Cannot load external locale '{}': {}", locale, e.getMessage());
                }
            }
            else
            {
                log.warn("Locale file not found: {}", f);
            }
        }

        try (InputStream is = I18nService.class.getResourceAsStream("/lang/" + locale + ".yaml"))
        {
            if (is != null)
            {
                log.info("Locale '{}' loaded from classpath", locale);
                return(new Yaml().load(is));
            }
        }
        catch (Exception e)
        {
            log.error("Cannot load classpath locale '{}': {}", locale, e.getMessage());
        }

        return(Map.of());
    }
}
