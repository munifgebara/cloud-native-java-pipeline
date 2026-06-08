alter table public.local_armazenamento
    add column imagem_bucket varchar(100);

alter table public.local_armazenamento
    add column imagem_object_key varchar(500);

alter table public.local_armazenamento
    add column imagem_content_type varchar(100);

alter table public.local_armazenamento
    add column imagem_tamanho_bytes bigint;

create index ix_local_armazenamento_imagem_object_key on public.local_armazenamento (imagem_object_key);

alter table public.local_armazenamento_aud
    add column imagem_bucket varchar(100);

alter table public.local_armazenamento_aud
    add column imagem_object_key varchar(500);

alter table public.local_armazenamento_aud
    add column imagem_content_type varchar(100);

alter table public.local_armazenamento_aud
    add column imagem_tamanho_bytes bigint;
