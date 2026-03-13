create table public.pessoa (
                               id uuid not null,
                               ativo boolean not null,
                               criado_em timestamp with time zone not null,
                               alterado_em timestamp with time zone not null,
                               version bigint not null,
                               extra varchar(200),
                               oi varchar(100),
                               nome varchar(150) not null,
                               cpf_cnpj varchar(14) not null,
                               constraint pk_pessoa primary key (id),
                               constraint uk_pessoa_cpf_cnpj unique (cpf_cnpj)
);

create index ix_pessoa_nome on public.pessoa (nome);
create index ix_pessoa_oi on public.pessoa (oi);
create index ix_pessoa_ativo on public.pessoa (ativo);

create table public.pessoa_aud (
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
                                   cpf_cnpj varchar(14),
                                   constraint pk_pessoa_aud primary key (rev, id),
                                   constraint fk_pessoa_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_pessoa_aud_id on public.pessoa_aud (id);
create index ix_pessoa_aud_rev on public.pessoa_aud (rev);
