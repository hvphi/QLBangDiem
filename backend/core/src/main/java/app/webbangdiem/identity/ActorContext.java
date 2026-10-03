package app.webbangdiem.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public final class ActorContext {
    private ActorContext() {}

    public static Actor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Actor actor) return actor;
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            Object roles = user.getClaims().getOrDefault("roles", user.getClaims().get("role"));
            java.util.Collection<?> values = roles instanceof java.util.Collection<?> collection ? collection : roles == null ? java.util.List.of() : java.util.List.of(roles);
            String role = values.stream().map(Object::toString).map(value -> value.replaceFirst("^ROLE_", ""))
                    .filter(value -> java.util.List.of("LECTURER", "DEPT_HEAD", "EXAMINATION", "ADMIN").contains(value))
                    .findFirst().orElse("UNASSIGNED");
            return new Actor(user.getSubject(), user.getFullName(), role, user.getClaimAsString("department_id"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục.");
    }

    public static Actor requireRole(String... roles) {
        Actor actor = current();
        if (Arrays.stream(roles).noneMatch(actor::hasRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không có quyền thực hiện thao tác này.");
        }
        return actor;
    }
}
