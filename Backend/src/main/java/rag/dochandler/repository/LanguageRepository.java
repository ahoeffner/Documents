package rag.dochandler.repository;

import java.util.List;
import rag.dochandler.model.Language;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;


@Repository
public class LanguageRepository
{
    private final JdbcTemplate jdbc;


    public LanguageRepository(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }


    public List<Language> findAll()
    {
        return(jdbc.query(
            "SELECT id, lang FROM languages ORDER BY lang",
            (rs, i) -> new Language(rs.getString("id"), rs.getString("lang"))
        ));
    }
}
