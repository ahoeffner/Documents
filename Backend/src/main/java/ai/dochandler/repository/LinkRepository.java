package ai.dochandler.repository;

import java.util.List;
import ai.dochandler.config.Database;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;


@Repository
public class LinkRepository
{
    private final JdbcTemplate jdbc;
    private final Database db;


    public LinkRepository(JdbcTemplate jdbc, Database db)
    {
        this.jdbc = jdbc;
        this.db = db;
    }


    public void createLinks(long fldid, List<Long> docIds)
    {
        for (Long docId : docIds)
            jdbc.update(
                "INSERT INTO " + links() + " (fldid, docid) SELECT ?, ? WHERE NOT EXISTS" +
                " (SELECT 1 FROM " + links() + " WHERE fldid = ? AND docid = ?)" +
                " AND NOT EXISTS (SELECT 1 FROM " + docs() + " WHERE id = ? AND fldid = ?)",
                fldid, docId, fldid, docId, docId, fldid
            );
    }


    public boolean deleteLink(long id)
    {
        return(jdbc.update("DELETE FROM " + links() + " WHERE id = ?", id) > 0);
    }


    private String links()
    {
        return(db.getTenant() + ".links");
    }


    private String docs()
    {
        return(db.getTenant() + ".documents");
    }
}
