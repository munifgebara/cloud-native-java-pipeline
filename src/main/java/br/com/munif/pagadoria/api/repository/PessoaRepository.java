package br.com.munif.pagadoria.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.pagadoria.api.entity.Pessoa;

import java.util.List;
import java.util.Optional;

public interface PessoaRepository extends SuperRepository<Pessoa> {

    Optional<Pessoa> findByCpfCnpj(String cpfCnpj);

    boolean existsByCpfCnpj(String cpfCnpj);

    List<Pessoa> findByAtivoTrueAndNomeContainingIgnoreCase(String nome);

    List<Pessoa> findByAtivoTrueOrderByNomeAsc();

    long countByAtivoTrue();
}