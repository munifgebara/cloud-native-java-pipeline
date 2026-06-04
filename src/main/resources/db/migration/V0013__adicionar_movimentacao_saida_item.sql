alter table public.movimentacao_item
    add column local_origem_id uuid;

alter table public.movimentacao_item
    add column motivo varchar(200);

alter table public.movimentacao_item
    alter column local_destino_id drop not null;

alter table public.movimentacao_item
    add constraint fk_movimentacao_item_local_origem
        foreign key (local_origem_id) references public.local_armazenamento (id);

create index ix_movimentacao_item_local_origem on public.movimentacao_item (local_origem_id);

alter table public.movimentacao_item_aud
    add column local_origem_id uuid;

alter table public.movimentacao_item_aud
    add column motivo varchar(200);
