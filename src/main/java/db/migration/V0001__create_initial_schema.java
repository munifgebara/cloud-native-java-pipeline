package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

public class V0001__create_initial_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean postgresql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase(Locale.ROOT)
                .contains("postgresql");

        try (Statement statement = connection.createStatement()) {
            if (postgresql) {
                statement.execute("create extension if not exists vector");
            }

            createAuditSchema(statement);
            createPersonSchema(statement);
            createCategorySchema(statement);
            createStorageLocationSchema(statement);
            createMainItemSchema(statement);
            createItemInstanceSchema(statement);
            createItemMovementSchema(statement);
            createItemLoanSchema(statement);
            createVectorSearchSchema(statement, postgresql);
        }
    }

    private void createAuditSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.revision (
                    id bigserial not null,
                    timestamp timestamp with time zone not null,
                    ip varchar(45),
                    username varchar(100),
                    external_id varchar(100),
                    constraint pk_revision primary key (id)
                )
                """);
    }

    private void createPersonSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.person (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150) not null,
                    tax_id varchar(14) not null,
                    primary_phone varchar(20),
                    secondary_phone varchar(20),
                    email varchar(150),
                    zip_code varchar(8),
                    address varchar(200),
                    complement varchar(100),
                    neighborhood varchar(100),
                    city varchar(100),
                    state varchar(2),
                    photo_bucket varchar(100),
                    photo_object_key varchar(500),
                    photo_content_type varchar(100),
                    photo_size_bytes bigint,
                    constraint pk_person primary key (id),
                    constraint uk_person_tax_id unique (tax_id)
                )
                """);
        statement.execute("create index ix_person_name on public.person (name)");
        statement.execute("create index ix_person_external_id on public.person (external_id)");
        statement.execute("create index ix_person_active on public.person (active)");
        statement.execute("create index ix_person_photo_object_key on public.person (photo_object_key)");

        statement.execute("""
                create table public.person_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150),
                    tax_id varchar(14),
                    primary_phone varchar(20),
                    secondary_phone varchar(20),
                    email varchar(150),
                    zip_code varchar(8),
                    address varchar(200),
                    complement varchar(100),
                    neighborhood varchar(100),
                    city varchar(100),
                    state varchar(2),
                    photo_bucket varchar(100),
                    photo_object_key varchar(500),
                    photo_content_type varchar(100),
                    photo_size_bytes bigint,
                    constraint pk_person_aud primary key (rev, id),
                    constraint fk_person_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_person_aud_id on public.person_aud (id)");
        statement.execute("create index ix_person_aud_rev on public.person_aud (rev)");
    }

    private void createCategorySchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.category (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150) not null,
                    description varchar(500),
                    icon varchar(50),
                    constraint pk_category primary key (id)
                )
                """);
        statement.execute("create index ix_category_name on public.category (name)");
        statement.execute("create index ix_category_external_id on public.category (external_id)");
        statement.execute("create index ix_category_active on public.category (active)");

        statement.execute("""
                create table public.category_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150),
                    description varchar(500),
                    icon varchar(50),
                    constraint pk_category_aud primary key (rev, id),
                    constraint fk_category_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_category_aud_id on public.category_aud (id)");
        statement.execute("create index ix_category_aud_rev on public.category_aud (rev)");
    }

    private void createStorageLocationSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.storage_location (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150) not null,
                    description varchar(500),
                    image_bucket varchar(100),
                    image_object_key varchar(500),
                    image_content_type varchar(100),
                    image_size_bytes bigint,
                    parent_location_id uuid,
                    constraint pk_storage_location primary key (id),
                    constraint fk_storage_location_parent foreign key (parent_location_id) references public.storage_location (id)
                )
                """);
        statement.execute("create index ix_storage_location_name on public.storage_location (name)");
        statement.execute("create index ix_storage_location_external_id on public.storage_location (external_id)");
        statement.execute("create index ix_storage_location_active on public.storage_location (active)");
        statement.execute("create index ix_storage_location_parent on public.storage_location (parent_location_id)");
        statement.execute("create index ix_storage_location_image_object_key on public.storage_location (image_object_key)");

        statement.execute("""
                create table public.storage_location_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150),
                    description varchar(500),
                    image_bucket varchar(100),
                    image_object_key varchar(500),
                    image_content_type varchar(100),
                    image_size_bytes bigint,
                    parent_location_id uuid,
                    constraint pk_storage_location_aud primary key (rev, id),
                    constraint fk_storage_location_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_storage_location_aud_id on public.storage_location_aud (id)");
        statement.execute("create index ix_storage_location_aud_rev on public.storage_location_aud (rev)");
    }

    private void createMainItemSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.main_item (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150) not null,
                    description varchar(500),
                    notes varchar(1000),
                    registration_origin varchar(50),
                    image_bucket varchar(100),
                    image_object_key varchar(500),
                    image_content_type varchar(100),
                    image_size_bytes bigint,
                    image_generated_by_ai boolean not null,
                    image_provider varchar(50),
                    category_id uuid,
                    constraint pk_main_item primary key (id),
                    constraint fk_main_item_category foreign key (category_id) references public.category (id)
                )
                """);
        statement.execute("create index ix_main_item_name on public.main_item (name)");
        statement.execute("create index ix_main_item_category on public.main_item (category_id)");
        statement.execute("create index ix_main_item_external_id on public.main_item (external_id)");
        statement.execute("create index ix_main_item_active on public.main_item (active)");
        statement.execute("create index ix_main_item_image_object_key on public.main_item (image_object_key)");

        statement.execute("""
                create table public.main_item_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    name varchar(150),
                    description varchar(500),
                    notes varchar(1000),
                    registration_origin varchar(50),
                    image_bucket varchar(100),
                    image_object_key varchar(500),
                    image_content_type varchar(100),
                    image_size_bytes bigint,
                    image_generated_by_ai boolean,
                    image_provider varchar(50),
                    category_id uuid,
                    constraint pk_main_item_aud primary key (rev, id),
                    constraint fk_main_item_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_main_item_aud_id on public.main_item_aud (id)");
        statement.execute("create index ix_main_item_aud_rev on public.main_item_aud (rev)");
    }

    private void createItemInstanceSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.item_instance (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    main_item_id uuid not null,
                    current_location_id uuid,
                    identifier varchar(100),
                    asset_tag varchar(100),
                    serial_number varchar(150),
                    operational_status varchar(30) not null,
                    notes varchar(1000),
                    registration_origin varchar(50),
                    constraint pk_item_instance primary key (id),
                    constraint fk_item_instance_main_item foreign key (main_item_id) references public.main_item (id),
                    constraint fk_item_instance_current_location foreign key (current_location_id) references public.storage_location (id)
                )
                """);
        statement.execute("create index ix_item_instance_main_item on public.item_instance (main_item_id)");
        statement.execute("create index ix_item_instance_current_location on public.item_instance (current_location_id)");
        statement.execute("create index ix_item_instance_identifier on public.item_instance (identifier)");
        statement.execute("create index ix_item_instance_asset_tag on public.item_instance (asset_tag)");
        statement.execute("create index ix_item_instance_serial_number on public.item_instance (serial_number)");
        statement.execute("create index ix_item_instance_operational_status on public.item_instance (operational_status)");
        statement.execute("create index ix_item_instance_external_id on public.item_instance (external_id)");
        statement.execute("create index ix_item_instance_active on public.item_instance (active)");

        statement.execute("""
                create table public.item_instance_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    main_item_id uuid,
                    current_location_id uuid,
                    identifier varchar(100),
                    asset_tag varchar(100),
                    serial_number varchar(150),
                    operational_status varchar(30),
                    notes varchar(1000),
                    registration_origin varchar(50),
                    constraint pk_item_instance_aud primary key (rev, id),
                    constraint fk_item_instance_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_item_instance_aud_id on public.item_instance_aud (id)");
        statement.execute("create index ix_item_instance_aud_rev on public.item_instance_aud (rev)");
    }

    private void createItemMovementSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.item_movement (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    type varchar(30) not null,
                    movement_date timestamp with time zone not null,
                    item_instance_id uuid not null,
                    origin_location_id uuid,
                    destination_location_id uuid,
                    reason varchar(200),
                    notes varchar(1000),
                    constraint pk_item_movement primary key (id),
                    constraint fk_item_movement_item_instance foreign key (item_instance_id) references public.item_instance (id),
                    constraint fk_item_movement_origin_location foreign key (origin_location_id) references public.storage_location (id),
                    constraint fk_item_movement_destination_location foreign key (destination_location_id) references public.storage_location (id)
                )
                """);
        statement.execute("create index ix_item_movement_item_instance on public.item_movement (item_instance_id)");
        statement.execute("create index ix_item_movement_origin_location on public.item_movement (origin_location_id)");
        statement.execute("create index ix_item_movement_destination_location on public.item_movement (destination_location_id)");
        statement.execute("create index ix_item_movement_type on public.item_movement (type)");
        statement.execute("create index ix_item_movement_date on public.item_movement (movement_date)");
        statement.execute("create index ix_item_movement_external_id on public.item_movement (external_id)");
        statement.execute("create index ix_item_movement_active on public.item_movement (active)");

        statement.execute("""
                create table public.item_movement_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    type varchar(30),
                    movement_date timestamp with time zone,
                    item_instance_id uuid,
                    origin_location_id uuid,
                    destination_location_id uuid,
                    reason varchar(200),
                    notes varchar(1000),
                    constraint pk_item_movement_aud primary key (rev, id),
                    constraint fk_item_movement_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_item_movement_aud_id on public.item_movement_aud (id)");
        statement.execute("create index ix_item_movement_aud_rev on public.item_movement_aud (rev)");
    }

    private void createItemLoanSchema(Statement statement) throws Exception {
        statement.execute("""
                create table public.item_loan (
                    id uuid not null,
                    active boolean not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    version bigint not null,
                    extra varchar(200),
                    external_id varchar(100),
                    item_instance_id uuid not null,
                    person_id uuid not null,
                    loan_date timestamp with time zone not null,
                    expected_return_date date,
                    return_date timestamp with time zone,
                    notes varchar(1000),
                    constraint pk_item_loan primary key (id),
                    constraint fk_item_loan_item_instance foreign key (item_instance_id) references public.item_instance (id),
                    constraint fk_item_loan_person foreign key (person_id) references public.person (id)
                )
                """);
        statement.execute("create index ix_item_loan_item_instance on public.item_loan (item_instance_id)");
        statement.execute("create index ix_item_loan_person on public.item_loan (person_id)");
        statement.execute("create index ix_item_loan_loan_date on public.item_loan (loan_date)");
        statement.execute("create index ix_item_loan_return_date on public.item_loan (return_date)");
        statement.execute("create index ix_item_loan_external_id on public.item_loan (external_id)");
        statement.execute("create index ix_item_loan_active on public.item_loan (active)");

        statement.execute("""
                create table public.item_loan_aud (
                    rev bigint not null,
                    revtype smallint,
                    id uuid not null,
                    active boolean,
                    created_at timestamp with time zone,
                    updated_at timestamp with time zone,
                    version bigint,
                    extra varchar(200),
                    external_id varchar(100),
                    item_instance_id uuid,
                    person_id uuid,
                    loan_date timestamp with time zone,
                    expected_return_date date,
                    return_date timestamp with time zone,
                    notes varchar(1000),
                    constraint pk_item_loan_aud primary key (rev, id),
                    constraint fk_item_loan_aud_rev foreign key (rev) references public.revision (id)
                )
                """);
        statement.execute("create index ix_item_loan_aud_id on public.item_loan_aud (id)");
        statement.execute("create index ix_item_loan_aud_rev on public.item_loan_aud (rev)");
    }

    private void createVectorSearchSchema(Statement statement, boolean postgresql) throws Exception {
        String embeddingType = postgresql ? "vector(384)" : "text";
        String currentTimestamp = postgresql ? "now()" : "current_timestamp";

        statement.execute("""
                create table public.main_item_embedding (
                    main_item_id uuid not null,
                    provider varchar(50) not null,
                    model varchar(200) not null,
                    dimensions integer not null,
                    indexed_text text not null,
                    embedding %s not null,
                    active boolean not null default true,
                    created_at timestamp with time zone not null default %s,
                    updated_at timestamp with time zone not null default %s,
                    constraint pk_main_item_embedding primary key (main_item_id),
                    constraint fk_main_item_embedding_main_item foreign key (main_item_id) references public.main_item (id) on delete cascade
                )
                """.formatted(embeddingType, currentTimestamp, currentTimestamp));
        statement.execute("create index ix_main_item_embedding_active on public.main_item_embedding (active)");
        if (postgresql) {
            statement.execute("create index ix_main_item_embedding_vector on public.main_item_embedding using hnsw (embedding vector_cosine_ops)");
        }

        statement.execute("""
                create table public.vector_search_metric (
                    id uuid not null,
                    query varchar(1000) not null,
                    result_count integer not null,
                    created_at timestamp with time zone not null default %s,
                    constraint pk_vector_search_metric primary key (id)
                )
                """.formatted(currentTimestamp));
    }
}
