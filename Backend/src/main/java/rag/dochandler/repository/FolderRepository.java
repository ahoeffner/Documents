package rag.dochandler.repository;

import java.util.List;
import java.sql.Types;
import java.sql.PreparedStatement;
import rag.dochandler.model.Folder;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;


@Repository
public class FolderRepository
{
    private final JdbcTemplate jdbc;


    public FolderRepository(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }


    public List<Folder> findAll()
    {
        return(jdbc.query(
            "SELECT id, pid, name FROM folders ORDER BY name",
            (rs, i) -> new Folder(rs.getLong("id"), (Long) rs.getObject("pid"), rs.getString("name"))
        ));
    }


    public long create(String name, Long pid)
    {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con ->
        {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO folders (pid, name) VALUES (?, ?)",
                new String[]{"id"}
            );
            if (pid != null) ps.setLong(1, pid);
            else ps.setNull(1, Types.BIGINT);
            ps.setString(2, name);
            return(ps);
        }, holder);
        return(holder.getKey().longValue());
    }


    public boolean existsById(long id)
    {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM folders WHERE id = ?", Integer.class, id);
        return(count != null && count > 0);
    }


    public boolean deleteById(long id)
    {
        return(jdbc.update("DELETE FROM folders WHERE id = ?", id) > 0);
    }


    public boolean deleteByName(String name)
    {
        return(jdbc.update("DELETE FROM folders WHERE upper(name) = upper(?)", name) > 0);
    }


    public Folder findByName(String name)
    {
        if (name == null) return(null);
        List<Folder> results = jdbc.query(
            "SELECT id, pid, name FROM folders WHERE upper(name) LIKE upper(?)",
            (rs, i) -> new Folder(rs.getLong("id"), (Long) rs.getObject("pid"), rs.getString("name")),
            name
        );
        return(results.isEmpty() ? null : results.get(0));
    }
}
