package ai.dochandler.controllers;

import ai.dochandler.services.OCRService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/ocr")
public class OCRController
{
    private final OCRService ocrService;


    public OCRController(OCRService ocrService)
    {
        this.ocrService = ocrService;
    }


    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ocr(@RequestParam("file") MultipartFile file)
    {
        try
        {
            String text = ocrService.scan(file.getBytes());
            return(ResponseEntity.ok(text));
        }
        catch (Exception e)
        {
            return(ResponseEntity.internalServerError().body("OCR failed: " + e.getMessage()));
        }
    }
}
