package ai.dochandler.services;

import java.util.List;
import ai.dochandler.entities.Folder;
import org.springframework.stereotype.Service;
import ai.dochandler.repository.FolderRepository;


@Service
public class FolderService
{
    private final FolderRepository repo;


    public FolderService(FolderRepository repo)
    {
        this.repo = repo;
    }


    public List<Folder> list()
    {
        return(repo.findAll().stream()
            .map(f -> new Folder(f.getId(), f.getPid(), f.getName()))
            .toList());
    }


    public long create(String name, Long pid)
    {
        return(repo.create(name, pid));
    }


    public boolean existsById(long id)
    {
        return(repo.existsById(id));
    }


    public boolean deleteById(long id)
    {
        return(repo.deleteById(id));
    }
}
