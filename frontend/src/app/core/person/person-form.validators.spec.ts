import { describe, expect, it } from 'vitest';

import {
  somenteDigitos,
  validarCep,
  validarCnpj,
  validarCpf,
  validarCpfCnpj,
  validarTelefone,
} from './person-form.validators';

describe('person form validators', () => {
  it('normalizes formatted values to digits', () => {
    expect(somenteDigitos('(11) 98765-4321')).toBe('11987654321');
    expect(somenteDigitos(null)).toBe('');
  });

  it('validates CPF and CNPJ check digits', () => {
    expect(validarCpf('529.982.247-25')).toBe(true);
    expect(validarCpf('111.111.111-11')).toBe(false);
    expect(validarCnpj('04.252.011/0001-10')).toBe(true);
    expect(validarCnpj('00.000.000/0000-00')).toBe(false);
  });

  it('selects CPF or CNPJ validation by normalized length', () => {
    expect(validarCpfCnpj('52998224725')).toBe(true);
    expect(validarCpfCnpj('04.252.011/0001-10')).toBe(true);
    expect(validarCpfCnpj('123')).toBe(false);
  });

  it('validates Brazilian phone and postal-code lengths', () => {
    expect(validarTelefone('(11) 98765-4321')).toBe(true);
    expect(validarTelefone('123')).toBe(false);
    expect(validarCep('01310-100')).toBe(true);
    expect(validarCep('01310')).toBe(false);
  });
});
