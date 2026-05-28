package ai.dochandler.repository;

import java.util.List;
import java.sql.Types;
import ai.dochandler.model.Folder;
import java.sql.PreparedStatement;
import ai.dochandler.config.Database;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;


@Repository
public class FolderRepository
{
    private final JdbcTemplate jdbc;
    private final Database db;


    public FolderRepository(JdbcTemplate jdbc, Database db)
    {
        this.jdbc = jdbc;
        this.db = db;
    }


    public List<Folder> findAll()
    {
        return(jdbc.query(
            "SELECT id, pid, name FROM " + folders() + " ORDER BY name",
            (rs, i) -> { Number pid = (Number) rs.getObject("pid"); return(new Folder(rs.getLong("id"), pid != null ? pid.longValue() : null, rs.getString("name"))); }
        ));
    }


    public long create(String name, Long pid)
    {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con ->
        {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO " + folders() + " (pid, name) VALUES (?, ?)",
                new String[]{"id"}
            );
            if (pid != null) ps.setLong(1, pid);
            else ps.setNull(1, Types.BIGINT);
            ps.setString(2, name);
            return(ps);
        }, holder);
        @SuppressWarnings("null")
        long id = holder.getKey().longValue();
        return(id);
    }


    public boolean existsById(long id)
    {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + folders() + " WHERE id = ?", Integer.class, id);
        return(count != null && count > 0);
    }


    public boolean deleteById(long id)
    {
        return(jdbc.update("DELETE FROM " + folders() + " WHERE id = ?", id) > 0);
    }


    public boolean rename(long id, String name)
    {
        return(jdbc.update("UPDATE " + folders() + " SET name = ? WHERE id = ?", name, id) > 0);
    }


    public boolean deleteByName(String name)
    {
        return(jdbc.update("DELETE FROM " + folders() + " WHERE upper(name) = upper(?)", name) > 0);
    }


    public Folder findByName(String name)
    {
        if (name == null) return(null);
        List<Folder> results = jdbc.query(
            "SELECT id, pid, name FROM " + folders() + " WHERE upper(name) LIKE upper(?)",
            (rs, i) -> { Number pid = (Number) rs.getObject("pid"); return(new Folder(rs.getLong("id"), pid != null ? pid.longValue() : null, rs.getString("name"))); },
            name
        );
        return(results.isEmpty() ? null : results.get(0));
    }


    private String folders()
    {
        return(db.getPrefix() + ".folders");
    }
}
