package ai.dochandler.config;

import java.util.List;
import java.util.Arrays;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ai.dochandler.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;


@Component
public class AdminRunner implements CommandLineRunner
{
    private final UserRepository userRepo;


    public AdminRunner(UserRepository userRepo)
    {
        this.userRepo = userRepo;
    }


    @Override
    public void run(String... args) throws Exception
    {
        if (Arrays.stream(args).anyMatch(a -> a.equals("-d") || a.equals("--daemon"))) return;

        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help"))
        {
            printHelp();
            System.exit(0);
            return;
        }

        switch (args[0])
        {
            case "create" -> create(args);
            case "delete" -> delete(args);
            case "grant"  -> grant(args);
            case "revoke" -> revoke(args);
            case "list"   -> list();
            default ->
            {
                System.err.println("Unknown command: " + args[0]);
                System.exit(1);
            }
        }

        System.exit(0);
    }


    private void create(String[] args) throws NoSuchAlgorithmException
    {
        if (args.length < 3) { System.err.println("Usage: create <user> <pass> [tenant...]"); System.exit(1); }

        String username = args[1];
        String hash     = sha256(args[2]);
        long   uid      = userRepo.createUser(username, hash);

        System.out.println("Created user '" + username + "'");

        for (int i = 3; i < args.length; i++)
        {
            userRepo.grantTenant(uid, args[i], false);
            System.out.println("Granted tenant '" + args[i] + "'");
        }
    }


    private void delete(String[] args)
    {
        if (args.length < 2) { System.err.println("Usage: delete <user>"); System.exit(1); }

        boolean ok = userRepo.deleteUser(args[1]);
        if (!ok) { System.err.println("User not found: " + args[1]); System.exit(1); }
        System.out.println("Deleted user '" + args[1] + "'");
    }


    @SuppressWarnings("null")
    private void grant(String[] args)
    {
        if (args.length < 3) { System.err.println("Usage: grant <user> <tenant> [--admin]"); System.exit(1); }

        Long    uid   = userRepo.findUserId(args[1]);
        if (uid == null) { System.err.println("User not found: " + args[1]); System.exit(1); }

        boolean admin = args.length > 3 && args[3].equals("--admin");
        userRepo.grantTenant(uid, args[2], admin);
        System.out.println("Granted tenant '" + args[2] + "' to '" + args[1] + "'" + (admin ? " [admin]" : ""));
    }


    @SuppressWarnings("null")
    private void revoke(String[] args)
    {
        if (args.length < 3) { System.err.println("Usage: revoke <user> <tenant>"); System.exit(1); }

        Long uid = userRepo.findUserId(args[1]);
        if (uid == null) { System.err.println("User not found: " + args[1]); System.exit(1); }

        userRepo.revokeTenant(uid, args[2]);
        System.out.println("Revoked tenant '" + args[2] + "' from '" + args[1] + "'");
    }


    private void list()
    {
        List<String[]> rows = userRepo.listUsersWithTenants();
        if (rows.isEmpty()) { System.out.println("No users found."); return; }
        rows.forEach(r -> System.out.println(r[0] + " -> " + r[1] + (Boolean.parseBoolean(r[2]) ? " [admin]" : "")));
    }


    private static String sha256(String input) throws NoSuchAlgorithmException
    {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
        return(HexFormat.of().formatHex(bytes));
    }


    public static void printHelp()
    {
        System.out.println("Usage: documents <command>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  -d, --daemon                         Start the HTTP server");
        System.out.println("  create <user> <pass> [tenant...]     Create a user");
        System.out.println("  delete <user>                        Delete a user");
        System.out.println("  grant  <user> <tenant>               Grant tenant access");
        System.out.println("  revoke <user> <tenant>               Revoke tenant access");
        System.out.println("  list                                 List all users and their tenants");
    }
}
