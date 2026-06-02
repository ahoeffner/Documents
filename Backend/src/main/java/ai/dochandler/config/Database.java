package ai.dochandler.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Component
public class Database
{
    private static final String USER_STORE_SCHEMA = "idm";

    private final Environment env;


    public Database(Environment env)
    {
        this.env = env;
    }


    public String getUserStoreSchema()
    {
        return(USER_STORE_SCHEMA);
    }


    public String getTenant()
    {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No request context");

        HttpServletRequest request = attrs.getRequest();
        var session = request.getSession(false);

        if (session != null)
        {
            String tenant = (String) session.getAttribute("tenant");
            if (tenant != null) return(tenant);
        }

        String host = request.getServerName();
        String tenant = env.getProperty("app.hosts." + host);
        if (tenant == null) throw new IllegalStateException("No tenant mapping for host: " + host);
        return(tenant);
    }
}
