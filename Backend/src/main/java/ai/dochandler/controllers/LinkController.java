package ai.dochandler.controllers;

import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import ai.dochandler.repository.LinkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/links")
public class LinkController
{
    private final LinkRepository linkRepo;


    public LinkController(LinkRepository linkRepo)
    {
        this.linkRepo = linkRepo;
    }


    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpSession session)
    {
        if (!Boolean.TRUE.equals(session.getAttribute("admin")))
            return(ResponseEntity.status(403).body(Map.of("success", false, "message", "Admin required")));

        long fldid = toLong(body.get("fldid"));

        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("docids");
        List<Long> docIds = raw.stream().map(LinkController::toLong).toList();

        linkRepo.createLinks(fldid, docIds);
        return(ResponseEntity.ok(Map.of("success", true)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable long id, HttpSession session)
    {
        if (!Boolean.TRUE.equals(session.getAttribute("admin")))
            return(ResponseEntity.status(403).body(Map.of("success", false, "message", "Admin required")));

        boolean ok = linkRepo.deleteLink(id);
        if (!ok) return(ResponseEntity.notFound().build());
        return(ResponseEntity.ok(Map.of("success", true)));
    }


    private static long toLong(Object o)
    {
        if (o instanceof Number) return(((Number) o).longValue());
        return(Long.parseLong(o.toString()));
    }
}
