package ai.dochandler.controllers;

import java.util.Map;
import ai.dochandler.services.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api/i18n")
public class I18nController
{
    private final I18nService service;


    public I18nController(I18nService service)
    {
        this.service = service;
    }


    @GetMapping("/locales")
    public ResponseEntity<Map<String, Object>> locales()
    {
        return(ResponseEntity.ok(Map.of("success", true, "locales", service.list())));
    }


    @GetMapping("/{locale}")
    public ResponseEntity<Map<String, Object>> messages(@PathVariable String locale)
    {
        return(ResponseEntity.ok(Map.of("success", true, "locale", locale, "messages", service.get(locale))));
    }
}
