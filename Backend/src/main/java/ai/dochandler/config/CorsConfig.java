package ai.dochandler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;


@Configuration
@SuppressWarnings("null")
public class CorsConfig implements WebMvcConfigurer
{
    private final AuthInterceptor authInterceptor;


    public CorsConfig(AuthInterceptor authInterceptor)
    {
        this.authInterceptor = authInterceptor;
    }


    @Override
    public void addCorsMappings(CorsRegistry registry)
    {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
