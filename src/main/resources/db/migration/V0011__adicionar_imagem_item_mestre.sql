alter table public.item_mestre
    add column imagem_bucket varchar(100);

alter table public.item_mestre
    add column imagem_object_key varchar(500);

alter table public.item_mestre
    add column imagem_content_type varchar(100);

alter table public.item_mestre
    add column imagem_tamanho_bytes bigint;

create index ix_item_mestre_imagem_object_key on public.item_mestre (imagem_object_key);

alter table public.item_mestre_aud
    add column imagem_bucket varchar(100);

alter table public.item_mestre_aud
    add column imagem_object_key varchar(500);

alter table public.item_mestre_aud
    add column imagem_content_type varchar(100);

alter table public.item_mestre_aud
    add column imagem_tamanho_bytes bigint;
