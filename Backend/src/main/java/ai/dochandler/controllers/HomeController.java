package ai.dochandler.controllers;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.dochandler.services.HomeService;
import ai.dochandler.entities.HomeRequest;
import ai.dochandler.entities.ChatResponse;
import ai.dochandler.services.HomePanelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/home")
public class HomeController
{
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final HomeService homeService;
    private final HomePanelService panelService;


    public HomeController(HomeService homeService, HomePanelService panelService)
    {
        this.homeService = homeService;
        this.panelService = panelService;
    }


    @PostMapping
    public ResponseEntity<?> chat(@RequestBody HomeRequest req)
    {
        try
        {
            ChatResponse response = new ChatResponse();
            response.setSuccess(true);
            response.setResponse(homeService.chat(req.id(), req.query()));
            response.setDocuments(null);
            response.setRefresh(false);
            return(ResponseEntity.ok(response));
        }
        catch (Exception e)
        {
            log.error("Home request failed", e);
            return(ResponseEntity.internalServerError()
                .body(Map.of("success", false, "response", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }


    @GetMapping("/panel")
    public ResponseEntity<?> panel()
    {
        try
        {
            return(ResponseEntity.ok(Map.of("success", true, "items", panelService.items())));
        }
        catch (Exception e)
        {
            log.error("Home panel failed", e);
            return(ResponseEntity.internalServerError().body(Map.of("success", false, "response", message(e))));
        }
    }


    @PostMapping("/panel")
    public ResponseEntity<?> control(@RequestBody Map<String, Object> body)
    {
        try
        {
            String id = String.valueOf(body.get("id"));
            String action = String.valueOf(body.get("action"));
            Integer value = body.get("value") instanceof Number n ? n.intValue() : null;
            return(ResponseEntity.ok(Map.of("success", true, "item", panelService.control(id, action, value))));
        }
        catch (IllegalArgumentException e)
        {
            return(ResponseEntity.badRequest().body(Map.of("success", false, "response", message(e))));
        }
        catch (Exception e)
        {
            log.error("Home panel control failed", e);
            return(ResponseEntity.internalServerError().body(Map.of("success", false, "response", message(e))));
        }
    }


    private String message(Exception e)
    {
        return(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    }
}
