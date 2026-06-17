package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

public class V0018__criar_indice_vetorial_item_mestre extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);

        if (database.contains("postgresql")) {
            migratePostgreSql(connection);
            return;
        }

        migrateFallback(connection);
    }

    private void migratePostgreSql(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create extension if not exists vector");
            statement.execute("""
                    create table public.item_mestre_embedding (
                        item_mestre_id uuid not null,
                        provider varchar(50) not null,
                        modelo varchar(200) not null,
                        dimensoes integer not null,
                        texto_indexado text not null,
                        embedding vector(384) not null,
                        active boolean not null default true,
                        criado_em timestamp with time zone not null default now(),
                        alterado_em timestamp with time zone not null default now(),
                        constraint pk_item_mestre_embedding primary key (item_mestre_id),
                        constraint fk_item_mestre_embedding_item_mestre foreign key (item_mestre_id) references public.item_mestre (id) on delete cascade
                    )
                    """);
            statement.execute("create index ix_item_mestre_embedding_ativo on public.item_mestre_embedding (active)");
            statement.execute("create index ix_item_mestre_embedding_vector on public.item_mestre_embedding using hnsw (embedding vector_cosine_ops)");
        }
    }

    private void migrateFallback(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table public.item_mestre_embedding (
                        item_mestre_id uuid not null,
                        provider varchar(50) not null,
                        modelo varchar(200) not null,
                        dimensoes integer not null,
                        texto_indexado text not null,
                        embedding text not null,
                        active boolean not null default true,
                        criado_em timestamp with time zone not null default current_timestamp,
                        alterado_em timestamp with time zone not null default current_timestamp,
                        constraint pk_item_mestre_embedding primary key (item_mestre_id),
                        constraint fk_item_mestre_embedding_item_mestre foreign key (item_mestre_id) references public.item_mestre (id) on delete cascade
                    )
                    """);
            statement.execute("create index ix_item_mestre_embedding_ativo on public.item_mestre_embedding (active)");
        }
    }
}
