package ai.dochandler.controllers;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.dochandler.services.ChatService;
import ai.dochandler.entities.ChatRequest;
import ai.dochandler.entities.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/ai")
public class AIChatController
{
    private static final Logger log = LoggerFactory.getLogger(AIChatController.class);

    private final ChatService chatService;


    public AIChatController(ChatService chatService)
    {
        this.chatService = chatService;
    }


    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest req)
    {
        try
        {
            ChatResponse response = chatService.chat(req);
            return(ResponseEntity.ok(response));
        }
        catch (Exception e)
        {
            log.error("Chat request failed", e);
            return(ResponseEntity.internalServerError()
                .body(Map.of("success", false, "response", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }
}
