create table public.item_mestre (
                                    id uuid not null,
                                    ativo boolean not null,
                                    criado_em timestamp with time zone not null,
                                    alterado_em timestamp with time zone not null,
                                    version bigint not null,
                                    extra varchar(200),
                                    oi varchar(100),
                                    nome varchar(150) not null,
                                    descricao varchar(500),
                                    observacoes varchar(1000),
                                    categoria_id uuid,
                                    constraint pk_item_mestre primary key (id),
                                    constraint fk_item_mestre_categoria foreign key (categoria_id) references public.categoria (id)
);

create index ix_item_mestre_nome on public.item_mestre (nome);
create index ix_item_mestre_categoria on public.item_mestre (categoria_id);
create index ix_item_mestre_oi on public.item_mestre (oi);
create index ix_item_mestre_ativo on public.item_mestre (ativo);

create table public.item_mestre_aud (
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
                                      observacoes varchar(1000),
                                      categoria_id uuid,
                                      constraint pk_item_mestre_aud primary key (rev, id),
                                      constraint fk_item_mestre_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_item_mestre_aud_id on public.item_mestre_aud (id);
create index ix_item_mestre_aud_rev on public.item_mestre_aud (rev);
