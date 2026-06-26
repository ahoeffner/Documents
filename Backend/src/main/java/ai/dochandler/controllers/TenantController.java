package ai.dochandler.controllers;

import java.util.Map;
import ai.dochandler.config.Database;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api/tenant")
public class TenantController
{
    private final Database db;


    public TenantController(Database db)
    {
        this.db = db;
    }


    private String displayName(HttpServletRequest request)
    {
        String host = request.getServerName();
        String sub = host.split("\\.")[0];
        return(sub.substring(0, 1).toUpperCase() + sub.substring(1));
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> tenant(HttpServletRequest request)
    {
        String tenant = db.getTenant();
        return(ResponseEntity.ok(Map.of("success", true, "tenant", tenant, "name", displayName(request))));
    }


    @GetMapping("/manifest.webmanifest")
    public ResponseEntity<Map<String, Object>> manifest(HttpServletRequest request)
    {
        String name = displayName(request);
        String fullName = name + " AI Documents";

        return(ResponseEntity.ok(Map.of(
            "name", fullName,
            "short_name", name,
            "description", fullName,
            "theme_color", "#3b82f6",
            "background_color", "#ffffff",
            "display", "standalone",
            "start_url", "/",
            "icons", java.util.List.of(
                Map.of("src", "/icon-192.png", "sizes", "192x192", "type", "image/png"),
                Map.of("src", "/icon-512.png", "sizes", "512x512", "type", "image/png"),
                Map.of("src", "/icon-512.png", "sizes", "512x512", "type", "image/png", "purpose", "maskable")
            )
        )));
    }
}
