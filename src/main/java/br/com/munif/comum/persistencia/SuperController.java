package br.com.munif.comum.controller;

import br.com.munif.comum.dto.RevisaoDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public abstract class SuperController<RESUMO, RESPONSE, CREATE, UPDATE, ENTITY> {

    public abstract ResponseEntity<RESPONSE> criar(CREATE dto);

    public abstract ResponseEntity<RESPONSE> buscarPorId(UUID id);

    public abstract ResponseEntity<List<RESUMO>> listar();

    public abstract ResponseEntity<RESPONSE> atualizar(UUID id, UPDATE dto);

    public abstract ResponseEntity<Void> excluir(UUID id);

    public abstract ResponseEntity<List<RESUMO>> listarTodosIncluindoInativos();

    public abstract ResponseEntity<List<RevisaoDTO<ENTITY>>> listarVersoesAnteriores(UUID id);
}
