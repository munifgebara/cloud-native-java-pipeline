package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperCrudService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
public class CategoryService extends SuperCrudService<
        Category,
        CategoryRepository,
        CategorySummaryDTO,
        CategoryResponseDTO,
        CategoryCreateDTO,
        CategoryUpdateDTO,
        RevisionDTO<Category>> {

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
     * <p>If the DTO {@code active} field is {@code false}, the category is created active
     * ({@code @PrePersist} constraint) and then deactivated in a second operation.</p>
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created category
     * @throws IllegalArgumentException if the provided icon is invalid
     */
    @Transactional
    public CategoryResponseDTO create(CategoryCreateDTO dto) {
        Category category = CategoryMapper.toEntity(dto);
        normalizeFields(category);

        Category salva = saveWithRequestedActiveState(category, dto.active());
        return CategoryMapper.toResponseDTO(salva);
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

        return repository.findAll(activeNameContains(normalizedName), Sort.by("name")).stream()
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
        normalizeFields(category);

        Category salva = save(category);
        return CategoryMapper.toResponseDTO(salva);
    }

    @Override
    protected CategorySummaryDTO toSummary(Category entity) {
        return CategoryMapper.toResumoDTO(entity);
    }

    @Override
    protected CategoryResponseDTO toResponse(Category entity) {
        return CategoryMapper.toResponseDTO(entity);
    }

    private void normalizeFields(Category category) {
        category.setName(BrValidations.trimToNull(category.getName()));
        category.setDescription(BrValidations.trimToNull(category.getDescription()));
        category.setIcon(normalizeIcon(category.getIcon()));
    }

    private String normalizeIcon(String icon) {
        String valor = BrValidations.trimToNull(icon);
        if (!CategoryIcon.isValidKey(valor)) {
            throw new IllegalArgumentException("Invalid category icon.");
        }
        return valor;
    }

    private Specification<Category> activeNameContains(String name) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")
        );
    }
}
