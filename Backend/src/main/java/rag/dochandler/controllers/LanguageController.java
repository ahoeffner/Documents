package rag.dochandler.controllers;

import java.util.Map;
import java.util.List;
import rag.dochandler.entities.Language;
import rag.dochandler.services.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/languages")
public class LanguageController
{
    private final LanguageService service;


    public LanguageController(LanguageService service)
    {
        this.service = service;
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> list()
    {
        List<Language> languages = service.list();
        return(ResponseEntity.ok(Map.of("success", true, "languages", languages)));
    }
}
