package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

public class V0019__criar_metricas_consulta_vetorial extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);

        try (Statement statement = connection.createStatement()) {
            if (database.contains("postgresql")) {
                statement.execute("""
                        create table public.consulta_vetorial_metrica (
                            id uuid not null,
                            consulta varchar(1000) not null,
                            quantidade_resultados integer not null,
                            criado_em timestamp with time zone not null default now(),
                            constraint pk_consulta_vetorial_metrica primary key (id)
                        )
                        """);
            } else {
                statement.execute("""
                        create table public.consulta_vetorial_metrica (
                            id uuid not null,
                            consulta varchar(1000) not null,
                            quantidade_resultados integer not null,
                            criado_em timestamp with time zone not null default current_timestamp,
                            constraint pk_consulta_vetorial_metrica primary key (id)
                        )
                        """);
            }
        }
    }
}
