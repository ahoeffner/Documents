package ai.dochandler.controllers;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import ai.dochandler.services.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/tts")
public class TTSController
{
    private static final Logger log = LoggerFactory.getLogger(TTSController.class);

    private final GeminiService geminiService;


    public TTSController(GeminiService geminiService)
    {
        this.geminiService = geminiService;
    }


    @PostMapping
    public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> body)
    {
        try
        {
            byte[] wav = geminiService.synthesizeSpeech(body.getOrDefault("text", ""));
            return(ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).body(wav));
        }
        catch (Exception e)
        {
            log.error("Speech synthesis failed", e);
            return(ResponseEntity.internalServerError().build());
        }
    }
}
