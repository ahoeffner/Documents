package ai.dochandler.controllers;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.dochandler.services.HomeService;
import ai.dochandler.entities.HomeRequest;
import ai.dochandler.entities.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/home")
public class HomeController
{
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final HomeService homeService;


    public HomeController(HomeService homeService)
    {
        this.homeService = homeService;
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
}
