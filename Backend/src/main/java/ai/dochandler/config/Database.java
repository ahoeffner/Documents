package ai.dochandler.config;

import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Component
@ConfigurationProperties(prefix = "app")
public class Database
{
    private static final String USER_STORE_SCHEMA = "idm";
    private Map<String, String> hosts = new HashMap<>();


    public void setHosts(Map<String, String> hosts)
    {
        this.hosts = hosts;
    }


    public String getUserStoreSchema()
    {
        return(USER_STORE_SCHEMA);
    }


    public String getTenant()
    {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No request context");

        HttpSession session = attrs.getRequest().getSession(false);
        if (session == null) throw new IllegalStateException("No active session");

        String tenant = (String) session.getAttribute("tenant");
        if (tenant == null) throw new IllegalStateException("No tenant in session");

        return(hosts.getOrDefault(tenant, tenant));
    }
}
