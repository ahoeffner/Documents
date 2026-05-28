package ai.dochandler.entities;

import java.util.List;


public class ChatResponse
{
    private boolean success;
    private String response;
    private List<Document> documents;
    private boolean refresh;


    public ChatResponse()
    {
    }


    public boolean isSuccess()
    {
        return(success);
    }


    public void setSuccess(boolean success)
    {
        this.success = success;
    }


    public String getResponse()
    {
        return(response);
    }


    public void setResponse(String response)
    {
        this.response = response;
    }


    public List<Document> getDocuments()
    {
        return(documents);
    }


    public void setDocuments(List<Document> documents)
    {
        this.documents = documents;
    }


    public boolean isRefresh()
    {
        return(refresh);
    }


    public void setRefresh(boolean refresh)
    {
        this.refresh = refresh;
    }
}
