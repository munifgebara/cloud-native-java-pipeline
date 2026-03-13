create table public.versao (
                               id bigserial not null,
                               timestamp timestamp with time zone not null,
                               ip varchar(45),
                               usuario varchar(100),
                               oi varchar(100),
                               constraint pk_versao primary key (id)
);

