create table public.emprestimo_item (
                                       id uuid not null,
                                       ativo boolean not null,
                                       criado_em timestamp with time zone not null,
                                       alterado_em timestamp with time zone not null,
                                       version bigint not null,
                                       extra varchar(200),
                                       oi varchar(100),
                                       instancia_item_id uuid not null,
                                       pessoa_id uuid not null,
                                       data_emprestimo timestamp with time zone not null,
                                       previsao_devolucao date,
                                       data_devolucao timestamp with time zone,
                                       observacao varchar(1000),
                                       constraint pk_emprestimo_item primary key (id),
                                       constraint fk_emprestimo_item_instancia foreign key (instancia_item_id) references public.instancia_item (id),
                                       constraint fk_emprestimo_item_pessoa foreign key (pessoa_id) references public.pessoa (id)
);

create index ix_emprestimo_item_instancia on public.emprestimo_item (instancia_item_id);
create index ix_emprestimo_item_pessoa on public.emprestimo_item (pessoa_id);
create index ix_emprestimo_item_data_emprestimo on public.emprestimo_item (data_emprestimo);
create index ix_emprestimo_item_data_devolucao on public.emprestimo_item (data_devolucao);
create index ix_emprestimo_item_oi on public.emprestimo_item (oi);
create index ix_emprestimo_item_ativo on public.emprestimo_item (ativo);

create table public.emprestimo_item_aud (
                                           rev bigint not null,
                                           revtype smallint,
                                           id uuid not null,
                                           ativo boolean,
                                           criado_em timestamp with time zone,
                                           alterado_em timestamp with time zone,
                                           version bigint,
                                           extra varchar(200),
                                           oi varchar(100),
                                           instancia_item_id uuid,
                                           pessoa_id uuid,
                                           data_emprestimo timestamp with time zone,
                                           previsao_devolucao date,
                                           data_devolucao timestamp with time zone,
                                           observacao varchar(1000),
                                           constraint pk_emprestimo_item_aud primary key (rev, id),
                                           constraint fk_emprestimo_item_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_emprestimo_item_aud_id on public.emprestimo_item_aud (id);
create index ix_emprestimo_item_aud_rev on public.emprestimo_item_aud (rev);
