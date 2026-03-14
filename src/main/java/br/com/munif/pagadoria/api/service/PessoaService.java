package br.com.munif.pagadoria.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.pagadoria.api.dto.PessoaCreateDTO;
import br.com.munif.pagadoria.api.dto.PessoaResponseDTO;
import br.com.munif.pagadoria.api.dto.PessoaResumoDTO;
import br.com.munif.pagadoria.api.dto.PessoaUpdateDTO;
import br.com.munif.pagadoria.api.entity.Pessoa;
import br.com.munif.pagadoria.api.mapper.PessoaMapper;
import br.com.munif.pagadoria.api.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PessoaService extends SuperService<Pessoa, PessoaRepository> {

    public PessoaService(PessoaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Pessoa.class);
    }

    public PessoaResponseDTO criar(PessoaCreateDTO dto) {
        Pessoa pessoa = PessoaMapper.toEntity(dto);
        Pessoa salva = salvar(pessoa);
        return PessoaMapper.toResponseDTO(salva);
    }

    public PessoaResponseDTO buscarResponsePorId(UUID id) {
        return PessoaMapper.toResponseDTO(buscarPorId(id));
    }

    public List<PessoaResumoDTO> listarResumo() {
        return listarTodos().stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    public List<PessoaResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    public PessoaResponseDTO atualizar(UUID id, PessoaUpdateDTO dto) {
        Pessoa pessoa = buscarPorId(id);
        PessoaMapper.updateEntity(pessoa, dto);
        Pessoa salva = salvar(pessoa);
        return PessoaMapper.toResponseDTO(salva);
    }

    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    public List<PessoaResumoDTO> buscarPorNome(String nome) {
        return repository.findByAtivoTrueAndNomeContainingIgnoreCase(nome).stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    public List<RevisaoDTO<Pessoa>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }
}
