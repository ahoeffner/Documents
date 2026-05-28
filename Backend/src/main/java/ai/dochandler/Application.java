package ai.dochandler;

import java.util.Arrays;
import ai.dochandler.config.Database;
import ai.dochandler.config.AdminRunner;
import ai.dochandler.entities.Folder;
import ai.dochandler.entities.Document;
import ai.dochandler.entities.Language;
import ai.dochandler.entities.ChatRequest;
import ai.dochandler.entities.ChatResponse;
import ai.dochandler.entities.LoginRequest;
import ai.dochandler.entities.SearchRequest;
import ai.dochandler.entities.CreateResponse;
import ai.dochandler.entities.DocumentDetail;
import ai.dochandler.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.jdbc.core.JdbcTemplate;


@SpringBootApplication
@RegisterReflectionForBinding
({
    ChatRequest.class,
    ChatResponse.class,
    CreateResponse.class,
    Document.class,
    DocumentDetail.class,
    Folder.class,
    Language.class,
    LoginRequest.class,
    SearchRequest.class
})

public class Application
{
    public static void main(String[] args) throws Exception
    {
        boolean daemon = Arrays.stream(args).anyMatch(a -> a.equals("-d") || a.equals("--daemon"));
        boolean aot    = Boolean.getBoolean("spring.aot.processing");

        if (!aot && !daemon)
        {
            if (args.length == 0 || Arrays.stream(args).anyMatch(a -> a.equals("-h") || a.equals("--help")))
            {
                AdminRunner.printHelp();
                return;
            }

            // Bypass Spring Boot for CLI commands: AOT-compiled MVC beans would fail
            // in a NONE-type context since their conditions were baked in at build time.
            runCli(args);
            return;
        }

        new SpringApplication(Application.class).run(args);
    }


    private static void runCli(String[] args) throws Exception
    {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(System.getenv("SPRING_DATASOURCE_URL"));
        ds.setUsername(System.getenv("SPRING_DATASOURCE_USERNAME"));
        ds.setPassword(System.getenv("SPRING_DATASOURCE_PASSWORD"));
        ds.setMaximumPoolSize(1);

        try
        {
            AdminRunner runner = new AdminRunner(new UserRepository(new JdbcTemplate(ds), new Database()));
            runner.run(args);
        }
        finally
        {
            ds.close();
        }
    }


}
