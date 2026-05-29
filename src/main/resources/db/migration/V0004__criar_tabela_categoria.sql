create table public.categoria (
                                  id uuid not null,
                                  ativo boolean not null,
                                  criado_em timestamp with time zone not null,
                                  alterado_em timestamp with time zone not null,
                                  version bigint not null,
                                  extra varchar(200),
                                  oi varchar(100),
                                  nome varchar(150) not null,
                                  descricao varchar(500),
                                  constraint pk_categoria primary key (id)
);

create index ix_categoria_nome on public.categoria (nome);
create index ix_categoria_oi on public.categoria (oi);
create index ix_categoria_ativo on public.categoria (ativo);

create table public.categoria_aud (
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
                                      constraint pk_categoria_aud primary key (rev, id),
                                      constraint fk_categoria_aud_rev foreign key (rev) references public.versao (id)
);

create index ix_categoria_aud_id on public.categoria_aud (id);
create index ix_categoria_aud_rev on public.categoria_aud (rev);
