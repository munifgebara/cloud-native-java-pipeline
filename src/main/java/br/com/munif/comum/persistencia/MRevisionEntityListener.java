package br.com.munif.comum.persistencia;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener do Hibernate Envers responsável por enriquecer cada revisão com
 * informações de contexto — quem fez a alteração e de onde.
 *
 * <p>Este listener é chamado automaticamente pelo Envers antes de persistir
 * uma nova linha na tabela {@code versao} ({@link MRevisionEntity}).
 * Extrai o usuário autenticado do {@link SecurityContextHolder} e o IP
 * da requisição HTTP em andamento (quando disponível).</p>
 *
 * @see MRevisionEntity
 */
public class MRevisionEntityListener implements RevisionListener {

    /**
     * Preenche os campos de contexto da revisão antes que o Envers a persista.
     *
     * @param revisionEntity instância de {@link MRevisionEntity} criada pelo Envers
     *                       para representar o momento da auditoria
     */
    @Override
    public void newRevision(Object revisionEntity) {
        if (revisionEntity instanceof MRevisionEntity revisao) {
            revisao.setUsuario(resolverUsuario());
            revisao.setIp(resolverIp());
        }
    }

    /**
     * Retorna o nome do usuário autenticado no contexto de segurança do Spring,
     * ou {@code "anonimo"} quando não houver autenticação ativa.
     */
    private String resolverUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonimo";
        }
        return auth.getName();
    }

    /**
     * Retorna o endereço IP da requisição HTTP atual, ou {@code "desconhecido"}
     * quando chamado fora de um contexto de requisição web (ex.: tarefas agendadas).
     */
    private String resolverIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "desconhecido";
        }
        String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return attrs.getRequest().getRemoteAddr();
    }
}