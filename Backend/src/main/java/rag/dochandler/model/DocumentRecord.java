package rag.dochandler.model;

import java.sql.Date;
import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import rag.dochandler.services.GeminiService;
import dev.langchain4j.data.segment.TextSegment;


public class DocumentRecord
{
    private long id;
    private long catid;
    private Date date;
    private String lang;
    private String file;
    private String text;
    private String title;
    private byte[] content;

    private final ArrayList<String> chunks = new ArrayList<>();
    private final ArrayList<Float[]> embeddings = new ArrayList<>();


    public long getId()
    {
        return(id);
    }


    public DocumentRecord setId(long id)
    {
        this.id = id;
        return(this);
    }


    public long getCatid()
    {
        return(catid);
    }


    public DocumentRecord setCatid(long catid)
    {
        this.catid = catid;
        return(this);
    }


    public DocumentRecord setCatid(String catid)
    {
        this.catid = Long.parseLong(catid);
        return(this);
    }


    public Date getDate()
    {
        return(date);
    }


    public DocumentRecord setDate(Date date)
    {
        this.date = date;
        return(this);
    }


    public DocumentRecord setDate(String dateStr)
    {
        if (dateStr == null || dateStr.isBlank()) return(this);
        LocalDate local = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        this.date = Date.valueOf(local);
        return(this);
    }


    public String getLang()
    {
        return(lang);
    }


    public DocumentRecord setLang(String lang)
    {
        this.lang = lang;
        return(this);
    }


    public String getFile()
    {
        return(file);
    }


    public DocumentRecord setFile(String file)
    {
        this.file = file;
        return(this);
    }


    public String getText()
    {
        return(text);
    }


    public DocumentRecord setText(String text)
    {
        this.text = text;
        return(this);
    }


    public String getTitle()
    {
        return(title);
    }


    public DocumentRecord setTitle(String title)
    {
        this.title = title;
        return(this);
    }


    public byte[] getContent()
    {
        return(content);
    }


    public DocumentRecord setContent(byte[] content)
    {
        this.content = content;
        return(this);
    }


    public DocumentRecord addTextChunk(String chunk)
    {
        this.chunks.add(0, chunk);
        return(this);
    }


    public DocumentRecord addTextChunks(ArrayList<String> list)
    {
        this.chunks.addAll(list);
        return(this);
    }


    public DocumentRecord addTextChunks(List<TextSegment> list)
    {
        list.forEach(ts -> this.chunks.add(ts.text()));
        return(this);
    }


    public DocumentRecord createEmbeddings(GeminiService geminiService)
    {
        for (String chunk : this.chunks)
        {
            Float[] embedding = geminiService.computeEmbedding(chunk, false);
            this.embeddings.add(embedding);
        }
        return(this);
    }


    public ArrayList<String> getTextChunks()
    {
        return(chunks);
    }


    public ArrayList<Float[]> getEmbeddings()
    {
        return(embeddings);
    }


    @Override
    public String toString()
    {
        return("catid=" + catid + ", date=" + date + ", title=" + title +
               ", file=" + file + ", chunks=" + chunks.size() +
               ", content=" + (content == null ? 0 : content.length));
    }
}
