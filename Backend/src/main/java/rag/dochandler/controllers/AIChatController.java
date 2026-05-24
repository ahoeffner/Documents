package rag.dochandler.controllers;

import java.util.Map;
import rag.dochandler.entities.ChatRequest;
import rag.dochandler.services.ChatService;
import rag.dochandler.entities.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/ai")
public class AIChatController
{
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
            return(ResponseEntity.internalServerError()
                .body(Map.of("success", false, "response", e.getMessage())));
        }
    }
}
