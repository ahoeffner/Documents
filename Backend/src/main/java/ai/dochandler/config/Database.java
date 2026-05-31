package ai.dochandler.config;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Component
public class Database
{
    private static final String USER_STORE_SCHEMA = "idm";

    private final static Map<String, String> hosts = Map.of
    (
        "localhost", "documents",
        "home.hoeffner.net", "documents",
        "private.hoeffner.net", "documents",
        "slotsdalen.hoeffner.net", "slotsdalen"
    );


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
