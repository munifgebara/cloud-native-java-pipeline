alter table item_mestre
    add column origem_cadastro varchar(50);

alter table item_mestre_aud
    add column origem_cadastro varchar(50);

alter table instancia_item
    add column origem_cadastro varchar(50);

alter table instancia_item_aud
    add column origem_cadastro varchar(50);
