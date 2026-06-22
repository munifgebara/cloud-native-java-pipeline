alter table public.pessoa
    add column foto_bucket varchar(100);

alter table public.pessoa
    add column foto_object_key varchar(500);

alter table public.pessoa
    add column foto_content_type varchar(100);

alter table public.pessoa
    add column foto_tamanho_bytes bigint;

create index ix_pessoa_foto_object_key on public.pessoa (foto_object_key);

alter table public.pessoa_aud
    add column foto_bucket varchar(100);

alter table public.pessoa_aud
    add column foto_object_key varchar(500);

alter table public.pessoa_aud
    add column foto_content_type varchar(100);

alter table public.pessoa_aud
    add column foto_tamanho_bytes bigint;
