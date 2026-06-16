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

/**
 * Service responsible for business operations on {@link Categoria}.
 *
 * <p>Orchestrates persistence, field normalization, and queries, delegating to
 * {@link CategoriaRepository} and {@link CategoriaMapper} as needed.</p>
 */
@Service
public class CategoriaService extends SuperService<Categoria, CategoriaRepository> {

    /**
     * Constructs the service injecting the repository and the {@code EntityManager}.
     *
     * @param repository    JPA repository for categories
     * @param entityManager entity manager, used internally by {@code SuperService}
     *                      for Envers queries
     */
    public CategoriaService(CategoriaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Categoria.class);
    }

    /**
     * Creates a new category from the input DTO.
     *
     * <p>If the DTO {@code ativa} field is {@code false}, the category is created active
     * ({@code @PrePersist} constraint) and then deactivated in a second operation.</p>
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created category
     * @throws IllegalArgumentException if the provided icon is invalid
     */
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

    /**
     * Returns the full DTO of a category by its identifier.
     *
     * @param id UUID of the category
     * @return full DTO of the category
     * @throws jakarta.persistence.EntityNotFoundException if the category does not exist
     */
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarResponsePorId(UUID id) {
        return CategoriaMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lists all active categories in alphabetical order by name.
     *
     * @return list of summary DTOs of active categories
     */
    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all categories, including inactive ones.
     *
     * @return list of summary DTOs of all categories
     */
    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Finds active categories whose name contains the given text (partial, case-insensitive search).
     *
     * @param nome text to search in the category name; returns empty list if blank
     * @return list of summary DTOs of the found categories
     */
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

    /**
     * Updates the data of an existing category.
     *
     * @param id  UUID of the category to update
     * @param dto update data validated by Bean Validation
     * @return full DTO of the updated category
     * @throws jakarta.persistence.EntityNotFoundException if the category does not exist
     * @throws IllegalArgumentException if the provided icon is invalid
     */
    @Transactional
    public CategoriaResponseDTO atualizar(UUID id, CategoriaUpdateDTO dto) {
        Categoria categoria = buscarPorId(id);
        CategoriaMapper.updateEntity(categoria, dto);
        normalizarCampos(categoria);

        Categoria salva = salvar(categoria);
        return CategoriaMapper.toResponseDTO(salva);
    }

    /**
     * Logically deactivates a category (sets {@code ativo = false}).
     *
     * @param id UUID of the category to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the category does not exist
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    /**
     * Returns the previous revision history of a category (Hibernate Envers).
     *
     * @param id UUID of the category
     * @return list of revisions in chronological order; empty list if there is no history
     */
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
            throw new IllegalArgumentException("Invalid category icon.");
        }
        return valor;
    }
}
