package br.com.munif.stella.api.service;

import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

final class InstanciaItemRegras {

    private InstanciaItemRegras() {
    }

    static void validarCoerenciaStatusLocal(InstanciaItem instancia) {
        StatusOperacionalInstancia status = instancia.getStatusOperacional();
        if (status == StatusOperacionalInstancia.DISPONIVEL && instancia.getLocalAtual() == null) {
            throw new IllegalArgumentException("Instância disponível deve possuir local atual.");
        }
        if (status == StatusOperacionalInstancia.EMPRESTADO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instância emprestada não deve possuir local atual.");
        }
        if (status == StatusOperacionalInstancia.EM_MOVIMENTACAO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instância em movimentação não deve possuir local atual.");
        }
        if (status == StatusOperacionalInstancia.INATIVO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instância inativa não deve possuir local atual.");
        }
    }

    static void exigirDisponivelComLocal(
            InstanciaItem instancia,
            String mensagemInstanciaInativa,
            String mensagemStatusInvalido,
            String mensagemLocalAusente
    ) {
        if (!instancia.isAtivo()) {
            throw new IllegalArgumentException(mensagemInstanciaInativa);
        }
        if (instancia.getStatusOperacional() != StatusOperacionalInstancia.DISPONIVEL) {
            throw new IllegalArgumentException(mensagemStatusInvalido);
        }
        if (instancia.getLocalAtual() == null) {
            throw new IllegalArgumentException(mensagemLocalAusente);
        }
    }

    static void exigirEmprestada(InstanciaItem instancia, String mensagemStatusInvalido) {
        if (instancia.getStatusOperacional() != StatusOperacionalInstancia.EMPRESTADO) {
            throw new IllegalArgumentException(mensagemStatusInvalido);
        }
    }
}
