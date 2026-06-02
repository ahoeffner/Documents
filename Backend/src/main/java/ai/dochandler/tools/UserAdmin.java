package ai.dochandler.tools;

import java.sql.ResultSet;
import java.sql.Connection;
import java.util.HexFormat;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class UserAdmin
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            printUsage();
            System.exit(1);
        }

        String url  = System.getenv("SPRING_DATASOURCE_URL");
        String user = System.getenv("SPRING_DATASOURCE_USERNAME");
        String pass = System.getenv("SPRING_DATASOURCE_PASSWORD");

        try (Connection con = DriverManager.getConnection(url, user, pass))
        {
            switch (args[0])
            {
                case "create" -> create(con, args);
                case "grant"  -> grant(con, args);
                case "list"   -> list(con);
                default ->
                {
                    System.err.println("Unknown command: " + args[0]);
                    printUsage();
                    System.exit(1);
                }
            }
        }
    }


    private static void create(Connection con, String[] args) throws SQLException, NoSuchAlgorithmException
    {
        if (args.length < 3)
        {
            System.err.println("Usage: create <username> <password> [tenant...]");
            System.exit(1);
        }

        String username = args[1];
        String hash     = sha256(args[2]);

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO idm.users (username, password) VALUES (?, ?) ON CONFLICT (username) DO NOTHING RETURNING id"
        );
        ps.setString(1, username);
        ps.setString(2, hash);
        ResultSet rs = ps.executeQuery();

        if (!rs.next())
        {
            System.err.println("User already exists: " + username);
            return;
        }

        long uid = rs.getLong(1);
        System.out.println("Created user '" + username + "' (id=" + uid + ")");

        for (int i = 3; i < args.length; i++)
        {
            grantTenant(con, uid, args[i], false);
        }
    }


    private static void grant(Connection con, String[] args) throws SQLException
    {
        if (args.length < 3)
        {
            System.err.println("Usage: grant <username> <tenant> [--admin]");
            System.exit(1);
        }

        PreparedStatement ps = con.prepareStatement("SELECT id FROM idm.users WHERE username = ?");
        ps.setString(1, args[1]);
        ResultSet rs = ps.executeQuery();

        if (!rs.next())
        {
            System.err.println("User not found: " + args[1]);
            System.exit(1);
        }

        boolean admin = args.length > 3 && args[3].equals("--admin");
        grantTenant(con, rs.getLong(1), args[2], admin);
    }


    private static void list(Connection con) throws SQLException
    {
        ResultSet rs = con.createStatement().executeQuery(
            "SELECT u.username, t.tenant FROM idm.users u " +
            "LEFT JOIN idm.tenants t ON t.uid = u.id " +
            "ORDER BY u.username, t.tenant"
        );

        while (rs.next())
        {
            System.out.println(rs.getString("username") + " -> " + rs.getString("tenant"));
        }
    }


    private static void grantTenant(Connection con, long uid, String tenant, boolean admin) throws SQLException
    {
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO idm.tenants (uid, tenant, admin) VALUES (?, ?, ?) " +
            "ON CONFLICT (uid, tenant) DO UPDATE SET admin = EXCLUDED.admin"
        );
        ps.setLong(1, uid);
        ps.setString(2, tenant);
        ps.setBoolean(3, admin);
        ps.executeUpdate();
        System.out.println("Granted tenant '" + tenant + "'" + (admin ? " [admin]" : ""));
    }


    private static String sha256(String input) throws NoSuchAlgorithmException
    {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
        return(HexFormat.of().formatHex(bytes));
    }


    private static void printUsage()
    {
        System.err.println("Usage:");
        System.err.println("  UserAdmin create <username> <password> [tenant...]");
        System.err.println("  UserAdmin grant  <username> <tenant> [--admin]");
        System.err.println("  UserAdmin list");
    }
}
