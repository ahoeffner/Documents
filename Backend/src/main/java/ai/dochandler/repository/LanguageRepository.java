package ai.dochandler.repository;

import java.util.List;
import ai.dochandler.model.Language;
import ai.dochandler.config.Database;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;


@Repository
public class LanguageRepository
{
    private final JdbcTemplate jdbc;
    private final Database db;


    public LanguageRepository(JdbcTemplate jdbc, Database db)
    {
        this.jdbc = jdbc;
        this.db = db;
    }


    public List<Language> findAll()
    {
        return(jdbc.query(
            "SELECT id, lang FROM " + languages() + " ORDER BY lang",
            (rs, i) -> new Language(rs.getString("id"), rs.getString("lang"))
        ));
    }


    private String languages()
    {
        return(db.getTenant() + ".languages");
    }
}
