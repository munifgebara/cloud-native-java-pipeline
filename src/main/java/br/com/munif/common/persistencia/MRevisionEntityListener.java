package br.com.munif.common.persistencia;

import br.com.munif.common.owner.OwnerContext;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Hibernate Envers listener responsible for enriching each revision with
 * context information — who made the change and from where.
 *
 * <p>This listener is called automatically by Envers before persisting
 * a new row in the {@code revision} table ({@link MRevisionEntity}).
 * It extracts the authenticated user from {@link SecurityContextHolder} and the IP
 * from the current HTTP request (when available).</p>
 *
 * @see MRevisionEntity
 */
public class MRevisionEntityListener implements RevisionListener {

    /**
     * Populates the revision context fields before Envers persists it.
     *
     * @param revisionEntity instance of {@link MRevisionEntity} created by Envers
     *                       to represent the audit moment
     */
    @Override
    public void newRevision(Object revisionEntity) {
        if (revisionEntity instanceof MRevisionEntity revision) {
            revision.setUsername(resolveUsername());
            revision.setIp(resolveIp());
            OwnerContext.current().ifPresent(owner -> {
                revision.setOwnerEmail(owner.email());
                revision.setOwnerIssuer(owner.issuer());
            });
        }
    }

    /**
     * Returns the name of the authenticated user from the Spring security context,
     * or {@code "anonymous"} when there is no active authentication.
     */
    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return auth.getName();
    }

    /**
     * Returns the IP address of the current HTTP request, or {@code "unknown"}
     * when called outside a web request context (e.g.: scheduled tasks).
     */
    private String resolveIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return attrs.getRequest().getRemoteAddr();
    }
}
