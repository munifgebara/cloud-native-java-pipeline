package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Summary DTO of a category, used in listings and selectors.
 *
 * <p>Contains the fields sufficient to display the category in grids and
 * selection lists, including the icon for visual identification.</p>
 *
 * @param id        unique identifier of the category
 * @param name      category name
 * @param description descriptive text of the category; may be {@code null}
 * @param icon     visual icon key; may be {@code null}
 * @param active     indicates whether the category is active in the system
 */
public record CategorySummaryDTO(
        UUID id,
        String name,
        String description,
        String icon,
        boolean active
) {}
