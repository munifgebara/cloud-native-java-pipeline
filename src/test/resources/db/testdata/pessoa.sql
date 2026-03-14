delete from public.pessoa;

insert into public.pessoa (
    id,
    ativo,
    criado_em,
    alterado_em,
    version,
    extra,
    oi,
    nome,
    cpf_cnpj
) values
      (
          '11111111-1111-1111-1111-111111111111',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'João da Silva',
          '12345678909'
      ),
      (
          '22222222-2222-2222-2222-222222222222',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Maria Oliveira',
          '11144477735'
      ),
      (
          '33333333-3333-3333-3333-333333333333',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Carlos Pereira',
          '98765432100'
      ),
      (
          '44444444-4444-4444-4444-444444444444',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Ana Souza',
          '39053344705'
      ),
      (
          '55555555-5555-5555-5555-555555555555',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Fernanda Costa',
          '52998224725'
      ),
      (
          '66666666-6666-6666-6666-666666666666',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Empresa Alpha Ltda',
          '11222333000181'
      ),
      (
          '77777777-7777-7777-7777-777777777777',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Beta Soluções Empresariais SA',
          '22333444000181'
      ),
      (
          '88888888-8888-8888-8888-888888888888',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Comercial Gama ME',
          '33444555000181'
      ),
      (
          '99999999-9999-9999-9999-999999999999',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Delta Serviços Digitais Ltda',
          '44555666000181'
      ),
      (
          'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
          true,
          now(),
          now(),
          0,
          'test',
          'public',
          'Omega Participações Ltda',
          '55666777000181'
      );
