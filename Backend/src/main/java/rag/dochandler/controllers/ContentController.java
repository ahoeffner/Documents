package rag.dochandler.controllers;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import rag.dochandler.repository.DocumentRepository;


@RestController
@RequestMapping("/api/content")
public class ContentController
{
    private final DocumentRepository documentRepo;


    public ContentController(DocumentRepository documentRepo)
    {
        this.documentRepo = documentRepo;
    }


    @GetMapping(value = "/{id}/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getText(@PathVariable long id)
    {
        String text = documentRepo.getDescription(id);
        if (text == null) return(ResponseEntity.notFound().build());
        return(ResponseEntity.ok(text));
    }


    @GetMapping("/{id}/file")
    public void getFile(@PathVariable long id, HttpServletResponse response) throws IOException
    {
        String filename = documentRepo.getFilename(id);
        if (filename == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (filename.startsWith("http://") || filename.startsWith("https://"))
            response.sendRedirect(filename);
        else
            response.sendRedirect("/api/content/" + id + "/file/" + filename);
    }


    @GetMapping("/{id}/file/{filename}")
    public ResponseEntity<byte[]> getFileContent(@PathVariable long id, @PathVariable String filename)
    {
        byte[] content = documentRepo.getContent(id);
        if (content == null) return(ResponseEntity.notFound().build());
        return(ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .body(content));
    }


    @GetMapping("/{id}/download")
    public void download(@PathVariable long id, HttpServletResponse response) throws IOException
    {
        String filename = documentRepo.getFilename(id);
        if (filename == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (filename.startsWith("http://") || filename.startsWith("https://"))
            response.sendRedirect(filename);
        else
            response.sendRedirect("/api/content/" + id + "/download/" + filename);
    }


    @GetMapping("/{id}/download/{filename}")
    public ResponseEntity<byte[]> downloadContent(@PathVariable long id, @PathVariable String filename)
    {
        byte[] content = documentRepo.getContent(id);
        if (content == null) return(ResponseEntity.notFound().build());
        return(ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(content));
    }
}
