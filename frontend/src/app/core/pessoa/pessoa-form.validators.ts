export function somenteDigitos(valor: string | null | undefined): string {
  return (valor ?? '').replace(/\D/g, '');
}

export function validarCpf(cpf: string | null | undefined): boolean {
  const valor = somenteDigitos(cpf);

  if (valor.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(valor)) return false;

  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += Number(valor[i]) * (10 - i);
  }

  let resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  if (resto !== Number(valor[9])) return false;

  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += Number(valor[i]) * (11 - i);
  }

  resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  return resto === Number(valor[10]);
}

export function validarCnpj(cnpj: string | null | undefined): boolean {
  const valor = somenteDigitos(cnpj);

  if (valor.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(valor)) return false;

  const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesos2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  let soma = 0;
  for (let i = 0; i < 12; i++) {
    soma += Number(valor[i]) * pesos1[i];
  }

  let resto = soma % 11;
  const dig1 = resto < 2 ? 0 : 11 - resto;
  if (dig1 !== Number(valor[12])) return false;

  soma = 0;
  for (let i = 0; i < 13; i++) {
    soma += Number(valor[i]) * pesos2[i];
  }

  resto = soma % 11;
  const dig2 = resto < 2 ? 0 : 11 - resto;
  return dig2 === Number(valor[13]);
}

export function validarCpfCnpj(valor: string | null | undefined): boolean {
  const digits = somenteDigitos(valor);

  if (digits.length === 11) return validarCpf(digits);
  if (digits.length === 14) return validarCnpj(digits);
  return false;
}

export function validarTelefone(valor: string | null | undefined): boolean {
  const digits = somenteDigitos(valor);
  return digits.length === 10 || digits.length === 11;
}

export function validarCep(valor: string | null | undefined): boolean {
  const digits = somenteDigitos(valor);
  return digits.length === 8;
}
