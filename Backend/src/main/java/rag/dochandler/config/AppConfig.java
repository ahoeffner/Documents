package rag.dochandler.config;

import java.time.Duration;
import java.net.http.HttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AppConfig
{
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
