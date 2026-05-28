package ai.dochandler.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import dev.langchain4j.model.Tokenizer;
import jakarta.annotation.PostConstruct;
import dev.langchain4j.data.document.Document;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.springframework.beans.factory.annotation.Value;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;


@Service
public class ChunkerService
{
    @Value("${app.chunker.max-tokens}")
    private int maxTokens;

    @Value("${app.chunker.overlap-size}")
    private int overlapSize;

    private Tokenizer tokenizer;


    @PostConstruct
    public void init()
    {
        tokenizer = new OpenAiTokenizer("gpt-3.5-turbo");
    }


    private DocumentByParagraphSplitter splitter()
    {
        return(new DocumentByParagraphSplitter(maxTokens, overlapSize, tokenizer,
            new DocumentByWordSplitter(maxTokens, overlapSize, tokenizer)));
    }


    public List<TextSegment> split(Document document)
    {
        return(splitter().split(document));
    }


    public ArrayList<String> split(String text)
    {
        String[] parts = splitter().split(text);
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, parts);
        return(list);
    }
}
