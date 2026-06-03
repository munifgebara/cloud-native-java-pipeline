create table public.instancia_item (
                                      id uuid not null,
                                      ativo boolean not null,
                                      criado_em timestamp with time zone not null,
                                      alterado_em timestamp with time zone not null,
                                      version bigint not null,
                                      extra varchar(200),
                                      oi varchar(100),
                                      item_mestre_id uuid not null,
                                      identificador varchar(100),
                                      patrimonio varchar(100),
                                      numero_serie varchar(150),
                                      observacoes varchar(1000),
                                      constraint pk_instancia_item primary key (id),
                                      constraint fk_instancia_item_item_mestre foreign key (item_mestre_id) references public.item_mestre (id)
);

create index ix_instancia_item_item_mestre on public.instancia_item (item_mestre_id);
create index ix_instancia_item_identificador on public.instancia_item (identificador);
create index ix_instancia_item_patrimonio on public.instancia_item (patrimonio);
create index ix_instancia_item_numero_serie on public.instancia_item (numero_serie);
create index ix_instancia_item_oi on public.instancia_item (oi);
create index ix_instancia_item_ativo on public.instancia_item (ativo);

create table public.instancia_item_aud (
                                        rev bigint not null,
                                        revtype smallint,
                                        id uuid not null,
                                        ativo boolean,
                                        criado_em timestamp with time zone,
                                        alterado_em timestamp with time zone,
                                        version bigint,
                                        extra varchar(200),
                                        oi varchar(100),
                                        item_mestre_id uuid,
                                        identificador varchar(100),
                                        patrimonio varchar(100),
                                        numero_serie varchar(150),
                                        observacoes varchar(1000),
                                        constraint pk_instancia_item_aud primary key (rev, id),
                                        constraint fk_instancia_item_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_instancia_item_aud_id on public.instancia_item_aud (id);
create index ix_instancia_item_aud_rev on public.instancia_item_aud (rev);
