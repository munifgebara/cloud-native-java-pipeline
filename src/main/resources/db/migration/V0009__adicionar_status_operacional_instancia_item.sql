alter table public.instancia_item
    add column status_operacional varchar(30);

update public.instancia_item
set status_operacional = 'DISPONIVEL'
where status_operacional is null;

alter table public.instancia_item
    alter column status_operacional set not null;

create index ix_instancia_item_status_operacional on public.instancia_item (status_operacional);

alter table public.instancia_item_aud
    add column status_operacional varchar(30);
