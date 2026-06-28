package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V0003__create_embedding_outbox extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table public.main_item_embedding_outbox (
                        event_id uuid not null,
                        main_item_id uuid not null,
                        event_type varchar(20) not null,
                        owner_email varchar(150) not null,
                        owner_issuer varchar(300) not null,
                        status varchar(20) not null default 'PENDING',
                        attempts integer not null default 0,
                        last_error varchar(1000),
                        created_at timestamp with time zone not null default current_timestamp,
                        published_at timestamp with time zone,
                        constraint pk_main_item_embedding_outbox primary key (event_id),
                        constraint ck_main_item_embedding_outbox_type check (event_type in ('UPSERT', 'REMOVE')),
                        constraint ck_main_item_embedding_outbox_status check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
                    )
                    """);
            statement.execute("""
                    create index ix_main_item_embedding_outbox_pending
                    on public.main_item_embedding_outbox (status, created_at)
                    """);
            statement.execute("""
                    create index ix_main_item_embedding_outbox_item
                    on public.main_item_embedding_outbox (main_item_id, created_at)
                    """);
        }
    }
}
