package ai.dochandler.services;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import ai.dochandler.repository.UserRepository;


@Service
public class AuthService
{
    private final UserRepository userRepo;


    public AuthService(UserRepository userRepo)
    {
        this.userRepo = userRepo;
    }


    public boolean login(String username, String passwordHash, String tenant, HttpSession session)
    {
        String stored = userRepo.findPasswordHash(username);
        if (stored == null || !stored.equals(passwordHash)) return(false);

        if (tenant != null && !tenant.isBlank())
        {
            List<String> tenants = userRepo.findTenants(username);
            if (!tenants.contains(tenant)) return(false);
            session.setAttribute("user", username);
            session.setAttribute("tenant", tenant);
            session.setAttribute("admin", userRepo.isAdmin(username, tenant));
        }
        else
        {
            session.setAttribute("user", username);
        }

        return(true);
    }


    public void logout(HttpSession session)
    {
        session.invalidate();
    }


    public boolean switchTenant(String tenant, HttpSession session)
    {
        String username = (String) session.getAttribute("user");
        if (username == null) return(false);

        List<String> tenants = userRepo.findTenants(username);
        if (!tenants.contains(tenant)) return(false);

        session.setAttribute("tenant", tenant);
        session.setAttribute("admin", userRepo.isAdmin(username, tenant));
        return(true);
    }


    public List<String> getTenants(HttpSession session)
    {
        String username = (String) session.getAttribute("user");
        if (username == null) return(List.of());
        return(userRepo.findTenants(username));
    }
}
