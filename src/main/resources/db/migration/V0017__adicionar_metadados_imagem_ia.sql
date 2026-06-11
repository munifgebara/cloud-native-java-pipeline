alter table public.item_mestre
    add column imagem_generated_by_ai boolean not null default false;

alter table public.item_mestre
    add column imagem_provider varchar(50);

alter table public.item_mestre_aud
    add column imagem_generated_by_ai boolean;

alter table public.item_mestre_aud
    add column imagem_provider varchar(50);
