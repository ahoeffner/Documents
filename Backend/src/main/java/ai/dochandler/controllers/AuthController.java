package ai.dochandler.controllers;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import jakarta.servlet.http.HttpSession;
import ai.dochandler.services.AuthService;
import ai.dochandler.entities.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    private final AuthService service;


    public AuthController(AuthService service)
    {
        this.service = service;
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req, HttpSession session)
    {
        boolean ok = service.login(req.username(), req.password(), req.tenant(), session);
        if (!ok) return(ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials or tenant")));
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        if (session.getAttribute("tenant") != null)
            body.put("admin", session.getAttribute("admin"));
        else
            body.put("tenants", service.getTenants(session));
        return(ResponseEntity.ok(body));
    }


    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session)
    {
        service.logout(session);
        return(ResponseEntity.ok(Map.of("success", true)));
    }


    @GetMapping("/tenants")
    public ResponseEntity<Map<String, Object>> tenants(HttpSession session)
    {
        List<String> tenants = service.getTenants(session);
        return(ResponseEntity.ok(Map.of("success", true, "tenants", tenants)));
    }


    @PostMapping("/switch")
    public ResponseEntity<Map<String, Object>> switchTenant(@RequestBody Map<String, Object> body, HttpSession session)
    {
        String tenant = (String) body.get("tenant");
        boolean ok = service.switchTenant(tenant, session);
        if (!ok) return(ResponseEntity.status(403).body(Map.of("success", false, "message", "Tenant not allowed")));
        return(ResponseEntity.ok(Map.of("success", true, "admin", session.getAttribute("admin"))));
    }
}
