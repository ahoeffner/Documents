package rag.dochandler.services;

import java.util.List;
import rag.dochandler.entities.Category;
import org.springframework.stereotype.Service;
import rag.dochandler.repository.CategoryRepository;


@Service
public class CategoryService
{
    private final CategoryRepository repo;


    public CategoryService(CategoryRepository repo)
    {
        this.repo = repo;
    }


    public List<Category> list()
    {
        return(repo.findAll().stream()
            .map(c -> new Category(c.getId(), c.getPid(), c.getName()))
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
