package ai.dochandler.entities;

public class CreateResponse
{
    private boolean success;
    private Long id;
    private String warning;


    public CreateResponse(boolean success, Long id)
    {
        this.success = success;
        this.id = id;
    }


    public CreateResponse(boolean success, Long id, String warning)
    {
        this.success = success;
        this.id = id;
        this.warning = warning;
    }


    public boolean isSuccess()
    {
        return(success);
    }


    public Long getId()
    {
        return(id);
    }


    public String getWarning()
    {
        return(warning);
    }
}
