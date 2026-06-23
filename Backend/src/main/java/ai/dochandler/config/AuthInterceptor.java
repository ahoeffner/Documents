package ai.dochandler.config;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthInterceptor implements HandlerInterceptor
{
    @Override
    @SuppressWarnings("null")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return(true);
        if (request.getRequestURI().startsWith("/api/auth/")) return(true);
        if (request.getRequestURI().startsWith("/api/i18n/")) return(true);
        if (request.getRequestURI().startsWith("/api/tenant")) return(true);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("tenant") == null)
        {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return(false);
        }

        return(true);
    }
}
