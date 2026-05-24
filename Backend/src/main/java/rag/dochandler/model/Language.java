package rag.dochandler.model;

public class Language
{
    private String id;
    private String name;


    public Language(String id, String name)
    {
        this.id = id;
        this.name = name;
    }


    public String getId()
    {
        return(id);
    }


    public String getName()
    {
        return(name);
    }
}
