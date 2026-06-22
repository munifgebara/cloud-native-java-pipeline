delete from public.person;

insert into public.person (
    id,
    active,
    created_at,
    updated_at,
    version,
    extra,
    external_id,
    name,
    tax_id,
    primary_phone,
    secondary_phone,
    email,
    zip_code,
    address,
    complement,
    neighborhood,
    city,
    state
) values
-- Individuals with valid CPF values
(
    '11111111-1111-1111-1111-111111111111',
    true, now(), now(), 0, 'test', 'public',
    'João da Silva', '52998224725',
    '44999990001', null,
    'joao@teste.com',
    '87000000',
    'Rua A', 'Casa', 'Centro', 'Maringá', 'PR'
),
(
    '22222222-2222-2222-2222-222222222222',
    true, now(), now(), 0, 'test', 'public',
    'Maria Oliveira', '12345678909',
    '44999990002', null,
    'maria@teste.com',
    '87000001',
    'Rua B', 'Apto 101', 'Zona 1', 'Maringá', 'PR'
),
(
    '33333333-3333-3333-3333-333333333333',
    true, now(), now(), 0, 'test', 'public',
    'Carlos Pereira', '11144477735',
    '44999990003', null,
    'carlos@teste.com',
    '87000002',
    'Rua C', null, 'Zona 2', 'Maringá', 'PR'
),
(
    '44444444-4444-4444-4444-444444444444',
    true, now(), now(), 0, 'test', 'public',
    'Ana Souza', '39053344705',
    '44999990004', null,
    'ana@teste.com',
    '87000003',
    'Rua D', null, 'Centro', 'Maringá', 'PR'
),
(
    '55555555-5555-5555-5555-555555555555',
    true, now(), now(), 0, 'test', 'public',
    'Fernanda Costa', '16899535009',
    '44999990005', null,
    'fernanda@teste.com',
    '87000004',
    'Rua E', null, 'Zona 3', 'Maringá', 'PR'
),

-- Legal entities with valid CNPJ values
(
    '66666666-6666-6666-6666-666666666666',
    true, now(), now(), 0, 'test', 'public',
    'Empresa Alpha Ltda', '11222333000181',
    '4433330001', null,
    'contato@alpha.com',
    '87010000',
    'Av. Brasil', 'Sala 1', 'Centro', 'Maringá', 'PR'
),
(
    '77777777-7777-7777-7777-777777777777',
    true, now(), now(), 0, 'test', 'public',
    'Beta Soluções Empresariais SA', '19131243000197',
    '4433330002', null,
    'contato@beta.com',
    '87010001',
    'Av. Colombo', 'Sala 2', 'Zona 7', 'Maringá', 'PR'
),
(
    '88888888-8888-8888-8888-888888888888',
    true, now(), now(), 0, 'test', 'public',
    'Comercial Gama ME', '27865757000102',
    '4433330003', null,
    'contato@gama.com',
    '87010002',
    'Av. Tuiuti', null, 'Zona 5', 'Maringá', 'PR'
),
(
    '99999999-9999-9999-9999-999999999999',
    true, now(), now(), 0, 'test', 'public',
    'Delta Serviços Digitais Ltda', '11444777000161',
    '4433330004', null,
    'contato@delta.com',
    '87010003',
    'Av. Mandacaru', null, 'Zona 6', 'Maringá', 'PR'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    true, now(), now(), 0, 'test', 'public',
    'Omega Participações Ltda', '74598723000180',
    '4433330005', null,
    'contato@omega.com',
    '87010004',
    'Av. Kakogawa', null, 'Zona 8', 'Maringá', 'PR'
);
