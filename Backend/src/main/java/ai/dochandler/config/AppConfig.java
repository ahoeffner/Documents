package ai.dochandler.config;

import java.time.Duration;
import javax.sql.DataSource;
import java.net.http.HttpClient;
import com.zaxxer.hikari.HikariDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AppConfig
{
    @Value("${database.url}")
    private String dbUrl;

    @Value("${database.username}")
    private String dbUsername;

    @Value("${database.password}")
    private String dbPassword;

    @Value("${database.minimum-idle:1}")
    private int dbMinIdle;

    @Value("${database.maximum-pool-size:3}")
    private int dbMaxPool;


    @Bean
    public DataSource dataSource()
    {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setMinimumIdle(dbMinIdle);
        ds.setMaximumPoolSize(dbMaxPool);
        return(ds);
    }


    @Bean
    public HttpClient httpClient()
    {
        return(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build());
    }


    @Bean
    public ObjectMapper objectMapper()
    {
        return(new ObjectMapper());
    }
}
