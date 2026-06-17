package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryResponseDTO;
import br.com.stella.api.dto.CategorySummaryDTO;
import br.com.stella.api.dto.CategoryUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.CategoryIcon;
import br.com.stella.api.mapper.CategoryMapper;
import br.com.stella.api.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for business operations on {@link Category}.
 *
 * <p>Orchestrates persistence, field normalization, and queries, delegating to
 * {@link CategoryRepository} and {@link CategoryMapper} as needed.</p>
 */
@Service
public class CategoryService extends SuperService<Category, CategoryRepository> {

    /**
     * Constructs the service injecting the repository and the {@code EntityManager}.
     *
     * @param repository    JPA repository for categories
     * @param entityManager entity manager, used internally by {@code SuperService}
     *                      for Envers queries
     */
    public CategoryService(CategoryRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Category.class);
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
    public CategoryResponseDTO create(CategoryCreateDTO dto) {
        Category category = CategoryMapper.toEntity(dto);
        normalizarCampos(category);

        Category salva = save(category);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setActive(false);
            salva = save(salva);
        }
        return CategoryMapper.toResponseDTO(salva);
    }

    /**
     * Returns the full DTO of a category by its identifier.
     *
     * @param id UUID of the category
     * @return full DTO of the category
     * @throws jakarta.persistence.EntityNotFoundException if the category does not exist
     */
    @Transactional(readOnly = true)
    public CategoryResponseDTO findResponseById(UUID id) {
        return CategoryMapper.toResponseDTO(findById(id));
    }

    /**
     * Lists all active categories in alphabetical order by name.
     *
     * @return list of summary DTOs of active categories
     */
    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> listSummary() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(CategoryMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all categories, including inactive ones.
     *
     * @return list of summary DTOs of all categories
     */
    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> listSummaryIncludingInactive() {
        return findAllIncludingInactive().stream()
                .map(CategoryMapper::toResumoDTO)
                .toList();
    }

    /**
     * Finds active categories whose name contains the given text (partial, case-insensitive search).
     *
     * @param name text to search in the category name; returns empty list if blank
     * @return list of summary DTOs of the found categories
     */
    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> findByName(String name) {
        String normalizedName = BrValidations.trimToNull(name);
        if (normalizedName == null) {
            return List.of();
        }

        return repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(normalizedName).stream()
                .map(CategoryMapper::toResumoDTO)
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
    public CategoryResponseDTO update(UUID id, CategoryUpdateDTO dto) {
        Category category = findById(id);
        CategoryMapper.updateEntity(category, dto);
        normalizarCampos(category);

        Category salva = save(category);
        return CategoryMapper.toResponseDTO(salva);
    }

    /**
     * Logically deactivates a category (sets {@code active = false}).
     *
     * @param id UUID of the category to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the category does not exist
     */
    @Transactional
    public void deleteLogically(UUID id) {
        delete(id);
    }

    /**
     * Returns the previous revision history of a category (Hibernate Envers).
     *
     * @param id UUID of the category
     * @return list of revisions in chronological order; empty list if there is no history
     */
    @Transactional(readOnly = true)
    public List<RevisionDTO<Category>> listRevisions(UUID id) {
        return listPreviousVersions(id);
    }

    private void normalizarCampos(Category category) {
        category.setName(BrValidations.trimToNull(category.getName()));
        category.setDescription(BrValidations.trimToNull(category.getDescription()));
        category.setIcon(normalizarIcone(category.getIcon()));
    }

    private String normalizarIcone(String icon) {
        String valor = BrValidations.trimToNull(icon);
        if (!CategoryIcon.isChaveValida(valor)) {
            throw new IllegalArgumentException("Invalid category icon.");
        }
        return valor;
    }
}
