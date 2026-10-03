package app.webbangdiem.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Profile("local")
public class DemoHeaderAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String role = request.getHeader("X-Demo-Role");
        Actor actor = switch (role == null ? "" : role) {
            case "LECTURER" -> new Actor(DemoIdentity.LECTURER_ID, "Nguyễn Minh An", "LECTURER", DemoIdentity.DEPARTMENT_ID);
            case "DEPT_HEAD" -> new Actor(DemoIdentity.DEPARTMENT_HEAD_ID, "Trần Thu Hà", "DEPT_HEAD", DemoIdentity.DEPARTMENT_ID);
            case "EXAMINATION" -> new Actor(DemoIdentity.EXAMINATION_ID, "Lê Vân", "EXAMINATION", null);
            case "ADMIN" -> new Actor(DemoIdentity.ADMIN_ID, "Quản trị staging", "ADMIN", null);
            default -> null;
        };
        if (actor != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    actor, "local-demo", List.of(new SimpleGrantedAuthority("ROLE_" + actor.role())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
