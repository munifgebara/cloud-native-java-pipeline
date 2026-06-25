package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;
import java.util.List;

public class V0002__add_owner_scope extends BaseJavaMigration {

    private static final String BACKFILL_OWNER_EMAIL = "admin@example.local";
    private static final String BACKFILL_OWNER_ISSUER = "https://keycloak.gebaralabs.dev/realms/stella";

    private static final List<String> DOMAIN_TABLES = List.of(
            "person",
            "category",
            "storage_location",
            "main_item",
            "item_instance",
            "item_movement",
            "item_loan"
    );

    private static final List<String> AUDIT_TABLES = List.of(
            "person_aud",
            "category_aud",
            "storage_location_aud",
            "main_item_aud",
            "item_instance_aud",
            "item_movement_aud",
            "item_loan_aud"
    );

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            addRevisionOwnerColumns(statement);
            for (String table : DOMAIN_TABLES) {
                addOwnerColumns(statement, table);
                createOwnerIndex(statement, table);
            }
            for (String table : AUDIT_TABLES) {
                addAuditOwnerColumns(statement, table);
            }
        }
    }

    private void addRevisionOwnerColumns(Statement statement) throws Exception {
        statement.execute("alter table public.revision add column owner_email varchar(150)");
        statement.execute("alter table public.revision add column owner_issuer varchar(300)");
    }

    private void addOwnerColumns(Statement statement, String table) throws Exception {
        statement.execute("""
                alter table public.%s
                add column owner_email varchar(150) default '%s' not null
                """.formatted(table, BACKFILL_OWNER_EMAIL));
        statement.execute("""
                alter table public.%s
                add column owner_issuer varchar(300) default '%s' not null
                """.formatted(table, BACKFILL_OWNER_ISSUER));
        statement.execute("""
                alter table public.%s
                add column owner_public boolean default false not null
                """.formatted(table));
    }

    private void addAuditOwnerColumns(Statement statement, String table) throws Exception {
        statement.execute("alter table public.%s add column owner_email varchar(150)".formatted(table));
        statement.execute("alter table public.%s add column owner_issuer varchar(300)".formatted(table));
        statement.execute("alter table public.%s add column owner_public boolean".formatted(table));
    }

    private void createOwnerIndex(Statement statement, String table) throws Exception {
        statement.execute("""
                create index ix_%s_owner_scope
                on public.%s (owner_issuer, owner_email, owner_public, active)
                """.formatted(table, table));
    }
}
