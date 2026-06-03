create table public.local_armazenamento (
                                             id uuid not null,
                                             ativo boolean not null,
                                             criado_em timestamp with time zone not null,
                                             alterado_em timestamp with time zone not null,
                                             version bigint not null,
                                             extra varchar(200),
                                             oi varchar(100),
                                             nome varchar(150) not null,
                                             descricao varchar(500),
                                             local_pai_id uuid,
                                             constraint pk_local_armazenamento primary key (id),
                                             constraint fk_local_armazenamento_pai foreign key (local_pai_id) references public.local_armazenamento (id)
);

create index ix_local_armazenamento_nome on public.local_armazenamento (nome);
create index ix_local_armazenamento_oi on public.local_armazenamento (oi);
create index ix_local_armazenamento_ativo on public.local_armazenamento (ativo);
create index ix_local_armazenamento_pai on public.local_armazenamento (local_pai_id);

create table public.local_armazenamento_aud (
                                                 rev bigint not null,
                                                 revtype smallint,
                                                 id uuid not null,
                                                 ativo boolean,
                                                 criado_em timestamp with time zone,
                                                 alterado_em timestamp with time zone,
                                                 version bigint,
                                                 extra varchar(200),
                                                 oi varchar(100),
                                                 nome varchar(150),
                                                 descricao varchar(500),
                                                 local_pai_id uuid,
                                                 constraint pk_local_armazenamento_aud primary key (rev, id),
                                                 constraint fk_local_armazenamento_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_local_armazenamento_aud_id on public.local_armazenamento_aud (id);
create index ix_local_armazenamento_aud_rev on public.local_armazenamento_aud (rev);
