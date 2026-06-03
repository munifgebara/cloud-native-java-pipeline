package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.CategoriaIcone;
import br.com.munif.stella.api.mapper.CategoriaMapper;
import br.com.munif.stella.api.repository.CategoriaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService extends SuperService<Categoria, CategoriaRepository> {

    public CategoriaService(CategoriaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Categoria.class);
    }

    @Transactional
    public CategoriaResponseDTO criar(CategoriaCreateDTO dto) {
        Categoria categoria = CategoriaMapper.toEntity(dto);
        normalizarCampos(categoria);

        Categoria salva = salvar(categoria);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setAtivo(false);
            salva = salvar(salva);
        }
        return CategoriaMapper.toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarResponsePorId(UUID id) {
        return CategoriaMapper.toResponseDTO(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado).stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    @Transactional
    public CategoriaResponseDTO atualizar(UUID id, CategoriaUpdateDTO dto) {
        Categoria categoria = buscarPorId(id);
        CategoriaMapper.updateEntity(categoria, dto);
        normalizarCampos(categoria);

        Categoria salva = salvar(categoria);
        return CategoriaMapper.toResponseDTO(salva);
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    @Transactional(readOnly = true)
    public List<RevisaoDTO<Categoria>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private void normalizarCampos(Categoria categoria) {
        categoria.setNome(ValidacoesBR.trimToNull(categoria.getNome()));
        categoria.setDescricao(ValidacoesBR.trimToNull(categoria.getDescricao()));
        categoria.setIcone(normalizarIcone(categoria.getIcone()));
    }

    private String normalizarIcone(String icone) {
        String valor = ValidacoesBR.trimToNull(icone);
        if (!CategoriaIcone.isChaveValida(valor)) {
            throw new IllegalArgumentException("Ícone de categoria inválido.");
        }
        return valor;
    }
}
