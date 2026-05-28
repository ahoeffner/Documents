package ai.dochandler.repository;

import java.util.List;
import java.sql.PreparedStatement;
import ai.dochandler.config.Database;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.GeneratedKeyHolder;


@Repository
public class UserRepository
{
    private final Database db;
    private final JdbcTemplate jdbc;


    public UserRepository(JdbcTemplate jdbc, Database db)
    {
        this.db = db;
        this.jdbc = jdbc;
    }


    public String findPasswordHash(String username)
    {
        String s = db.getUserStoreSchema();

        List<String> results = jdbc.query
        (
            "SELECT password FROM " + s + ".users WHERE username = ?",
            (rs, i) -> rs.getString("password"), username
        );

        return(results.isEmpty() ? null : results.get(0));
    }


    public List<String> findTenants(String username)
    {
        String s = db.getUserStoreSchema();

        return(jdbc.query
        (
            "SELECT t.tenant FROM " + s + ".user_tenants t " +
            "JOIN " + s + ".users u ON u.id = t.user_id " +
            "WHERE u.username = ?",
            (rs, i) -> rs.getString("tenant"), username
        ));
    }


    public boolean isAdmin(String username, String tenant)
    {
        String s = db.getUserStoreSchema();

        List<Boolean> results = jdbc.query(
            "SELECT t.admin FROM " + s + ".user_tenants t " +
            "JOIN " + s + ".users u ON u.id = t.user_id " +
            "WHERE u.username = ? AND t.tenant = ?",
            (rs, i) -> rs.getBoolean("admin"), username, tenant
        );

        return(!results.isEmpty() && results.get(0));
    }


    public long createUser(String username, String passwordHash)
    {
        String s = db.getUserStoreSchema();
        KeyHolder holder = new GeneratedKeyHolder();

        jdbc.update(con ->
        {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO " + s + ".users (username, password) VALUES (?, ?) RETURNING id",
                new String[]{"id"}
            );
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            return(ps);
        }, holder);

        @SuppressWarnings("null")
        long id = holder.getKey().longValue();

        return(id);
    }


    public Long findUserId(String username)
    {
        String s = db.getUserStoreSchema();

        List<Long> results = jdbc.query
        (
            "SELECT id FROM " + s + ".users WHERE username = ?",
            (rs, i) -> rs.getLong("id"), username
        );

        return(results.isEmpty() ? null : results.get(0));
    }


    public boolean deleteUser(String username)
    {
        String s = db.getUserStoreSchema();
        return(jdbc.update("DELETE FROM " + s + ".users WHERE username = ?", username) > 0);
    }


    public void grantTenant(long userId, String tenant, boolean admin)
    {
        String s = db.getUserStoreSchema();

        jdbc.update
        (
            "INSERT INTO " + s + ".user_tenants (user_id, tenant, admin) VALUES (?, ?, ?) " +
            "ON CONFLICT (user_id, tenant) DO UPDATE SET admin = EXCLUDED.admin",
            userId, tenant, admin
        );
    }


    public void revokeTenant(long userId, String tenant)
    {
        String s = db.getUserStoreSchema();

        jdbc.update
        (
            "DELETE FROM " + s + ".user_tenants WHERE user_id = ? AND tenant = ?",
            userId, tenant
        );
    }


    public List<String[]> listUsersWithTenants()
    {
        String s = db.getUserStoreSchema();
        return(jdbc.query
        (
            "SELECT u.username, t.tenant, t.admin FROM " + s + ".users u " +
            "LEFT JOIN " + s + ".user_tenants t ON t.user_id = u.id " +
            "ORDER BY u.username, t.tenant",
            (rs, i) -> new String[]{rs.getString("username"), rs.getString("tenant"), String.valueOf(rs.getBoolean("admin"))}
        ));
    }
}
