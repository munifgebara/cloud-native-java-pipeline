delete from public.instancia_item_aud;
delete from public.instancia_item;

alter table public.instancia_item
    add column local_armazenamento_id uuid;

alter table public.instancia_item
    add constraint fk_instancia_item_local_armazenamento
        foreign key (local_armazenamento_id) references public.local_armazenamento (id);

create index ix_instancia_item_local_armazenamento on public.instancia_item (local_armazenamento_id);

alter table public.instancia_item_aud
    add column local_armazenamento_id uuid;

create table public.movimentacao_item (
                                         id uuid not null,
                                         ativo boolean not null,
                                         criado_em timestamp with time zone not null,
                                         alterado_em timestamp with time zone not null,
                                         version bigint not null,
                                         extra varchar(200),
                                         oi varchar(100),
                                         tipo varchar(30) not null,
                                         data_movimentacao timestamp with time zone not null,
                                         instancia_item_id uuid not null,
                                         local_destino_id uuid not null,
                                         observacao varchar(1000),
                                         constraint pk_movimentacao_item primary key (id),
                                         constraint fk_movimentacao_item_instancia foreign key (instancia_item_id) references public.instancia_item (id),
                                         constraint fk_movimentacao_item_local_destino foreign key (local_destino_id) references public.local_armazenamento (id)
);

create index ix_movimentacao_item_instancia on public.movimentacao_item (instancia_item_id);
create index ix_movimentacao_item_local_destino on public.movimentacao_item (local_destino_id);
create index ix_movimentacao_item_tipo on public.movimentacao_item (tipo);
create index ix_movimentacao_item_data on public.movimentacao_item (data_movimentacao);
create index ix_movimentacao_item_oi on public.movimentacao_item (oi);
create index ix_movimentacao_item_ativo on public.movimentacao_item (ativo);

create table public.movimentacao_item_aud (
                                             rev bigint not null,
                                             revtype smallint,
                                             id uuid not null,
                                             ativo boolean,
                                             criado_em timestamp with time zone,
                                             alterado_em timestamp with time zone,
                                             version bigint,
                                             extra varchar(200),
                                             oi varchar(100),
                                             tipo varchar(30),
                                             data_movimentacao timestamp with time zone,
                                             instancia_item_id uuid,
                                             local_destino_id uuid,
                                             observacao varchar(1000),
                                             constraint pk_movimentacao_item_aud primary key (rev, id),
                                             constraint fk_movimentacao_item_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_movimentacao_item_aud_id on public.movimentacao_item_aud (id);
create index ix_movimentacao_item_aud_rev on public.movimentacao_item_aud (rev);
