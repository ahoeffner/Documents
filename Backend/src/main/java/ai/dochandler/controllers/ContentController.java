package ai.dochandler.controllers;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.*;
import ai.dochandler.repository.DocumentRepository;


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
        {
            response.sendRedirect(filename);
            return;
        }

        response.sendRedirect("/api/content/" + id + "/file/" + UriUtils.encodePathSegment(filename, "UTF-8"));
    }


    @GetMapping("/{id}/file/{filename}")
    public ResponseEntity<byte[]> getFileContent(@PathVariable long id, @PathVariable String filename)
    {
        byte[] content = documentRepo.getContent(id);
        if (content == null) return(ResponseEntity.notFound().build());

        String contentType = MediaTypeFactory.getMediaType(filename)
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return(ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
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
        {
            response.sendRedirect(filename);
            return;
        }

        response.sendRedirect("/api/content/" + id + "/download/" + UriUtils.encodePathSegment(filename, "UTF-8"));
    }


    @GetMapping("/{id}/download/{filename}")
    public ResponseEntity<byte[]> downloadContent(@PathVariable long id, @PathVariable String filename)
    {
        byte[] content = documentRepo.getContent(id);
        if (content == null) return(ResponseEntity.notFound().build());

        String contentType = MediaTypeFactory.getMediaType(filename)
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return(ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(content));
    }
}
