package ai.dochandler.services;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import ai.dochandler.entities.Folder;
import org.springframework.stereotype.Service;
import ai.dochandler.repository.FolderRepository;
import ai.dochandler.repository.DocumentRepository;


@Service
public class FolderService
{
    private final FolderRepository repo;
    private final DocumentRepository documentRepo;


    public FolderService(FolderRepository repo, DocumentRepository documentRepo)
    {
        this.repo = repo;
        this.documentRepo = documentRepo;
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


    public boolean rename(long id, String name)
    {
        return(repo.rename(id, name));
    }


    public boolean move(long id, Long pid)
    {
        if (pid != null && pid == id) return(false);
        if (pid != null && isDescendant(id, pid)) return(false);
        return(repo.move(id, pid));
    }


    private boolean isDescendant(long ancestorId, long id)
    {
        Map<Long, Long> parentById = new HashMap<>();
        for (ai.dochandler.model.Folder f : repo.findAll()) parentById.put(f.getId(), f.getPid());

        Long current = id;
        while (true)
        {
            Long parent = parentById.get(current);
            if (parent == null) return(false);
            if (parent == ancestorId) return(true);
            current = parent;
        }
    }


    public boolean hasContent(long id)
    {
        return(repo.hasChildren(id) || documentRepo.hasDocuments(id));
    }


    public boolean deleteById(long id)
    {
        return(repo.deleteById(id));
    }
}
