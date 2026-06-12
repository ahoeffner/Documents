package ai.dochandler.controllers;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.dochandler.services.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/transcribe")
public class TranscribeController
{
    private static final Logger log = LoggerFactory.getLogger(TranscribeController.class);

    private final GeminiService geminiService;


    public TranscribeController(GeminiService geminiService)
    {
        this.geminiService = geminiService;
    }


    @PostMapping
    public ResponseEntity<?> transcribe(@RequestParam("audio") MultipartFile audio)
    {
        try
        {
            String text = geminiService.transcribe(audio.getBytes(), audio.getContentType());
            return(ResponseEntity.ok(Map.of("text", text)));
        }
        catch (Exception e)
        {
            log.error("Transcription failed", e);
            return(ResponseEntity.internalServerError()
                .body(Map.of("text", "", "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }
}
