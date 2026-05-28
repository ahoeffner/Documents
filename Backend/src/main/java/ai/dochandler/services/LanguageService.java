package ai.dochandler.services;

import java.util.List;
import ai.dochandler.entities.Language;
import org.springframework.stereotype.Service;
import ai.dochandler.repository.LanguageRepository;


@Service
public class LanguageService
{
    private final LanguageRepository repo;


    public LanguageService(LanguageRepository repo)
    {
        this.repo = repo;
    }


    public List<Language> list()
    {
        return(repo.findAll().stream()
            .map(l -> new Language(l.getId(), l.getName()))
            .toList());
    }
}
