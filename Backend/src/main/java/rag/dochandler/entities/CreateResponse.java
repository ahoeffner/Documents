package rag.dochandler.entities;

public class CreateResponse
{
    private boolean success;
    private Long id;


    public CreateResponse(boolean success, Long id)
    {
        this.success = success;
        this.id = id;
    }


    public boolean isSuccess()
    {
        return(success);
    }


    public Long getId()
    {
        return(id);
    }
}
