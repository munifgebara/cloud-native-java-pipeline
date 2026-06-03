package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.mapper.LocalArmazenamentoMapper;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocalArmazenamentoService extends SuperService<LocalArmazenamento, LocalArmazenamentoRepository> {

    public LocalArmazenamentoService(LocalArmazenamentoRepository repository, EntityManager entityManager) {
        super(repository, entityManager, LocalArmazenamento.class);
    }

    @Transactional
    public LocalArmazenamentoResponseDTO criar(LocalArmazenamentoCreateDTO dto) {
        LocalArmazenamento local = LocalArmazenamentoMapper.toEntity(dto);
        normalizarCampos(local);
        local.setPai(buscarPaiAtivo(dto.paiId()));

        LocalArmazenamento salvo = salvar(local);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setAtivo(false);
            salvo = salvar(salvo);
        }
        return LocalArmazenamentoMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public LocalArmazenamentoResponseDTO buscarResponsePorId(UUID id) {
        return LocalArmazenamentoMapper.toResponseDTO(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> listarResumo() {
        return montarHierarquia(repository.findByAtivoTrueOrderByNomeAsc());
    }

    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> listarResumoIncluindoInativos() {
        return montarHierarquia(listarTodosIncluindoInativos());
    }

    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return montarHierarquia(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado));
    }

    @Transactional
    public LocalArmazenamentoResponseDTO atualizar(UUID id, LocalArmazenamentoUpdateDTO dto) {
        LocalArmazenamento local = buscarPorId(id);
        LocalArmazenamento pai = buscarPaiAtivo(dto.paiId());
        validarHierarquia(local, pai);

        LocalArmazenamentoMapper.updateEntity(local, dto);
        normalizarCampos(local);
        local.setPai(pai);

        LocalArmazenamento salvo = salvar(local);
        return LocalArmazenamentoMapper.toResponseDTO(salvo);
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    @Transactional(readOnly = true)
    public List<RevisaoDTO<LocalArmazenamento>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private List<LocalArmazenamentoResumoDTO> montarHierarquia(List<LocalArmazenamento> locais) {
        Map<UUID, List<LocalArmazenamento>> filhosPorPai = locais.stream()
                .filter(local -> local.getPai() != null)
                .collect(Collectors.groupingBy(local -> local.getPai().getId()));

        List<LocalArmazenamento> raizes = locais.stream()
                .filter(local -> local.getPai() == null || locais.stream().noneMatch(item -> item.getId().equals(local.getPai().getId())))
                .sorted(Comparator.comparing(LocalArmazenamento::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LocalArmazenamentoResumoDTO> resultado = new ArrayList<>();
        Set<UUID> visitados = new HashSet<>();
        for (LocalArmazenamento raiz : raizes) {
            adicionarNaHierarquia(raiz, raiz.getNome(), 0, filhosPorPai, resultado, visitados);
        }
        return resultado;
    }

    private void adicionarNaHierarquia(
            LocalArmazenamento local,
            String caminho,
            int nivel,
            Map<UUID, List<LocalArmazenamento>> filhosPorPai,
            List<LocalArmazenamentoResumoDTO> resultado,
            Set<UUID> visitados
    ) {
        if (!visitados.add(local.getId())) {
            return;
        }

        resultado.add(LocalArmazenamentoMapper.toResumoDTO(local, caminho, nivel));
        filhosPorPai.getOrDefault(local.getId(), List.of()).stream()
                .sorted(Comparator.comparing(LocalArmazenamento::getNome, String.CASE_INSENSITIVE_ORDER))
                .forEach(filho -> adicionarNaHierarquia(filho, caminho + " > " + filho.getNome(), nivel + 1, filhosPorPai, resultado, visitados));
    }

    private LocalArmazenamento buscarPaiAtivo(UUID paiId) {
        if (paiId == null) {
            return null;
        }

        LocalArmazenamento pai = buscarPorId(paiId);
        if (!pai.isAtivo()) {
            throw new IllegalArgumentException("Local pai deve estar ativo.");
        }
        return pai;
    }

    private void validarHierarquia(LocalArmazenamento local, LocalArmazenamento pai) {
        if (pai == null) {
            return;
        }

        if (local.getId().equals(pai.getId())) {
            throw new IllegalArgumentException("Local não pode ser pai dele mesmo.");
        }

        LocalArmazenamento atual = pai.getPai();
        while (atual != null) {
            if (local.getId().equals(atual.getId())) {
                throw new IllegalArgumentException("Local pai não pode ser descendente do próprio local.");
            }
            atual = atual.getPai();
        }
    }

    private void normalizarCampos(LocalArmazenamento local) {
        local.setNome(ValidacoesBR.trimToNull(local.getNome()));
        local.setDescricao(ValidacoesBR.trimToNull(local.getDescricao()));
    }
}
