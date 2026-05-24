package rag.dochandler.model;

public class Category
{
    private long id;
    private Long pid;
    private String name;


    public Category(long id, Long pid, String name)
    {
        this.id = id;
        this.pid = pid;
        this.name = name;
    }


    public long getId()
    {
        return(id);
    }


    public Long getPid()
    {
        return(pid);
    }


    public String getName()
    {
        return(name);
    }
}
