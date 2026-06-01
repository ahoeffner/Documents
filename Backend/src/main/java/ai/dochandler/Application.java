package ai.dochandler;

import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import ai.dochandler.config.Database;
import ai.dochandler.entities.Folder;
import ai.dochandler.entities.Document;
import ai.dochandler.entities.Language;
import ai.dochandler.config.AdminRunner;
import ai.dochandler.entities.ChatRequest;
import com.zaxxer.hikari.HikariDataSource;
import ai.dochandler.entities.ChatResponse;
import ai.dochandler.entities.LoginRequest;
import ai.dochandler.entities.SearchRequest;
import ai.dochandler.entities.CreateResponse;
import ai.dochandler.entities.DocumentDetail;
import ai.dochandler.repository.UserRepository;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;


@SpringBootApplication
@ImportRuntimeHints(Application.TikaHints.class)
@RegisterReflectionForBinding
({
    Folder.class,
    Document.class,
    Language.class,
    ChatRequest.class,
    ChatResponse.class,
    LoginRequest.class,
    SearchRequest.class,
    CreateResponse.class,
    DocumentDetail.class
})

public class Application
{
    @SuppressWarnings("null")
    static class TikaHints implements RuntimeHintsRegistrar
    {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader)
        {
            hints.resources().registerPattern("org/apache/tika/mime/tika-mimetypes.xml");
            hints.resources().registerPattern("META-INF/services/org\\.apache\\.tika\\..*");

            MemberCategory[] all = { MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS };

            for (String svc : new String[]
            {
                "org.apache.tika.parser.Parser",
                "org.apache.tika.detect.Detector",
                "org.apache.tika.detect.EncodingDetector"
            })
            {
                try
                {
                    Enumeration<URL> urls = classLoader.getResources("META-INF/services/" + svc);
                    while (urls.hasMoreElements())
                    {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(urls.nextElement().openStream())))
                        {
                            r.lines()
                             .map(String::trim)
                             .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                             .forEach(cls -> hints.reflection().registerType(TypeReference.of(cls), all));
                        }
                    }
                }
                catch (Exception ignored) {}
            }
        }
    }


    public static void main(String[] args) throws Exception
    {
        boolean aot = Boolean.getBoolean("spring.aot.processing");
        boolean daemon = Arrays.stream(args).anyMatch(a -> a.equals("-d") || a.equals("--daemon"));

        if (!aot && !daemon)
        {
            if (args.length == 0 || Arrays.stream(args).anyMatch(a -> a.equals("-h") || a.equals("--help")))
            {
                AdminRunner.printHelp();
                return;
            }

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
            AdminRunner runner = new AdminRunner(new UserRepository(new JdbcTemplate(ds), new Database(new StandardEnvironment())));
            runner.run(args);
        }
        finally
        {
            ds.close();
        }
    }
}
