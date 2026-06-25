package br.com.munif.common.persistencia;

import br.com.munif.common.owner.OwnerContext;
import br.com.munif.common.owner.OwnerIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MRevisionEntityListenerTest {

    @AfterEach
    void clearOwner() {
        OwnerContext.clear();
    }

    @Test
    void shouldPopulateSocialOwnerIdentityInRevision() {
        OwnerContext.set(new OwnerIdentity("Social.User@Example.Local", "https://keycloak.example/realms/stella"));
        MRevisionEntity revision = new MRevisionEntity();

        new MRevisionEntityListener().newRevision(revision);

        assertThat(revision.getOwnerEmail()).isEqualTo("social.user@example.local");
        assertThat(revision.getOwnerIssuer()).isEqualTo("https://keycloak.example/realms/stella");
    }
}
